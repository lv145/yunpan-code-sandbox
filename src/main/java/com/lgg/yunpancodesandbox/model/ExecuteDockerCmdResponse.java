package com.lgg.yunpancodesandbox.model;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ExecuteDockerCmdResponse {
    private String resultMessage;
    private String errorMessage;
    private Long time;
    private Long maxMemory;
    private Boolean isOutTime;
    private Boolean isOutputLimitExceeded;
}
