package com.lgg.yunpancodesandbox;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.lgg.yunpancodesandbox.model.ExecuteCodeRequest;
import com.lgg.yunpancodesandbox.model.ExecuteCodeResponse;
import com.lgg.yunpancodesandbox.model.ExecuteMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;
@Component
public class JavaDockerCodeSandbox extends JavaCodeSandboxTemplate {

    @Resource
    private DockerClient dockerClient;

    public static void main(String[] args) {
        JavaDockerCodeSandbox javaNativeCodeSandbox = new JavaDockerCodeSandbox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "1 3"));
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }
    @Override
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        String image = "openjdk:8-alpine";

        // ==========1. 检查镜像，不存在才拉取==========
        boolean imageExists = checkImageExists(image);
        if (!imageExists) {
            System.out.println("本地无镜像，开始拉取镜像：" + image);
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("下载镜像：" + item.getStatus());
                    super.onNext(item);
                }
            };
            try {
                pullImageCmd
                        .exec(pullImageResultCallback)
                        .awaitCompletion(120, TimeUnit.SECONDS);
                System.out.println("镜像下载完成");
            } catch (InterruptedException e) {
                System.out.println("拉取镜像异常");
                throw new RuntimeException(e);
            }
        }

        // 容器ID，finally里面用来清理
        String containerId = null;
        List<ExecuteMessage> executeMessageList = new ArrayList<>();

        try {
            // ==========2. 创建全新容器（每次请求新建，不复用）==========
            CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
            HostConfig hostConfig = new HostConfig();
            hostConfig.withMemory(100 * 1000 * 1000L);
            hostConfig.withMemorySwap(0L);
            hostConfig.withCpuCount(1L);
            // 当前请求独立代码目录挂载到容器 /app
            hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));
            CreateContainerResponse createContainerResponse = containerCmd
                    .withHostConfig(hostConfig)
                    .withNetworkDisabled(true)
                    .withReadonlyRootfs(true)
                    .withAttachStdin(true)
                    .withAttachStderr(true)
                    .withAttachStdout(true)
                    // 常驻进程，防止容器启动后立刻退出
                    .withCmd("sh", "-c", "sleep 36000")
                    .exec();
            containerId = createContainerResponse.getId();
            System.out.println("创建容器成功：" + containerId);

            // ==========3. 启动容器==========
            dockerClient.startContainerCmd(containerId).exec();
            System.out.println("容器启动成功");

            // ==========循环执行多组输入用例==========
            for (String inputArgs : inputList) {
                StopWatch stopWatch = new StopWatch();
                String[] inputArgsArray = inputArgs.split(" ");
                String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main"}, inputArgsArray);
                ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                        .withCmd(cmdArray)
                        .withAttachStderr(true)
                        .withAttachStdin(true)
                        .withAttachStdout(true)
                        .exec();
                System.out.println("创建执行命令：" + execCreateCmdResponse);

                ExecuteMessage executeMessage = new ExecuteMessage();
                final String[] message = {null};
                final String[] errorMessage = {null};
                long time = 0L;
                String execId = execCreateCmdResponse.getId();
                ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                    @Override
                    public void onComplete() {
                        super.onComplete();
                    }

                    @Override
                    public void onNext(Frame frame) {
                        StreamType streamType = frame.getStreamType();
                        String payload = new String(frame.getPayload());
                        if (StreamType.STDERR.equals(streamType)) {
                            errorMessage[0] = payload;
                            System.out.println("输出错误结果：" + errorMessage[0]);
                        } else {
                            message[0] = payload;
                            System.out.println("输出结果：" + message[0]);
                        }
                        super.onNext(frame);
                    }
                };

                final long[] maxMemory = {0L};
                StatsCmd statsCmd = dockerClient.statsCmd(containerId);
                AtomicBoolean statsClosed = new AtomicBoolean(false);
                statsCmd.exec(new ResultCallback<Statistics>() {
                    @Override
                    public void onNext(Statistics statistics) {
                        if (statsClosed.get()) {
                            return;
                        }
                        MemoryStatsConfig memoryStats = statistics.getMemoryStats();
                        if (memoryStats != null && memoryStats.getMaxUsage() != null) {
                            long peak = memoryStats.getMaxUsage();
                            if (peak > maxMemory[0]) {
                                maxMemory[0] = peak;
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        statsClosed.set(true);
                        statsCmd.close();
                    }
                    @Override
                    public void close() throws IOException {
                        statsClosed.set(true);
                        statsCmd.close();
                    }

                    @Override
                    public void onStart(Closeable closeable) {}
                    @Override
                    public void onComplete() {}
                });


                try {
                    stopWatch.start();
                    boolean isCompleted = dockerClient.execStartCmd(execId)
                            .exec(execStartResultCallback)
                            .awaitCompletion(TIME_OUT, TimeUnit.MILLISECONDS);
                    stopWatch.stop();
                    time = stopWatch.getLastTaskTimeMillis();
                    statsClosed.set(true);
                    statsCmd.close();
                    // ======================【修改点2】判断超时标记执行信息 ======================
                    if (!isCompleted) {
                        System.out.println("执行用例超时，限制时间：" + TIME_OUT + "ms");
                        return Collections.emptyList();
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException("程序执行异常");
                }
                executeMessage.setMessage(message[0]);
                executeMessage.setErrorMessage(errorMessage[0]);
                executeMessage.setTime(time);
                executeMessage.setMemory(maxMemory[0]);
                executeMessageList.add(executeMessage);
            }
        } finally {
            if (containerId != null) {
                try {
                    // 直接kill，跳过10s等待，瞬间结束
                    dockerClient.killContainerCmd(containerId).exec();
                } catch (NotModifiedException e) {
                    System.out.println("容器已经停止，无需kill");
                } catch (Exception e) {
                    System.err.println("kill容器失败：" + e.getMessage());
                }
                try {
                    dockerClient.removeContainerCmd(containerId).exec();
                    System.out.println("容器已删除");
                } catch (Exception e) {
                    System.err.println("删除容器失败：" + e.getMessage());
                }
            }
        }

        return executeMessageList;
    }


    /**
     * 工具方法：检查镜像是否存在本地
     */
    private boolean checkImageExists(String imageName) {
        List<Image> imageList = dockerClient.listImagesCmd()
                .withShowAll(true)
                .exec();
        return imageList.stream().anyMatch(
                image -> {
                    String[] repoTags = image.getRepoTags();
                    return repoTags != null && Arrays.stream(repoTags).anyMatch(imageName::equals);
                });

    }


    /**
     * 工具方法：根据容器名称查找容器id，找不到返回null
     */
    private String findContainerIdByName(String containerName) {
        List<Container> containerList = dockerClient.listContainersCmd()
                .withShowAll(true) // 必须true，才能查到stopped容器
                .withFilter("name", Arrays.asList("/" + containerName)) // docker容器名称自带前缀/
                .exec();
        if (containerList.isEmpty()) {
            return null;
        }
        return containerList.get(0).getId();
    }
}
