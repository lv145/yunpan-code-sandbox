package com.lgg.yunpancodesandbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 本地执行返回
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeResponse {

    /**
     * 执行状态
     */
    private Integer status;

    private Long maxMemory;
    private Long maxTime;
    private String compileErrorMessage;
    /**
     * 判题信息
     */
    private  List<ExecuteCodeInfo> executeCodeInfoList;
}
