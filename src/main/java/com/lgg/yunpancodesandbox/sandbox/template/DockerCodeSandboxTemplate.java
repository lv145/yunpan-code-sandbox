package com.lgg.yunpancodesandbox.sandbox.template;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.IdUtil;
import com.github.dockerjava.api.DockerClient;
import com.lgg.yunpancodesandbox.sandbox.CodeSandbox;
import com.lgg.yunpancodesandbox.common.ErrorCode;
import com.lgg.yunpancodesandbox.docker.DockerFlagManager;
import com.lgg.yunpancodesandbox.enums.LanguageEnum;
import com.lgg.yunpancodesandbox.enums.SandBoxResponseStatus;
import com.lgg.yunpancodesandbox.enums.SandboxExceptionMsgEnum;
import com.lgg.yunpancodesandbox.exception.BusinessException;
import com.lgg.yunpancodesandbox.exception.ThrowUtils;
import com.lgg.yunpancodesandbox.model.ExecuteCodeInfo;
import com.lgg.yunpancodesandbox.model.ExecuteCodeRequest;
import com.lgg.yunpancodesandbox.model.ExecuteCodeResponse;
import com.lgg.yunpancodesandbox.model.ExecuteCmdInfo;
import com.lgg.yunpancodesandbox.sandbox.CodeSandboxProperties;
import com.lgg.yunpancodesandbox.utils.CodeErrorMsgUtil;
import com.lgg.yunpancodesandbox.utils.DockerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Python3、C 等语言 Docker 代码沙箱模板方法的实现。
 */
@Slf4j
public abstract class DockerCodeSandboxTemplate implements CodeSandbox {

    /** Docker 客户端，用于创建容器、执行命令和读取容器状态。 */
    @Resource
    protected DockerClient dockerClient;
    @Resource
    DockerFlagManager dockerFlagManager;
    @Resource
    protected CodeErrorMsgUtil codeErrorMsgUtil;

    @Resource
    private CodeSandboxProperties properties;

    /** 返回当前沙箱对应的语言配置，包括镜像、编译命令和运行命令。 */
    protected abstract LanguageEnum getLanguage();
    private  String globalCodeDirPath;
    @PostConstruct
    public void initPattern() {
        globalCodeDirPath = System.getProperty("user.dir") + File.separator +
                properties.getGlobalCodeDirName();

    }
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        //判断语言docker镜像环境是否就绪
        ThrowUtils.throwIf(!dockerFlagManager.isImageReady(getLanguage().getImageName()),
                ErrorCode.SYSTEM_ERROR,"docker环境异常");
        //保存代码
        File userCodeFile = saveCodeToFile(executeCodeRequest.getCode());

        File parentFile = userCodeFile.getParentFile();
        String containerFilePath=properties.getGlobalDockerCodeDirPath()+File.separator+
                parentFile.getName()+File.separator+userCodeFile.getName();
        String containerWorkPath=properties.getGlobalDockerCodeDirPath()+File.separator+
                parentFile.getName();

        try {
            //编译代码
            ExecuteCmdInfo compileInfo = compileFile(containerFilePath,containerWorkPath);
            if (compileInfo.getExitValue() == null || compileInfo.getExitValue() != 0) {
                String errorMessage = codeErrorMsgUtil.filterPathToFileName(compileInfo.getErrorMessage());
                return ExecuteCodeResponse.builder()
                        .status(SandBoxResponseStatus.COMPILE_FAILED.getCode())
                        .executeCodeInfoList(Collections.singletonList(ExecuteCodeInfo.builder()
                                .errorMessage(errorMessage).build()))
                        .build();
            }
            if (SandBoxResponseStatus.COMPILE_TIMEOUT.equals(compileInfo.getStatus())) {
                return ExecuteCodeResponse.builder()
                        .status(SandBoxResponseStatus.COMPILE_TIMEOUT.getCode())
                        .executeCodeInfoList(Collections.singletonList(ExecuteCodeInfo.builder()
                                .errorMessage(SandBoxResponseStatus.COMPILE_TIMEOUT.getMessage()).build()))
                        .build();
            }

            return getOutputResponse(runFile( executeCodeRequest.getInputList(),containerFilePath,containerWorkPath));
        } finally {
            deleteFile(userCodeFile);
        }
    }

//    /** 将用户代码保存到随机目录，避免不同请求之间互相覆盖。 */
//    protected File saveCodeToFile(String code) {
//        String userCodePath= globalCodeDirPath +File.separator + UUID.randomUUID()+
//                File.separator + properties.getGlobalFileName()+getLanguage().getFileSuffix();
//        return FileUtil.writeString(code, userCodePath , StandardCharsets.UTF_8);
//    }
    /**
     * 将用户代码保存到随机目录，避免不同请求之间互相覆盖。
     * @return 代码文件File
     */
    protected File saveCodeToFile(String code) {
        // 示例，限制源码最大 256KB
        ThrowUtils.throwIf(code.length() >256 * 1024,ErrorCode.PARAMS_ERROR, "代码过长");

        // 用hutool的UUID，和UUID.randomUUID等价
        String uuid = IdUtil.fastSimpleUUID();
        File codeDir = FileUtil.file(globalCodeDirPath, uuid);
        // 显式创建目录
        FileUtil.mkdir(codeDir);

        String fileName = properties.getGlobalFileName() + getLanguage().getFileSuffix();
        File codeFile = FileUtil.file(codeDir, fileName);
        try {
            FileUtil.writeString(code, codeFile, StandardCharsets.UTF_8);
            // 可选：权限收紧，linux下 0600，仅所有者读写
            codeFile.setReadable(true, true);
            codeFile.setWritable(true, true);
            codeFile.setExecutable(false, false);
        } catch (Exception e) {
            // 抛出你自己的业务异常，比如代码保存失败
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户代码文件写入失败");
        }
        return codeFile;
    }
    /** 在独立容器中编译需要编译的语言，Python3 直接返回成功。 */
    private ExecuteCmdInfo compileFile(String containerFilePath,String workDir) {
        if (!getLanguage().needCompile()) {
            return ExecuteCmdInfo.builder().exitValue(0).build();
        }
        LanguageEnum language = getLanguage();
        String imageName = language.getImageName();
        String containerId = null;
        try {
            containerId = DockerUtils.createContainer(dockerClient,imageName,
                    globalCodeDirPath,properties.getGlobalDockerCodeDirPath(),true);

            String compileCmd = getLanguage().getCompileCmd(containerFilePath,
                    workDir, properties.getGlobalFileName());

            return DockerUtils.executeDockerCmd(dockerClient,compileCmd.split("\\s+"), containerId,properties,null);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    SandboxExceptionMsgEnum.COMPILE_ERROR.getMessage());
        }
    }

    /** 为每组输入创建独立容器并执行用户程序。 */
    private List<ExecuteCmdInfo> runFile( List<String> inputList,String containerFilePath,String workDir) {
        String image = getLanguage().getImageName();
        // ==========1. 检查镜像，不存在才拉取==========

        List<ExecuteCmdInfo> executeCmdInfoList= new ArrayList<>();
        // ==========循环执行多组输入用例==========
        for (String inputText : inputList) {
            // 容器ID，finally里面用来清理
            String containerId = null;
            try {
                // ==========2. 创建容器==========

                containerId = DockerUtils.createContainer(dockerClient,image,
                        globalCodeDirPath,properties.getGlobalDockerCodeDirPath(),false);

                // ==========5. 拼接命令==========
                String[] inputArgsArray = inputText.split(" ");

                String runCmd = getLanguage().getRunCmd(containerFilePath,
                        workDir, properties.getGlobalFileName());
                String[] cmdArray = ArrayUtil.append(runCmd.split(" "), inputArgsArray);
                ExecuteCmdInfo executeCmdInfo = DockerUtils.executeDockerCmd(dockerClient,cmdArray, containerId,properties,inputText);
                ExecuteCmdInfo info = ExecuteCmdInfo.builder().build();
                BeanUtils.copyProperties(executeCmdInfo, info);
                executeCmdInfoList.add(info);
                if (!SandBoxResponseStatus.SUCCESS.equals(executeCmdInfo.getStatus())){
                    break;
                }
            }catch (Exception e) {
                log.error("用例执行异常:{}", e.getMessage());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,SandboxExceptionMsgEnum.DOCKER_ERROR.getMessage());
            } finally {
                DockerUtils.closeContainer(dockerClient,containerId);
            }
        }

        return executeCmdInfoList;
    }


    /**
     * 将每个测试用例的执行信息转换为接口响应，并确定最终判题状态。
     * 规则：一旦某个用例非SUCCESS，直接截断后续用例；整体状态取最后一条用例状态。
     * @param executeCmdInfoList 各个测试用例单独执行结果列表
     * @return 沙箱统一返回 ExecuteCodeResponse
     */
    public ExecuteCodeResponse getOutputResponse(List<ExecuteCmdInfo> executeCmdInfoList) {
        ExecuteCmdResponseBuilder responseBuilder = new ExecuteCmdResponseBuilder();
        // 遍历所有用例，收集信息，计算全局maxTime、maxMemory
        for (ExecuteCmdInfo cmdInfo : executeCmdInfoList) {
            if (cmdInfo.getStatus().equals(SandBoxResponseStatus.RUN_FAILED)){
                String errorMessage = cmdInfo.getErrorMessage();
                String  parseErrorMessage  = codeErrorMsgUtil.parseUserCodeError(getLanguage().getLanguage(), errorMessage);
                cmdInfo.setErrorMessage(parseErrorMessage);
            }
            responseBuilder.add(cmdInfo);
        }


        return responseBuilder.build();
    }


    private static class ExecuteCmdResponseBuilder {
        private final List<ExecuteCodeInfo> infoList = new ArrayList<>();
        private Integer status = SandBoxResponseStatus.SUCCESS.getCode();
        private long maxTime;
        private long maxMemory;

        private void add(ExecuteCmdInfo cmdInfo) {
            status = cmdInfo.getStatus().getCode();
            maxTime = Math.max(maxTime, cmdInfo.getTime() == null ? 0 : cmdInfo.getTime());
            maxMemory = Math.max(maxMemory, cmdInfo.getMemory() == null ? 0 : cmdInfo.getMemory());
            ExecuteCodeInfo info = ExecuteCodeInfo.builder().build();
            BeanUtils.copyProperties(cmdInfo, info);
            infoList.add(info);
        }

        private ExecuteCodeResponse build() {
            return com.lgg.yunpancodesandbox.model.ExecuteCodeResponse.builder().status(status).maxTime(maxTime).maxMemory(maxMemory)
                    .executeCodeInfoList(infoList).build();
        }
    }


    //




    /** 删除当前请求生成的临时源代码目录。 */
    private boolean deleteFile(File userCodeFile) {
        return userCodeFile.getParentFile() == null || FileUtil.del(userCodeFile.getParentFile());
    }
}


