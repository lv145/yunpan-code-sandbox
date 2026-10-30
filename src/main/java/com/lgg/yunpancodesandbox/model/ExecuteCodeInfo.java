package com.lgg.yunpancodesandbox.model;


import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * 进程执行信息
 */

@SuperBuilder
@Data

public class ExecuteCodeInfo {

    private String resultMessage;

    private String errorMessage;

    private Long time;

    private Long memory;
}
