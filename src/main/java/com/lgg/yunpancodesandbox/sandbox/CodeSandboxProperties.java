package com.lgg.yunpancodesandbox.sandbox;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 代码沙箱配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "code-sandbox")
public class CodeSandboxProperties {

    /**
     * 最大输出长度，单位字节
     */
    private Integer maxOutputLength;

    /**
     * 鉴权请求头名称
     */
    private String authRequestHeader;

    /**
     * 鉴权密钥
     */
    private String authRequestSecret;

    /**
     * 超时时间，毫秒
     */
    private Long timeOut;

    /**
     * 本地临时代码目录名 tmpCode
     */
    private String globalCodeDirName;

    /**
     * Docker容器内代码目录 /app
     */
    private String globalDockerCodeDirPath;

    /**
     * 全局代码文件名 Main（不带后缀）
     */
    private String globalFileName;

    /**
     * 错误提示语言 zh / en
     */
    private String errorLang;
}
