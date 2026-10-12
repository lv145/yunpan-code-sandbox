package com.lgg.yunpancodesandbox.exception;


import com.lgg.yunpancodesandbox.common.ErrorCode;
import com.lgg.yunpancodesandbox.enums.SandBoxResponseStatus;
import com.lgg.yunpancodesandbox.enums.SandboxExceptionMsgEnum;

/**
 * 自定义异常类
 *
 * @author lgg
 * @from 云盘在线判题平台
 */
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
    public BusinessException(SandboxExceptionMsgEnum errorCode) {
        super(errorCode.getMessage());
        this.code = SandBoxResponseStatus.RUN_FAILED.getCode();
    }
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public int getCode() {
        return code;
    }
}
