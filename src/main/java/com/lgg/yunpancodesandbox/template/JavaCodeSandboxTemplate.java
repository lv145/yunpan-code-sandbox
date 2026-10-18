package com.lgg.yunpancodesandbox.template;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.lgg.yunpancodesandbox.CodeSandbox;

import com.lgg.yunpancodesandbox.enums.JavaCodeExceptionEnum;
import com.lgg.yunpancodesandbox.enums.LanguageEnum;
import com.lgg.yunpancodesandbox.enums.SandBoxResponseStatus;
import com.lgg.yunpancodesandbox.enums.SandboxExceptionMsgEnum;
import com.lgg.yunpancodesandbox.model.*;
import com.lgg.yunpancodesandbox.utils.CodeErrorMsgUtil;
import com.lgg.yunpancodesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Java 代码沙箱模板方法的实现
 */
@Slf4j
public abstract class JavaCodeSandboxTemplate implements CodeSandbox {
    @Value("${code_sandbox.jvm_error_lang}")
    public String lang ;
    @Value("${code_sandbox.global_code_dir_name}")
    private  String globalCodeDirName;
    @Value("${code_sandbox.global_java_class_name}")
    private  String globalJavaClassName ;
    @Value("${code_sandbox.time_out}")
    public  long timeOut ;
    @Value("${code_sandbox.max_output_bytes}")
    public long  maxOutputBytes;
    @Resource
    CodeErrorMsgUtil codeErrorMsgUtil;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();
//        1. 把用户的代码保存为文件
        File userCodeFile = saveCodeToFile(code);
//        2. 编译代码，得到 class 文件
        ExecuteCmdInfo compileFileExecuteMessage = compileFile(userCodeFile);
        if (compileFileExecuteMessage.getExitValue()==null){
            ExecuteCodeInfo executeCodeInfo = ExecuteCodeInfo.builder()
                    .errorMessage(SandBoxResponseStatus.COMPILE_TIMEOUT.getMessage()).build();
            return ExecuteCodeResponse.builder().status(SandBoxResponseStatus.COMPILE_TIMEOUT.getCode())
                    .executeCodeInfoList(Collections.singletonList(executeCodeInfo)).build();
        }
        if (compileFileExecuteMessage.getExitValue()!=0){
            deleteFile(userCodeFile);
            String errorMessage= codeErrorMsgUtil.filterCompileMessage(
                            compileFileExecuteMessage.getErrorMessage());
            ExecuteCodeInfo executeCodeInfo = ExecuteCodeInfo.builder()
                    .errorMessage(errorMessage).build();
           return ExecuteCodeResponse.builder().status(SandBoxResponseStatus.COMPILE_FAILED.getCode())
                   .executeCodeInfoList(Collections.singletonList(executeCodeInfo)).build();
        }

        // 3. 执行代码，得到输出结果
        List<ExecuteCmdInfo> executeMessageList = runFile(userCodeFile, inputList);

//        4. 收集整理输出结果
        ExecuteCodeResponse outputResponse = getOutputResponse(executeMessageList);

//        5. 文件清理
        boolean b = deleteFile(userCodeFile);
        if (!b) {
            log.error("deleteFile error, userCodeFilePath = {}", userCodeFile.getAbsolutePath());
        }
        return outputResponse;
    }


    /**
     * 1. 把用户的代码保存为文件
     * @param code 用户代码
     * @return
     */
    public File saveCodeToFile(String code) {
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + globalCodeDirName;
        // 判断全局代码目录是否存在，没有则新建
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }

        // 把用户的代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + globalJavaClassName;
        return FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
    }

    /**
     * 2、编译代码
     * @param userCodeFile
     * @return
     */
    public ExecuteCmdInfo compileFile(File userCodeFile) {
        String compileCmd = String.format(LanguageEnum.JAVA.getCompileCmdTemplate(), userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            return ProcessUtils.runProcessAndGetMessage(compileProcess,timeOut);
        } catch (Exception e) {
            throw new RuntimeException(SandboxExceptionMsgEnum.COMPILE_ERROR.getMessage()+":"+e.getMessage());
        }
    }
    /**
     * 3、执行文件，获得执行结果列表
     * @param userCodeFile
     * @param inputList
     * @return
     */
    public List<ExecuteCmdInfo> runFile(File userCodeFile, List<String> inputList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        List<ExecuteCmdInfo> executeMessageList = new ArrayList<>();

        for (String inputArgs : inputList) {
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                AtomicBoolean taskDone = new AtomicBoolean(false);

                Thread timeoutThread = getTimeOutThread(taskDone, runProcess);

                ExecuteCmdInfo executeNativeMessage = ProcessUtils.runProcessAndGetMessage(runProcess,timeOut);
                System.out.println(executeNativeMessage);
                ExecuteCmdInfo executeMessage=ExecuteCmdInfo.builder().build();
                BeanUtils.copyProperties(executeNativeMessage, executeMessage);
                executeMessageList.add(executeMessage);
                taskDone.set(true);
                timeoutThread.interrupt();
            } catch (Exception e) {
//                throw new RuntimeException("执行错误", e);
                return Collections.emptyList();
            }
        }
        return executeMessageList;
    }

    private Thread getTimeOutThread(AtomicBoolean taskDone, Process runProcess) {
        Thread timeoutThread = new Thread(() -> {
            try {
                Thread.sleep(timeOut);
                if (!taskDone.get() && runProcess.isAlive()) {
                    System.out.println("超时了，中断");
                    runProcess.destroy();
                    // 兜底强制销毁
                    if(runProcess.isAlive()){
                        runProcess.destroyForcibly();
                    }
                }
            } catch (InterruptedException e) {
                // 正常中断，直接退出线程，不抛异常
            }
        });
        timeoutThread.start();
        return timeoutThread;
    }


    /**
     * 4、获取输出结果
     * @param executeCmdInfoList
     * @return
     */
    public ExecuteCodeResponse getOutputResponse(List<ExecuteCmdInfo> executeCmdInfoList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<ExecuteCodeInfo> executeCodeInfoList = new ArrayList<>();

        //设置判题状态
        ExecuteCmdInfo executeCmdInfo = executeCmdInfoList.get(executeCmdInfoList.size() - 1);
        executeCodeResponse.setStatus(executeCmdInfo.getStatus().getCode());

        long maxTime = 0;
        long maxMemory = 0L;
        for (ExecuteCmdInfo codeInfo : executeCmdInfoList) {
            Long time = codeInfo.getTime();
            Long memory = codeInfo.getMemory();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
            if (memory != null) {
                maxMemory = Math.max(maxMemory, memory);
            }
            ExecuteCodeInfo executeCodeInfo=ExecuteCodeInfo.builder().build();
            BeanUtils.copyProperties(codeInfo, executeCodeInfo);
            executeCodeInfoList.add(executeCodeInfo);
        }
        executeCodeResponse.setMaxTime(maxTime);
        executeCodeResponse.setMaxMemory(maxMemory);//kb
        executeCodeResponse.setExecuteCodeInfoList(executeCodeInfoList);
        return executeCodeResponse;
    }

    /**
     * 5、删除文件
     * @param userCodeFile
     * @return
     */
    public boolean deleteFile(File userCodeFile) {
        if (userCodeFile.getParentFile() != null) {
            String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
            return del;
        }
        return true;
    }

}
