package com.lgg.yunpancodesandbox.utils;

import cn.hutool.core.util.StrUtil;
import com.lgg.yunpancodesandbox.exception.BusinessException;
import com.lgg.yunpancodesandbox.model.ExecuteCmdInfo;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 进程工具类
 */
public class ProcessUtils {

    /**
     * 执行进程并获取信息
     *
     * @param runProcess
     * @return
     */
    public static ExecuteCmdInfo runProcessAndGetMessage(Process runProcess, Long outTime) {
        return runProcessAndGetMessage(runProcess,outTime,TimeUnit.MILLISECONDS);
    }
    public static ExecuteCmdInfo runProcessAndGetMessage(Process runProcess, Long outTime, TimeUnit unit) {
        // ========== 异步消费 stdout 和 stderr，防止缓冲区满阻塞进程 ==========
        StringBuilder stdOutSb = new StringBuilder();
        StringBuilder stdErrSb = new StringBuilder();
        try {


            // 读取标准输出线程
            Thread stdOutThread = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(runProcess.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        stdOutSb.append(line).append("\n");
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e.getMessage());
                }
            });

//        // 读取错误输出线程
            Thread stdErrThread = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        stdErrSb.append(line).append("\n");
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e.getMessage());
                }
            });
            stdOutThread.start();
            stdErrThread.start();

            // 等待程序执行，获取错误码
// Java8没有waitFor超时，用CompletableFuture包装计时
            CompletableFuture<Integer> processFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return runProcess.waitFor();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e.getMessage());
                }
            });
            Integer exitValue;
            try {
                 exitValue = processFuture.get(outTime, unit);
            }catch (TimeoutException e){
                exitValue=null;
            }
            System.out.println("运行码： " + exitValue);
            // 等待两个流读取线程跑完
            stdOutThread.join();
            stdErrThread.join();

            String stdOut = stdOutSb.toString();
            String stdErr = stdErrSb.toString();

            return ExecuteCmdInfo.builder().errorMessage(stdErr)
                    .exitValue(exitValue).resultMessage(stdOut).build();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }finally {
            runProcess.destroy();
        }
    }


    private static String getProcessResultNormally(Process runProcess) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
        List<String> outputStrList = new ArrayList<>();
        // 逐行读取
        String compileOutputLine;
        while ((compileOutputLine = bufferedReader.readLine()) != null) {
            outputStrList.add(compileOutputLine);
        }
        return StringUtils.join(outputStrList, "\n");
    }

    /**
     * 执行交互式进程并获取信息
     *
     * @param runProcess
     * @param args
     * @return
     */
    public static ExecuteCmdInfo runInteractProcessAndGetMessage(Process runProcess, String args) {
//        ExecuteMessage executeMessage = new ExecuteMessage();

        try {
            // 向控制台输入程序
            OutputStream outputStream = runProcess.getOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            String[] s = args.split(" ");
            String join = StrUtil.join("\n", s) + "\n";
            outputStreamWriter.write(join);
            // 相当于按了回车，执行输入的发送
            outputStreamWriter.flush();

            // 分批获取进程的正常输出
            InputStream inputStream = runProcess.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder compileOutputStringBuilder = new StringBuilder();
            // 逐行读取
            String compileOutputLine;
            while ((compileOutputLine = bufferedReader.readLine()) != null) {
                compileOutputStringBuilder.append(compileOutputLine);
            }
//            executeMessage.setMessage(compileOutputStringBuilder.toString());
            // 记得资源的释放，否则会卡死
            outputStreamWriter.close();
            outputStream.close();
            inputStream.close();
            runProcess.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ExecuteCmdInfo.builder().build();
    }
}
