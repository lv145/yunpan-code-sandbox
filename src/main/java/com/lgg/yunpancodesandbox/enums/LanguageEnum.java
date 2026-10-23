package com.lgg.yunpancodesandbox.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
public enum LanguageEnum {
    JAVA(
            "java",
            "openjdk:8-alpine",
            ".java",
            "javac -encoding UTF-8 %1$s",
            "java -cp %2$s %3$s"
    ),
    C(
            "c",
            "alpine-gcc:lastest",
            ".c",
            "gcc %1$s -o %2$s/%3$s -O2 -Wall",
            "%2$s/%3$s"
    ),
    PYTHON3(
            "python3",
            "python:3.11-alpine",
            ".py",
            "",
            "python3 %1$s"
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
     * 源码文件后缀 .java / .c / .py
     */
    private final String fileSuffix;
    /**
     * 编译命令模板
     * %1$s = 源码完整绝对路径 /app/Main.java
     * %2$s = 容器工作目录 /app
     * %3$s = 执行名称 Main
     */
    private final String compileCmdTemplate;
    /**
     * 运行命令模板
     * %1$s = 源码完整绝对路径
     * %2$s = 容器工作目录
     * %3$s = 执行名称 Main
     */
    private final String runCmdTemplate;

    LanguageEnum(String language, String imageName, String fileSuffix, String compileCmdTemplate, String runCmdTemplate) {
        this.language = language;
        this.imageName = imageName;
        this.fileSuffix = fileSuffix;
        this.compileCmdTemplate = compileCmdTemplate;
        this.runCmdTemplate = runCmdTemplate;
    }

    /**
     * 根据请求传入的language字符串查找枚举
     * @param language 请求参数（java / python3 / c）
     * @return LanguageEnum
     */
    public static LanguageEnum getEnumByLanguage(String language) {
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
    public static String getLanguageByImageName(String imageName) {
        if (imageName == null) {
            return null;
        }
        for (LanguageEnum value : values()) {
            if (value.getImageName().equals(imageName)) {
                return value.getLanguage();
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
     * @param sourceFullPath 源码完整绝对路径 /app/Main.java
     * @param workDir 容器工作目录 /app
     * @param execName 执行名称 Main
     */
    public String getCompileCmd(String sourceFullPath, String workDir, String execName) {
        if (!needCompile()) {
            return "";
        }
        return String.format(this.compileCmdTemplate, sourceFullPath, workDir, execName);
    }

    /**
     * 获取运行完整命令
     * @param sourceFullPath 源码完整绝对路径 /app/uuid/Main.java
     * @param workDir 容器工作目录 /app
     * @param execName 执行名称 Main
     */
    public String getRunCmd(String sourceFullPath, String workDir, String execName) {
        return String.format(this.runCmdTemplate, sourceFullPath, workDir, execName);
    }
}
