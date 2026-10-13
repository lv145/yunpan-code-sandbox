package com.lgg.yunpancodesandbox.controller;

import com.lgg.yunpancodesandbox.common.BaseResponse;
import com.lgg.yunpancodesandbox.common.ErrorCode;
import com.lgg.yunpancodesandbox.common.ResultUtils;
import com.lgg.yunpancodesandbox.template.JavaNativeCodeSandbox;
import com.lgg.yunpancodesandbox.model.ExecuteCodeRequest;
import com.lgg.yunpancodesandbox.model.ExecuteCodeResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController("/")
public class MainController {

    // 定义鉴权请求头和密钥
    @Value("${code_sandbox.auth_request_header}")
    private  String authRequestHeader;
    @Value("${code_sandbox.auth_request_secret}")
    private String authRequestSecret;

    @Resource
    private JavaNativeCodeSandbox javaNativeCodeSandbox;

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
                || CollectionUtils.isEmpty(req.getInputList())
                || StringUtils.isBlank(req.getLanguage())) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR,ErrorCode.PARAMS_ERROR.getMessage());
        }
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);
        return ResultUtils.success(executeCodeResponse);
    }
}
