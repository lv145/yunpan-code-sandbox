package com.lgg.yunpancodesandbox;


import com.lgg.yunpancodesandbox.model.ExecuteCodeRequest;
import com.lgg.yunpancodesandbox.model.ExecuteCodeResponse;

/**
 * 代码沙箱接口定义
 */
public interface CodeSandbox {

    /**
     * 执行代码
     *
     * @param executeCodeRequest
     * @return
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
