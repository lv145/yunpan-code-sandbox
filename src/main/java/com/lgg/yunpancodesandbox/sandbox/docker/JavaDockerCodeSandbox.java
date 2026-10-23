package com.lgg.yunpancodesandbox.sandbox.docker;

import com.lgg.yunpancodesandbox.enums.LanguageEnum;

import com.lgg.yunpancodesandbox.sandbox.template.DockerCodeSandboxTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JavaDockerCodeSandbox extends DockerCodeSandboxTemplate {

    @Override
    protected LanguageEnum getLanguage() {
        return LanguageEnum.JAVA;
    }

}
