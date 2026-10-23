package com.lgg.yunpancodesandbox.sandbox.docker;

import com.lgg.yunpancodesandbox.enums.LanguageEnum;

import com.lgg.yunpancodesandbox.sandbox.template.DockerCodeSandboxTemplate;
import org.springframework.stereotype.Component;

/**
 * Python3 Docker 代码沙箱实现。
 */
@Component
public class Python3DockerCodeSandbox extends DockerCodeSandboxTemplate {

    @Override
    protected LanguageEnum getLanguage() {
        return LanguageEnum.PYTHON3;
    }

}



