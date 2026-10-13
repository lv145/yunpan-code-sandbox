package com.lgg.yunpancodesandbox.exception;


import com.lgg.yunpancodesandbox.common.BaseResponse;
import com.lgg.yunpancodesandbox.common.ErrorCode;
import com.lgg.yunpancodesandbox.common.ResultUtils;
import com.lgg.yunpancodesandbox.enums.SandboxExceptionMsgEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author lgg
 * @from 云盘在线判题平台
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }
}
