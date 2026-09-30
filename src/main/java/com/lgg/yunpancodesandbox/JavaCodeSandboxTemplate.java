package com.lgg.yunpancodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.lgg.yunpancodesandbox.enums.SandBoxResponseStatus;
import com.lgg.yunpancodesandbox.model.ExecuteCodeRequest;
import com.lgg.yunpancodesandbox.model.ExecuteCodeResponse;
import com.lgg.yunpancodesandbox.model.ExecuteMessage;
import com.lgg.yunpancodesandbox.model.JudgeInfo;
import com.lgg.yunpancodesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Java 代码沙箱模板方法的实现
 */
@Slf4j
public abstract class JavaCodeSandboxTemplate implements CodeSandbox {
    @Value("${code_sandbox.global_code_dir_name}")
    private  String globalCodeDirName;
    @Value("${code_sandbox.global_java_class_name}")
    private  String globalJavaClassName ;
    @Value("${code_sandbox.time_out}")
    public  long TIME_OUT;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

//        1. 把用户的代码保存为文件
        File userCodeFile = saveCodeToFile(code);

//        2. 编译代码，得到 class 文件
        ExecuteMessage compileFileExecuteMessage = compileFile(userCodeFile);
        if (compileFileExecuteMessage.getExitValue()!=0){
            deleteFile(userCodeFile);
            return ExecuteCodeResponse.builder()
                    .status(SandBoxResponseStatus.COMPILE_FAILED.getCode())
                    .message(SandBoxResponseStatus.COMPILE_FAILED.getMessage())
                    .build();
        }
        System.out.println(compileFileExecuteMessage);

        // 3. 执行代码，得到输出结果
        List<ExecuteMessage> executeMessageList = runFile(userCodeFile, inputList);
        if (executeMessageList.isEmpty()){
            deleteFile(userCodeFile);
            return ExecuteCodeResponse.builder()
                    .status(SandBoxResponseStatus.RUN_TIME_OUT.getCode())
                    .message(SandBoxResponseStatus.RUN_TIME_OUT.getMessage()).build();
        }

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
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        return userCodeFile;
    }

    /**
     * 2、编译代码
     * @param userCodeFile
     * @return
     */
    public ExecuteMessage compileFile(File userCodeFile) {
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
//            if (executeMessage.getExitValue() != 0) {
//                throw new RuntimeException("编译错误");
//            }
            return executeMessage;
        } catch (Exception e) {
//            return getErrorResponse(e);
            throw new RuntimeException("编译时出现异常");
        }
    }
    /**
     * 3、执行文件，获得执行结果列表
     * @param userCodeFile
     * @param inputList
     * @return
     */
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        List<ExecuteMessage> executeMessageList = new ArrayList<>();

        for (String inputArgs : inputList) {
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                AtomicBoolean taskDone = new AtomicBoolean(false);

                Thread timeoutThread = getTimeOutThread(taskDone, runProcess);

                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                System.out.println(executeMessage);
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
                Thread.sleep(TIME_OUT);
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
     * @param executeMessageList
     * @return
     */
    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        // 取用时最大值，便于判断是否超时
        long maxTime = 0;
        long maxMemory = 0L;
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)) {

                executeCodeResponse.setMessage(errorMessage);
                // 用户提交的代码执行中存在错误
                executeCodeResponse.setStatus(SandBoxResponseStatus.RUN_FAILED.getCode());
                break;
            }
            outputList.add(executeMessage.getMessage());
            Long time = executeMessage.getTime();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
            Long memory = executeMessage.getMemory();
            if (memory != null) {
                maxMemory = Math.max(maxMemory, memory);
            }
        }
        JudgeInfo judgeInfo =JudgeInfo.builder().time(maxTime)
                .memory(maxMemory/1024).build(); //kb


        // 正常运行完成
        if (outputList.size() == executeMessageList.size()&&executeCodeResponse.getStatus()==null) {
            executeCodeResponse.setStatus(SandBoxResponseStatus.SUCCESS.getCode());
            executeCodeResponse.setMessage(SandBoxResponseStatus.SUCCESS.getMessage());
        }else{
            judgeInfo.setMessage(executeCodeResponse.getMessage());
            executeCodeResponse.setMessage(SandBoxResponseStatus.RUN_FAILED.getMessage());
        }

        executeCodeResponse.setOutputList(outputList);
        executeCodeResponse.setJudgeInfo(judgeInfo);
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

    /**
     * 6、获取错误响应
     *
     * @param e
     * @return
     */
    private ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        // 表示代码沙箱错误
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(null);
        return executeCodeResponse;
    }
}
