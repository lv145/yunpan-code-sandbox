package com.lgg.yunpancodesandbox.enums;

import io.netty.util.internal.StringUtil;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
public enum LanguageEnum {
    JAVA(
            "java",
            "openjdk:8-alpine",
            "javac -encoding UTF-8 %s",
            "java -cp /app Main"
    ),
    C(
            "c",
            "gcc:12-alpine",
            "gcc %s -o Main -O2 -Wall",
            "./Main"
    ),
    CPP(
            "cpp",
            "gcc:12-alpine",
            "g++ %s -o Main -O2 -Wall",
            "./Main"
    ),
    PYTHON2(
            "python",
            "python:2.7-alpine",
            "",
            "python %s"
    ),
    PYTHON3(
            "python3",
            "python:3.11-alpine",
            "",
            "python3 %s"
    ),
    JS(
            "js",
            "node:20-alpine",
            "",
            "node %s"
    ),
    TYPESCRIPT(
            "ts",
            "node:20-alpine",
            "tsc %s",
            "node %s"
    ),
    GO(
            "go",
            "golang:1.22-alpine",
            "go build -o Main %s",
            "./Main"
    ),
    PHP(
            "php",
            "php:8.3-alpine",
            "",
            "php %s"
    );

    /**
     * 对外语言标识，传给沙箱request的language字段
     * 例：python3
     */
    private final String language;
    /**
     * docker镜像名称带版本
     */
    private final String imageName;
    /**
     * 编译命令模板，%s = 容器内文件绝对路径
     */
    private final String compileCmdTemplate;
    /**
     * 运行命令模板
     */
    private final String runCmdTemplate;

    LanguageEnum(String language, String imageName, String compileCmdTemplate, String runCmdTemplate) {
        this.language = language;
        this.imageName = imageName;
        this.compileCmdTemplate = compileCmdTemplate;
        this.runCmdTemplate = runCmdTemplate;
    }

    /**
     * 根据请求传入的language字符串查找枚举
     * @param language 请求参数（java / python3 / cpp）
     * @return LanguageEnum
     */
    public static LanguageEnum getByLanguage(String language) {
        if (language == null) {
            return null;
        }
        for (LanguageEnum value : values()) {
            if (value.getLanguage().equals(language)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 是否需要编译
     */
    public boolean needCompile() {
        return compileCmdTemplate != null && !StringUtils.isBlank(compileCmdTemplate);
    }

    /**
     * 获取编译完整命令
     * @param containerFilePath 容器内文件路径 /app/Main.java
     */
    public String getCompileCmd(String containerFilePath) {
        if (!needCompile()) {
            return "";
        }
        return String.format(this.compileCmdTemplate, containerFilePath);
    }

    /**
     * 获取运行完整命令
     */
    public String getRunCmd(String containerFilePath) {
        return String.format(this.runCmdTemplate, containerFilePath);
    }
}
