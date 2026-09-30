package com.lgg.yunpancodesandbox.controller;

import com.lgg.yunpancodesandbox.JavaNativeCodeSandbox;
import com.lgg.yunpancodesandbox.model.ExecuteCodeRequest;
import com.lgg.yunpancodesandbox.model.ExecuteCodeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController("/")
public class MainController {

    // 定义鉴权请求头和密钥
    private static final String AUTH_REQUEST_HEADER = "auth";

    private static final String AUTH_REQUEST_SECRET = "secretKey";

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
    ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest request,
                                    HttpServletResponse response) throws IOException {
        // 基本的认证
        String authHeader = request.getHeader(AUTH_REQUEST_HEADER);
        if (!AUTH_REQUEST_SECRET.equals(authHeader)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return ExecuteCodeResponse.builder().message("鉴权失败!").build();
        }
        if (executeCodeRequest == null||executeCodeRequest.getCode().isEmpty()
                ||executeCodeRequest.getInputList().isEmpty()||executeCodeRequest.getLanguage().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return ExecuteCodeResponse.builder().message("参数不能为空!").build();
        }
        return javaNativeCodeSandbox.executeCode(executeCodeRequest);
    }
}
