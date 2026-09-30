package com.lgg.yunpancodesandbox.enums;

public enum SandBoxResponseStatus {
    SUCCESS(0,"运行成功"),
    RUN_FAILED(1,"运行失败"),
    COMPILE_FAILED(2,"编译失败"),
    RUN_TIME_OUT(3,"运行超时");
    private String message;
    private Integer code;

    SandBoxResponseStatus() {
    }

    SandBoxResponseStatus(String message, Integer code) {
        this.message = message;
        this.code = code;
    }


    SandBoxResponseStatus(Integer code, String message){
        this.message = message;
        this.code = code;
    }

    /**
     * 获取
     * @return message
     */
    public String getMessage() {
        return message;
    }
    /**
     * 获取
     * @return code
     */
    public Integer getCode() {
        return code;
    }

}
