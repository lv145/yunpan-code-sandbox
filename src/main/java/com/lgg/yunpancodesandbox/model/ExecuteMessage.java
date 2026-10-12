package com.lgg.yunpancodesandbox.model;

import lombok.Builder;
import lombok.Data;

/**
 * 进程执行信息
 */
@Data
@Builder
public class ExecuteMessage {

    private Integer exitValue;

    private String resultMessage;

    private String errorMessage;

    private Long time;

    private Long maxMemory;
}
