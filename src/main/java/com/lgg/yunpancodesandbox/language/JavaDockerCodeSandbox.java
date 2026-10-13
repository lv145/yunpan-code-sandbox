package com.lgg.yunpancodesandbox.language;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.lgg.yunpancodesandbox.common.ErrorCode;
import com.lgg.yunpancodesandbox.enums.LanguageEnum;
import com.lgg.yunpancodesandbox.enums.SandBoxResponseStatus;
import com.lgg.yunpancodesandbox.enums.SandboxExceptionMsgEnum;
import com.lgg.yunpancodesandbox.exception.BusinessException;
import com.lgg.yunpancodesandbox.exception.ThrowUtils;
import com.lgg.yunpancodesandbox.model.ExecuteCodeRequest;
import com.lgg.yunpancodesandbox.model.ExecuteCodeResponse;
import com.lgg.yunpancodesandbox.model.ExecuteDockerCmdResponse;
import com.lgg.yunpancodesandbox.template.JavaCodeSandboxTemplate;
import com.lgg.yunpancodesandbox.model.ExecuteMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.io.Closeable;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class JavaDockerCodeSandbox extends JavaCodeSandboxTemplate {

    @Resource
    private DockerClient dockerClient;
    // 配置文件读取，例如 1024 * 1024 = 1MB

    /**
     * 获取容器运行的内存峰值（单位：字节，转KB /1024）
     *
     * @param dockerClient 客户端
     * @param containerId  容器ID
     * @return 内存峰值 bytes
     */
    public static long getContainerMaxMemory(DockerClient dockerClient, String containerId) throws InterruptedException {
        AtomicLong peakMemory = new AtomicLong(0L);
        // stats 流式采集
        dockerClient.statsCmd(containerId)
                .exec(new ResultCallback<Statistics>() {
                    @Override
                    public void onStart(Closeable closeable) {

                    }

                    @Override
                    public void onNext(Statistics stats) {
                        if (stats.getMemoryStats() != null && stats.getMemoryStats().getMaxUsage() != null) {
                            long currentPeak = stats.getMemoryStats().getMaxUsage();
                            if (currentPeak > peakMemory.get()) {
                                peakMemory.set(currentPeak);
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                    }

                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void close() {
                    }
                });
        // 等待采集，根据你的代码执行时间调整
        Thread.sleep(800);
        return peakMemory.get();
    }
    /**
     * 2、在容器中运行命令
     * @param cmdArray
     * @return
     */
    public ExecuteDockerCmdResponse executeDockerCmd(String[] cmdArray,String containerId) {
        // 改成数组存放StringBuilder
        AtomicBoolean oleFlag = new AtomicBoolean(false);//输出超限表示
        final StringBuilder[] resultSb = {new StringBuilder()};
        final StringBuilder[] errorSb = {new StringBuilder()};
        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                .withCmd(cmdArray)
                .withAttachStderr(true)
                .withAttachStdin(true)
                .withAttachStdout(true)
                .exec();
        String execId = execCreateCmdResponse.getId();
        ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
            @Override
            public void onComplete() {super.onComplete();}

            @Override
            public void onNext(Frame frame) {
                // 如果已经标记OLE，直接丢弃所有后续帧
                if (oleFlag.get()) {
                    return;
                }
                StreamType streamType = frame.getStreamType();
                byte[] payloadBytes = frame.getPayload();
                if (StreamType.STDERR.equals(streamType)) {
                    errorSb[0].append(new String(payloadBytes));
                } else {
                    // 先预判追加之后的总长度，不要先append再判断！
                    int newLen = resultSb[0].length() + payloadBytes.length;
                    if (newLen > maxOutputBytes) {
                        oleFlag.set(true);
                        return;
                    }
                    resultSb[0].append(new String(payloadBytes));
                }
                super.onNext(frame);
            }
        };
        long time;
        StopWatch stopWatch = new StopWatch();
        boolean isCompleted;
        long maxMemory;
        try {
            stopWatch.start();
            isCompleted = dockerClient.execStartCmd(execId)
                    .exec(execStartResultCallback)
                    .awaitCompletion(timeOut, TimeUnit.MILLISECONDS);
            stopWatch.stop();
            maxMemory = getContainerMaxMemory(dockerClient, containerId);
        }catch (InterruptedException e) {
            stopWatch.stop();
            throw  new BusinessException(ErrorCode.SYSTEM_ERROR,SandboxExceptionMsgEnum.SANDBOX_DOCKER_ERROR.getMessage());
        }

        time = stopWatch.getLastTaskTimeMillis();
        // 取出完整内容
        String resultMessage = resultSb[0].toString().replaceAll("[\\r\\n]+$", "");
        String errorMessage = errorSb[0].toString();
        return ExecuteDockerCmdResponse.builder().time(time).errorMessage(errorMessage).isOutTime(!isCompleted)
                .maxMemory(maxMemory).resultMessage(resultMessage).isOutputLimitExceeded(oleFlag.get()).build();
    }
    @Override
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        String image = LanguageEnum.JAVA.getImageName();
        // ==========1. 检查镜像，不存在才拉取==========
        pullImage(image);

        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        // ==========循环执行多组输入用例==========
        for (String inputArgs : inputList) {
            // 容器ID，finally里面用来清理
            String containerId = null;
            StatsCmd statsCmd = null;
            try {
                // ==========2. 创建容器==========
                containerId = createContainer(image, userCodeParentPath);
                log.info("创建容器成功containerId: {}", containerId);
                // ==========3. 启动容器==========
                dockerClient.startContainerCmd(containerId).exec();
                log.info("容器启动成功containerId: {}", containerId);
                // ==========5. 拼接命令==========
                String[] inputArgsArray = inputArgs.split(" ");
                String[] cmdArray = ArrayUtil.append(LanguageEnum.JAVA.getRunCmdTemplate().split(" "), inputArgsArray);
                ExecuteDockerCmdResponse executeDockerCmdResponse = executeDockerCmd(cmdArray, containerId);
                log.info("exe:{}",executeDockerCmdResponse);
                if (executeDockerCmdResponse.getIsOutputLimitExceeded()) {
                    System.out.println("执行用例输出超限，限制长度：" + maxOutputBytes );
                    executeMessageList.add(ExecuteMessage.builder()
                            .errorMessage(SandBoxResponseStatus.OUTPUT_LIMIT_EXCEEDED.getMessage()).build());
                    break;
                }
                if (executeDockerCmdResponse.getIsOutTime()) {
                    System.out.println("执行用例超时，限制时间：" + timeOut + "ms");
                    executeMessageList.add(ExecuteMessage.builder()
                            .errorMessage(SandBoxResponseStatus.RUN_TIME_OUT.getMessage()).build());
                    break;
                }
                ExecuteMessage executeMessage = ExecuteMessage.builder().build();
                // 修剪末尾换行
                BeanUtils.copyProperties(executeDockerCmdResponse, executeMessage);
                executeMessageList.add(executeMessage);
            }catch (Exception e) {
                log.error("用例执行异常:{}", e.getMessage());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,SandboxExceptionMsgEnum.SANDBOX_DOCKER_ERROR.getMessage());
            } finally {
                closeContainer(containerId, statsCmd);
            }
        }

        return executeMessageList;
    }


    private void closeContainer(String containerId, StatsCmd statsCmd) {
        if (containerId != null) {
            // 先关监控
            try {
                statsCmd.close();
            } catch (Exception e) {
                System.err.println("关闭stats失败：" + e.getMessage());
            }
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

    private void pullImage(String image) {
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
    }

    private String createContainer(String image, String userCodeParentPath) {
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
        return createContainerResponse.getId();
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
