package com.lgg.yunpancodesandbox.enums;

import lombok.Getter;

@Getter
public enum SandboxSystemErrorMsgEnum {
    SANDBOX_CONTAINER_CREATE_FAIL("容器创建失败"),
    SANDBOX_CONTAINER_START_FAIL("容器启动失败"),
    SANDBOX_DOCKER_IO_ERROR("Docker IO通信异常"),
    SANDBOX_DOCKER_CLIENT_ERROR("Docker客户端调用异常"),
    SANDBOX_EXEC_CREATE_FAIL("创建容器执行命令失败"),
    SANDBOX_EXEC_ATTACH_ERROR("attach执行流异常"),
    SANDBOX_THREAD_INTERRUPT("执行线程被中断"),
    SANDBOX_FILE_OP_ERROR("代码文件读写异常"),
    SANDBOX_UNKNOWN_ERROR("沙箱内部未知异常");

    private final String message;

    SandboxSystemErrorMsgEnum(String message) {
        this.message = message;
    }
}
