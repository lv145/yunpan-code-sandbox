package com.lgg.yunpancodesandbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class DockerClientConfig {

    @Bean
    public DockerClient dockerClient() {
        // 1. 读取docker连接配置（默认读取系统环境变量，本地docker默认unix socket）
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .build();

        // 2. 实例化 HttpClient5 传输客户端（重点！消除警告的核心）
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(180))
                .build();

        // 3. 构造DockerClient，显式传入httpClient
        DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);
        return dockerClient;
    }
}
