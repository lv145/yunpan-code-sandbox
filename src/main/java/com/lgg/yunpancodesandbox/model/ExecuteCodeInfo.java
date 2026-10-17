package com.lgg.yunpancodesandbox.model;

import com.lgg.yunpancodesandbox.enums.SandBoxResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
