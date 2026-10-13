package com.lgg.yunpancodesandbox.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OJ 沙箱 stderr 多语言异常信息解析工具
 */
@Component
public class CodeErrorMsgUtil {
    @Value("${code_sandbox.jvm_error_lang}")
    public String lang ;
    @Value("${code_sandbox.global_java_class_name}")
    private  String globalJavaClassName ;

    private Pattern compilePathPattern;

    // ✅ 用 @PostConstruct，等属性注入完成后再初始化正则
    @PostConstruct
    public void initPattern() {
        if (StringUtils.isBlank(globalJavaClassName)) {
            return;
        }
        // 按小数点分割文件名 Main.java -> [Main, java]
        String[] fileNameArray = globalJavaClassName.split("\\.");
        if (fileNameArray.length >= 2) {
            String patternStr = ".*(?=" + fileNameArray[0] + "\\." + fileNameArray[1] + ")";
            compilePathPattern = Pattern.compile(patternStr);
        }
    }
    /**
     * 过滤编译错误信息，移除前面的绝对路径
     * @param rawMsg 原始javac编译输出
     * @return 脱敏后的编译信息
     */
    public  String filterCompileMessage(String rawMsg) {
        if (StringUtils.isBlank(rawMsg)) {
            return rawMsg;
        }
        Matcher matcher = compilePathPattern.matcher(rawMsg);
        return matcher.replaceAll("");
    }
    /**
     * 解析用户代码运行异常堆栈，返回友好提示（支持中英文）
     * @param stderr 沙箱原始stderr
     * @return 友好提示文本
     */
    public  String parseUserCodeError(String stderr) {

        String err = stderr.trim();
        boolean isEn = "en".equals(lang);

        if (err.contains("NumberFormatException")) {
            return isEn ? "Number format exception: cannot convert string to number" : "数字格式异常：字符串无法转为数字";
        }
        if (err.contains("NullPointerException")) {
            return isEn ? "Null pointer exception" : "空指针异常";
        }
        if (err.contains("ArrayIndexOutOfBoundsException")) {
            return isEn ? "Array index out of bounds" : "数组下标越界";
        }
        if (err.contains("StringIndexOutOfBoundsException")) {
            return isEn ? "String index out of bounds" : "字符串下标越界";
        }
        if (err.contains("ArithmeticException")) {
            return isEn ? "Arithmetic exception: division by zero" : "算术异常：除零错误";
        }
        if (err.contains("ClassCastException")) {
            return isEn ? "Class cast exception" : "类型转换异常";
        }
        if (err.contains("NoSuchElementException")) {
            return isEn ? "Input missing / input mismatch" : "输入不匹配/缺少输入";
        }
        if (err.contains("StackOverflowError")) {
            return isEn ? "Stack overflow: too deep recursion" : "栈溢出：大概率递归深度过大";
        }
        if (err.contains("OutOfMemoryError")) {
            return isEn ? "Out of memory" : "内存溢出";
        }
        if (err.contains("IllegalArgumentException")) {
            return isEn ? "Illegal argument" : "参数非法";
        }
        if (err.contains("IOException")) {
            return isEn ? "IO exception" : "IO异常";
        }
        // Java编译错误匹配
        if (err.contains("error:") && err.contains("Main.java")) {
            return isEn ? "Code compilation failed" : "代码编译失败";
        }

        // 默认兜底
        return isEn ? "Code execution error" : "代码运行错误";
    }


    /**
     * 提取原始异常第一行（管理员后台查看）
     */
    public static String getRawErrorFirstLine(String stderr) {
        if (StringUtils.isBlank(stderr)) {
            return "";
        }
        String[] lines = stderr.split("\\r?\\n");
        for (String line : lines) {
            if (StringUtils.isNotBlank(line)) {
                return line.trim();
            }
        }
        return "";
    }
}
