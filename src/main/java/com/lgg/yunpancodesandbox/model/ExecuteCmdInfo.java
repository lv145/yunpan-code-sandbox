package com.lgg.yunpancodesandbox.model;


import com.lgg.yunpancodesandbox.enums.SandBoxResponseStatus;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class ExecuteCmdInfo extends ExecuteCodeInfo {
    private Integer exitValue;
    private SandBoxResponseStatus status;
    private Boolean isRunFailed;
    private Boolean isOutTime;
    private Boolean isOutputLimitExceeded;
}
