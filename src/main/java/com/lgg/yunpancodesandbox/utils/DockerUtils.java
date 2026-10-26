package com.lgg.yunpancodesandbox.utils;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.lgg.yunpancodesandbox.common.ErrorCode;
import com.lgg.yunpancodesandbox.docker.DockerCmdResultCallback;
import com.lgg.yunpancodesandbox.docker.DockerFlagManager;
import com.lgg.yunpancodesandbox.enums.LanguageEnum;
import com.lgg.yunpancodesandbox.enums.SandBoxResponseStatus;
import com.lgg.yunpancodesandbox.enums.SandboxExceptionMsgEnum;
import com.lgg.yunpancodesandbox.exception.BusinessException;
import com.lgg.yunpancodesandbox.model.ExecuteCmdInfo;
import com.lgg.yunpancodesandbox.sandbox.CodeSandboxProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Dragon
 */
@Slf4j
public class DockerUtils {
    public static final String DOCKER_STATE_RUNNING = "running";
    public static final String LANGUAGE_CONTAINER_COMPILE_SUFFIX = "compile-";
    /** 读取 Docker stats，返回容器运行期间的内存峰值，单位字节 */
    public static long getContainerMaxMemory(DockerClient dockerClient, String containerId) throws InterruptedException {
        AtomicLong peakMemory = new AtomicLong(0L);
        CountDownLatch latch = new CountDownLatch(1);
        // 保存closeable用来关闭stats流
        AtomicReference<Closeable> statsCloseable = new AtomicReference<>();

        ResultCallback<Statistics> statsCallback = new ResultCallback<Statistics>() {
            @Override
            public void onStart(Closeable closeable) {
                statsCloseable.set(closeable);
            }

            @Override
            public void onNext(Statistics stats) {
                if (stats.getMemoryStats() != null && stats.getMemoryStats().getMaxUsage() != null) {
                    peakMemory.updateAndGet(v -> Math.max(v, stats.getMemoryStats().getMaxUsage()));
                    // 收到第一条有效数据，放行
                    latch.countDown();
                }
            }
            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }

            @Override
            public void close() {
            }
        };

        dockerClient.statsCmd(containerId)
                .withNoStream(false) // 持续流式
                .exec(statsCallback);

        // 最多等待2s拿到第一条stats，不要无限等
        latch.await(2, TimeUnit.SECONDS);

        return peakMemory.get();
    }

    /**
     * 在容器中执行命令，同时采集标准输出、错误输出、耗时和内存峰值。
     */
    public static ExecuteCmdInfo executeDockerCmd(DockerClient dockerClient,String[] cmdArray,
                                                  String containerId, CodeSandboxProperties properties,String inputText) {

        AtomicBoolean outputLimitExceeded = new AtomicBoolean(false);
        StringBuilder resultBuilder = new StringBuilder();
        StringBuilder errorBuilder = new StringBuilder();
        ExecCreateCmdResponse execResponse = dockerClient.execCreateCmd(containerId)
                .withCmd(cmdArray).withAttachStderr(true).withAttachStdin(true)
                .withAttachStdout(true).exec();
        //创建回调
        DockerCmdResultCallback callback=new DockerCmdResultCallback(resultBuilder,errorBuilder,
                properties.getMaxOutputLength(),outputLimitExceeded);


        long time;
        StopWatch stopWatch = new StopWatch();
        boolean isCompleted;
        long memory;

        try {
            ExecStartCmd execStartCmd = dockerClient.execStartCmd(execResponse.getId());
            if (StrUtil.isNotBlank(inputText)) {
                String formatInputText = StdinNormalizeUtil.formattingParameter(inputText);
                execStartCmd.withStdIn(new ByteArrayInputStream(formatInputText.getBytes(StandardCharsets.UTF_8)));
            }
            stopWatch.start();

            isCompleted = execStartCmd.exec(callback)
                    .awaitCompletion(properties.getTimeOut(), TimeUnit.MILLISECONDS);
            stopWatch.stop();
            memory = getContainerMaxMemory(dockerClient, containerId);
        } catch (InterruptedException e) {
            stopWatch.stop();
            Thread.currentThread().interrupt();
            throw new BusinessException(com.lgg.yunpancodesandbox.common.ErrorCode.SYSTEM_ERROR,
                    SandboxExceptionMsgEnum.DOCKER_ERROR.getMessage());
        }
        Long exitCode = dockerClient.inspectExecCmd(execResponse.getId()).exec().getExitCodeLong();
        time = stopWatch.getLastTaskTimeMillis();
        // 取出完整内容
        String resultMessage = resultBuilder.toString().replaceAll("[\\r\\n]+$", "");
        String errorMessage = errorBuilder.toString();

        ExecuteCmdInfo executeCmdInfo = ExecuteCmdInfo.builder().errorMessage(errorMessage)
                .exitValue(exitCode == null ? null : exitCode.intValue())
                .resultMessage(resultMessage).memory(memory/1024) //kb
                .time(time).status(SandBoxResponseStatus.SUCCESS).build();

        if (StrUtil.isNotBlank(errorMessage)){
            log.info("运行出现错误:{}", errorMessage);
            // 修剪末尾换行
            executeCmdInfo.setErrorMessage(errorMessage);
            executeCmdInfo.setStatus(SandBoxResponseStatus.RUN_FAILED);
            return executeCmdInfo;
        }

        if (outputLimitExceeded.get()) {
            log.info("执行用例输出超限，限制长度：{}" , properties.getMaxOutputLength() );
            // 修剪末尾换行
            SandBoxResponseStatus status = SandBoxResponseStatus.OUTPUT_LIMIT_EXCEEDED;
            executeCmdInfo.setErrorMessage(status.getMessage());
            executeCmdInfo.setStatus(status);
            return executeCmdInfo;
        }
        if (!isCompleted) {
            log.info("执行用例超时，限制时间：{}" , properties.getTimeOut() +"ms");
            SandBoxResponseStatus runTimeOut =SandBoxResponseStatus.RUN_TIME_OUT ;
            executeCmdInfo.setErrorMessage(runTimeOut.getMessage());
            executeCmdInfo.setStatus(runTimeOut);
            return executeCmdInfo;
        }
        return executeCmdInfo;
    }

    /**
     * 根据 image:tag 查询正在运行的容器
     * @param dockerClient docker客户端
     * @param imageWithTag 例如 openjdk:8-alpine
     * @return 找到返回Container，没找到返回null（只找running的）
     */
    public static Container findRunningContainerIdByAncestor(DockerClient dockerClient, String imageWithTag) {
        List<Container> list = dockerClient.listContainersCmd()
                .withShowAll(true) // false：只看running
                .withNameFilter(Collections.singletonList(imageWithTag))
                .exec();
        return list.isEmpty() ? null : list.get(0);
    }

    /** 创建受限容器：限制内存和 CPU、关闭网络。 */
    public static String createContainer(DockerClient dockerClient,String imageName, String userCodeParentPath,
                                         String globalDockerCodeDirName ,Boolean isCompile) {
         Map<String, String>  logOpts = new HashMap<>();
        logOpts.put("max-size", "2m");
        logOpts.put("max-file", "1");
        LogConfig logConfig = new LogConfig(LogConfig.LoggingType.JSON_FILE, logOpts);
        HostConfig hostConfig = new HostConfig()
                .withMemory(100 * 1000 * 1000L)
                .withMemorySwap(100 * 1000 * 1000L)
                .withCapDrop(Capability.ALL)
                .withSecurityOpts(Collections.singletonList("no-new-privileges:true"))
                .withReadonlyRootfs(true)
                .withPidsLimit(32L)
                .withTmpFs(Collections.singletonMap("/tmp", "size=64m,noexec,nosuid,nodev"))
                .withCpuCount(1L)
                .withNanoCPUs(1_000_000_000L)
                .withLogConfig(logConfig)
                .withBinds(new Bind(userCodeParentPath, new Volume(globalDockerCodeDirName),AccessMode.ro));
        CreateContainerCmd cmd = dockerClient
                .createContainerCmd(imageName)
                .withUser("1000")
                .withHostConfig(hostConfig)
                .withNetworkDisabled(true)
                .withAttachStderr(true);

        if (isCompile){
            cmd.withCmd("sleep", "864000");
            String containerName=LANGUAGE_CONTAINER_COMPILE_SUFFIX+LanguageEnum.getLanguageByImageName(imageName);
            cmd.withName(containerName);
            Container container = findRunningContainerIdByAncestor(dockerClient, containerName);
            if (container == null){
                CreateContainerResponse exec = cmd.exec();
                dockerClient.startContainerCmd(exec.getId()).exec();
                return exec.getId();
            }


            if (container.getState().equals(DOCKER_STATE_RUNNING)) {
                return container.getId();
            }
            dockerClient.startContainerCmd(container.getId()).exec();
            return container.getId();
        }

        cmd.withAttachStdout(true).withCmd("sh", "-c", "sleep 30000");
        CreateContainerResponse response = cmd.exec();
        dockerClient.startContainerCmd(response.getId()).exec();
        return response.getId();
    }

    /** 无论程序成功、失败还是超时，都必须清理容器。 */
    public static void closeContainer(DockerClient dockerClient, String containerId) {
        if (containerId == null) {
            return;
        }
        try {
            dockerClient.killContainerCmd(containerId).exec();
        } catch (NotModifiedException ignored) {
        } catch (Exception e) {
            log.warn("关闭容器失败: {}", e.getMessage());
        }
        try {
            dockerClient.removeContainerCmd(containerId).exec();
        } catch (Exception e) {
            log.warn("删除容器失败: {}", e.getMessage());
        }
    }

    /** 本地不存在目标镜像时再拉取，避免每次请求重复下载。 */
    public static void pullLanguageImages(DockerClient dockerClient, DockerFlagManager dockerFlagManager) {
        String imageName;
        try {
            checkImagesExists(dockerClient, dockerFlagManager);
            for (LanguageEnum value : LanguageEnum.values()) {
                //默认false
                imageName = value.getImageName();
                if (dockerFlagManager.isImageReady(imageName)) {
                    continue;
                }
                dockerClient.pullImageCmd(imageName).exec(new PullImageResultCallback())
                        .awaitCompletion(360, TimeUnit.SECONDS);
                dockerFlagManager.markSuccess(imageName);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, SandboxExceptionMsgEnum.DOCKER_UPLOAD_IMAGES_ERROR.getMessage());
        }
    }
    //将已存在镜像状态设置未true;
    private static void checkImagesExists(DockerClient dockerClient, DockerFlagManager dockerFlagManager) {
        List<Image> images = dockerClient.listImagesCmd().withShowAll(true).exec();

        for (LanguageEnum value : LanguageEnum.values()) {
            String imageName = value.getImageName();
            images.forEach(image -> {
                String[] repoTags = image.getRepoTags();
                if(repoTags != null){
                    Arrays.stream(repoTags).forEach(tag -> {
                        if (imageName.equals(tag)) {
                            dockerFlagManager.markSuccess(imageName);
                        }
                    });
                }
            });
        }
    }
}
