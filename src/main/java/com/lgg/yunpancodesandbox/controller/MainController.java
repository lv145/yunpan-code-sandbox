package com.lgg.yunpancodesandbox.controller;

import com.lgg.yunpancodesandbox.common.BaseResponse;
import com.lgg.yunpancodesandbox.common.ErrorCode;
import com.lgg.yunpancodesandbox.common.ResultUtils;
import com.lgg.yunpancodesandbox.model.enums.LanguageEnum;
import com.lgg.yunpancodesandbox.sandbox.CodeSandbox;
import com.lgg.yunpancodesandbox.model.ExecuteCodeResponse;
import com.lgg.yunpancodesandbox.sandbox.CodeSandboxFactory;
import com.lgg.yunpancodesandbox.model.ExecuteCodeRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController("/")
public class MainController {

    // 定义鉴权请求头和密钥
    @Value("${code-sandbox.auth-request-header}")
    private  String authRequestHeader;
    @Value("${code-sandbox.auth-request-secret}")
    private String authRequestSecret;

    @Resource
    private CodeSandboxFactory codeSandboxFactory;

    @GetMapping("/health")
    public String healthCheck() {
        return "ok";
    }

    /**
     * 执行代码
     *
     * @param executeCodeRequest
     * @return
     */
    @PostMapping("/executeCode")
    BaseResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest request){
        // 基本的认证
        String authHeader = request.getHeader(authRequestHeader);
        if (!authRequestSecret.equals(authHeader)) {
            return ResultUtils.error(ErrorCode.NO_AUTH_ERROR,ErrorCode.NO_AUTH_ERROR.getMessage());
        }
        ExecuteCodeRequest req = executeCodeRequest;
        if (req == null
                || StringUtils.isBlank(req.getCode())
                || StringUtils.isBlank(req.getLanguage())) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR,ErrorCode.PARAMS_ERROR.getMessage());
        }

        String language = executeCodeRequest.getLanguage();
        CodeSandbox sandbox = codeSandboxFactory.getSandbox(LanguageEnum.getEnumByLanguage(language));
        ExecuteCodeResponse executeCodeResponse = sandbox.executeCode(executeCodeRequest);
        return ResultUtils.success(executeCodeResponse);
    }
}


