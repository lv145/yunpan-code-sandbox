package com.lgg.yunpancodesandbox.model.enums;

public enum SandBoxResponseStatus {
    SUCCESS(0, "运行成功"),
    RUN_FAILED(1, "运行失败RE"),
    COMPILE_FAILED(2, "编译失败CE"),
    RUN_TIME_OUT(3, "运行超时TLE"),
    OUTPUT_LIMIT_EXCEEDED(4, "输出超限OLE"),
    COMPILE_TIMEOUT(5, "编译超时"),

    SANDBOX_ERROR(99, "沙箱底层异常");

    private final Integer code;
    private final String message;

    SandBoxResponseStatus(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public Integer getCode() {
        return code;
    }
    public static Integer getCodeResponseStatus(String message) {
        for (SandBoxResponseStatus value : SandBoxResponseStatus.values()) {
            if (value.getMessage().equals(message)) {
                return value.code;
            }
        }
        return null;
    }

}
