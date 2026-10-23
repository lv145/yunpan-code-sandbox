package com.lgg.yunpancodesandbox.model;


import com.lgg.yunpancodesandbox.enums.SandBoxResponseStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
public class ExecuteCmdInfo extends ExecuteCodeInfo {
    private Integer exitValue;
    private SandBoxResponseStatus status;
}
