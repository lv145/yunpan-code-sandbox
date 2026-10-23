package com.lgg.yunpancodesandbox.enums;

import lombok.Getter;

@Getter
public enum SandboxExceptionMsgEnum {

    // ========== 编译阶段异常 ==========
    COMPILE_ERROR("编译失败"),
    // ========== 沙箱底层系统异常（放顶层message，不要放到judgeInfo） ==========
    DOCKER_ERROR("Docker服务异常"),
    DOCKER_UPLOAD_IMAGES_ERROR("Docker镜像下载异常");

    private final String message;

    SandboxExceptionMsgEnum(String message) {
        this.message = message;
    }
}
