package com.lgg.yunpancodesandbox.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 判题信息
 */
@Data
@Builder
public class JudgeMessage {

    /**
     * 程序错误信息
     */
    private String errorMessage;

    /**
     * 消耗内存
     */
    private Long memory;

    /**
     * 消耗时间（KB）
     */
    private Long time;
}
