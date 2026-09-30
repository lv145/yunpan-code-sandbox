package com.lgg.yunpancodesandbox;

import com.lgg.yunpancodesandbox.model.ExecuteCodeRequest;
import com.lgg.yunpancodesandbox.model.ExecuteCodeResponse;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Java 原生代码沙箱实现（直接复用模板方法）
 */
@Component
public class JavaNativeCodeSandbox extends JavaCodeSandboxTemplate {
    @Resource
    private JavaDockerCodeSandbox dockerCodeSandbox;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return dockerCodeSandbox.executeCode(executeCodeRequest);
    }
}
