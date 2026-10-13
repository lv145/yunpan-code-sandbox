package com.lgg.yunpancodesandbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
    private List<String> outputList;
    /**
     * 判题信息
     */
    private  List<JudgeMessage> judgeMessageList;
}
