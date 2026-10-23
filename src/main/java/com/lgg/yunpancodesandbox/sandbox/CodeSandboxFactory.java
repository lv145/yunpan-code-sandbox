package com.lgg.yunpancodesandbox.sandbox;

import com.lgg.yunpancodesandbox.common.ErrorCode;
import com.lgg.yunpancodesandbox.enums.LanguageEnum;
import com.lgg.yunpancodesandbox.exception.BusinessException;
import com.lgg.yunpancodesandbox.sandbox.docker.CDockerCodeSandbox;
import com.lgg.yunpancodesandbox.sandbox.docker.JavaDockerCodeSandbox;
import com.lgg.yunpancodesandbox.sandbox.docker.Python3DockerCodeSandbox;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Component
public class CodeSandboxFactory {

    @Resource
    private JavaDockerCodeSandbox javaDockerCodeSandbox;
    @Resource
    private Python3DockerCodeSandbox python3DockerCodeSandbox;
    @Resource
    private CDockerCodeSandbox cDockerCodeSandbox;

    private static final Map<LanguageEnum, CodeSandbox> SANDBOX_MAP = new HashMap<>();

    // 初始化注入，也可以用@PostConstruct
    @PostConstruct
    public void init() {
        SANDBOX_MAP.put(LanguageEnum.JAVA, javaDockerCodeSandbox);
        SANDBOX_MAP.put(LanguageEnum.PYTHON3, python3DockerCodeSandbox);
        SANDBOX_MAP.put(LanguageEnum.C, cDockerCodeSandbox);
    }

    public CodeSandbox getSandbox(LanguageEnum languageEnum) {
        CodeSandbox sandbox = SANDBOX_MAP.get(languageEnum);
        if (sandbox == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的编程语言");
        }
        return sandbox;
    }
}
