package com.lgg.yunpancodesandbox.utils;


import com.lgg.yunpancodesandbox.model.enums.LanguageEnum;
import com.lgg.yunpancodesandbox.sandbox.CodeSandboxProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * OJ 沙箱 stderr 多语言异常信息解析工具
 */
@Component
public class CodeErrorMsgUtil {
    @Resource
    private CodeSandboxProperties properties;


    public static String filterPathToFileName(String content) {
        // 匹配所有形如 /a/b/c/文件名 的路径，把目录部分删掉，只保留文件名
        return content.replaceAll("/[^\\s:]+/([^/\\s:]+)", "$1");
    }

    /**
     * 解析用户代码运行异常堆栈，返回友好提示（支持中英文 + 多语言：java / c / python3）
     * @param language 语言标识：java / c / python3
     * @param stderr 沙箱原始stderr
     * @return 友好提示文本
     */
    public String parseUserCodeError(String language, String stderr) {
        if (StringUtils.isBlank(stderr)) {
            return "";
        }
        String err = stderr.trim();
        boolean isEn = "en".equals(properties.getErrorLang());

        // ====================== PYTHON3 ======================
        if (LanguageEnum.PYTHON3.getLanguage().equals(language)) {
            if (err.contains("IndentationError")) {
                return isEn ? "IndentationError: indentation error" : "缩进错误：缩进格式不正确";
            }
            if (err.contains("SyntaxError")) {
                return isEn ? "SyntaxError: code syntax error" : "语法错误：代码语法非法";
            }
            if (err.contains("NameError")) {
                return isEn ? "NameError: variable not defined" : "变量未定义";
            }
            if (err.contains("ZeroDivisionError")) {
                return isEn ? "ZeroDivisionError: division by zero" : "算术异常：除零错误";
            }
            if (err.contains("IndexError")) {
                return isEn ? "IndexError: list index out of range" : "数组下标越界";
            }
            if (err.contains("KeyError")) {
                return isEn ? "KeyError: key not found" : "字典键不存在";
            }
            if (err.contains("TypeError")) {
                return isEn ? "TypeError: type mismatch" : "类型错误，类型不匹配";
            }
            if (err.contains("EOFError")) {
                return isEn ? "EOFError: insufficient input" : "输入不足，读取到文件末尾";
            }
            // python兜底
            return isEn ? "Python code runtime error" : "Python代码运行错误";
        }

        // ====================== C语言 ======================
        if (LanguageEnum.C.getLanguage().equals(language)) {
            // gcc编译错误
            if (err.contains("error:")) {
                return isEn ? "C code compile failed" : "C代码编译失败";
            }
            // 运行时段错误
            if (err.contains("segmentation fault") || err.contains("SIGSEGV")) {
                return isEn ? "Segmentation fault: invalid memory access" : "段错误：非法内存访问";
            }
            if (err.contains("floating point exception")) {
                return isEn ? "Floating point exception: division by zero" : "浮点异常：除零错误";
            }
            // C兜底
            return isEn ? "C code runtime error" : "C代码运行错误";
        }

        // ====================== JAVA 原有逻辑 ======================
        if (LanguageEnum.JAVA.getLanguage().equals(language)) {
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
        }

        // 全局兜底
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
