package com.lgg.yunpancodesandbox.enums;

import lombok.Getter;

@Getter
public enum SandboxExceptionMsgEnum {

    // ========== 编译阶段异常 ==========
    COMPILE_ERROR("编译失败"),
    COMPILE_TIMEOUT("编译超时"),
    COMPILE_OOM("编译内存超限"),

    // ========== 运行阶段异常 ==========
    RUN_TIMEOUT("运行超时(TLE)"),
    RUN_OOM("内存超限(MLE)"),
    RUN_ERROR("运行时错误(RE)"),

    // ========== 沙箱底层系统异常（放顶层message，不要放到judgeInfo） ==========
    SANDBOX_CONTAINER_CREATE_FAIL("容器创建失败"),
    SANDBOX_DOCKER_ERROR("Docker服务异常"),
    SANDBOX_INTERNAL_ERROR("沙箱内部未知异常");

    private final String message;

    SandboxExceptionMsgEnum(String message) {
        this.message = message;
    }
}
