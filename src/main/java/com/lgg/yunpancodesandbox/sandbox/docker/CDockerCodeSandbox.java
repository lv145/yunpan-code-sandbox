package com.lgg.yunpancodesandbox.sandbox.docker;

import com.lgg.yunpancodesandbox.enums.LanguageEnum;

import com.lgg.yunpancodesandbox.sandbox.template.DockerCodeSandboxTemplate;
import org.springframework.stereotype.Component;

/**
 * C Docker 代码沙箱实现。
 */
@Component
public class CDockerCodeSandbox extends DockerCodeSandboxTemplate {

    @Override
    protected LanguageEnum getLanguage() {
        return LanguageEnum.C;
    }

}



