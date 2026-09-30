package com.lgg.yunpancodesandbox.utils;

import cn.hutool.core.util.StrUtil;
import com.lgg.yunpancodesandbox.model.ExecuteMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StopWatch;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 进程工具类
 */
public class ProcessUtils {

    /**
     * 执行进程并获取信息
     *
     * @param runProcess
     * @param opName
     * @return
     */
    public static ExecuteMessage runProcessAndGetMessage(Process runProcess, String opName) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // ========== 异步消费 stdout 和 stderr，防止缓冲区满阻塞进程 ==========
        StringBuilder stdOutSb = new StringBuilder();
//        StringBuilder stdErrSb = new StringBuilder();

        // 读取标准输出线程
        Thread stdOutThread = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(runProcess.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    stdOutSb.append(line).append("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

//        // 读取错误输出线程
//        Thread stdErrThread = new Thread(() -> {
//            try (BufferedReader br = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()))) {
//                String line;
//                while ((line = br.readLine()) != null) {
//                    stdErrSb.append(line).append("\n");
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });
        stdOutThread.start();
//        stdErrThread.start();

        try {
            // 等待程序执行，获取错误码
            int exitValue = runProcess.waitFor();
            executeMessage.setExitValue(exitValue);

            // 等待两个流读取线程跑完
            stdOutThread.join();
//            stdErrThread.join();

            String stdOut = stdOutSb.toString();
//            String stdErr = stdErrSb.toString();

            executeMessage.setMessage(stdOut);
            if (exitValue == 0) {
                System.out.println(opName + "成功");
            } else {
                System.out.println(opName + "失败，错误码： " + exitValue);
//                executeMessage.setErrorMessage(stdErr);
            }
            stopWatch.stop();
            executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
        } catch (Exception e) {
            stopWatch.stop();
            executeMessage.setErrorMessage("进程执行异常：" + e.getMessage());
            e.printStackTrace();
        }
        return executeMessage;
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
    public static ExecuteMessage runInteractProcessAndGetMessage(Process runProcess, String args) {
        ExecuteMessage executeMessage = new ExecuteMessage();

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
            executeMessage.setMessage(compileOutputStringBuilder.toString());
            // 记得资源的释放，否则会卡死
            outputStreamWriter.close();
            outputStream.close();
            inputStream.close();
            runProcess.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return executeMessage;
    }
}
