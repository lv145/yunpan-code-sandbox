package com.lgg.yunpancodesandbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.lgg.yunpancodesandbox.utils.DockerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class DockerClientConfig {


    @Bean
    public DockerClient dockerClient() {
        // 1. 读取docker连接配置（默认读取系统环境变量，本地docker默认unix socket）
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("unix:///var/run/docker.sock")
                .build();

        // 2. 实例化 HttpClient5 传输客户端（重点！消除警告的核心）
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(180))
                .build();

        // 3. 构造DockerClient，显式传入httpClient
        return DockerClientImpl.getInstance(config, httpClient);
    }

    @Bean
    public CommandLineRunner prePullImageRunner(DockerClient dockerClient, DockerFlagManager dockerFlagManager) {
        return args -> {
            // 新开线程执行pull，主线程不卡
            new Thread(() -> {
                try {
                   DockerUtils.pullLanguageImages(dockerClient, dockerFlagManager);
                    log.info("镜像{}异步拉取成功", dockerFlagManager);
                } catch (Exception e) {
                    log.error("镜像{}异步拉取失败", dockerFlagManager.toString(),e);
                }
            }).start();
        };
    }




}
