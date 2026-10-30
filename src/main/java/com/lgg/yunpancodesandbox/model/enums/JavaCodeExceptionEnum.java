package com.lgg.yunpancodesandbox.model.enums;

import org.apache.commons.lang3.StringUtils;

public enum JavaCodeExceptionEnum {
    NUMBER_FORMAT_EXCEPTION("NumberFormatException", "数字格式异常：字符串无法转为数字", "Number format exception: cannot convert string to number"),
    NULL_POINTER_EXCEPTION("NullPointerException", "空指针异常", "Null pointer exception"),
    ARRAY_INDEX_OUT_OF_BOUNDS("ArrayIndexOutOfBoundsException", "数组下标越界", "Array index out of bounds"),
    STRING_INDEX_OUT_OF_BOUNDS("StringIndexOutOfBoundsException", "字符串下标越界", "String index out of bounds"),
    ARITHMETIC_EXCEPTION("ArithmeticException", "算术异常：除零错误", "Arithmetic exception: division by zero"),
    CLASS_CAST_EXCEPTION("ClassCastException", "类型转换异常", "Class cast exception"),
    NO_SUCH_ELEMENT_EXCEPTION("NoSuchElementException", "输入不匹配/缺少输入", "Input missing / input mismatch"),
    STACK_OVERFLOW_ERROR("StackOverflowError", "栈溢出：大概率递归深度过大", "Stack overflow: too deep recursion"),
    OUT_OF_MEMORY_ERROR("OutOfMemoryError", "内存溢出", "Out of memory"),
    ILLEGAL_ARGUMENT_EXCEPTION("IllegalArgumentException", "参数非法", "Illegal argument"),
    IO_EXCEPTION("IOException", "IO异常", "IO exception"),
    COMPILE_ERROR("error:Main.java", "代码编译失败", "Code compilation failed"),
    DEFAULT_ERROR("", "代码运行错误", "Code execution error");

    /** 匹配关键字 */
    private final String keyword;
    /** 中文提示 */
    private final String cnMsg;
    /** 英文提示 */
    private final String enMsg;

    JavaCodeExceptionEnum(String keyword, String cnMsg, String enMsg) {
        this.keyword = keyword;
        this.cnMsg = cnMsg;
        this.enMsg = enMsg;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getCnMsg() {
        return cnMsg;
    }

    public String getEnMsg() {
        return enMsg;
    }

    /**
     * 根据stderr内容匹配异常，返回友好提示
     * @param stderr 原始错误输出
     * @param lang cn / en
     * @return 友好提示文本
     */
    public static String matchError(String stderr, String lang) {
        String err = stderr.trim();
        for (JavaCodeExceptionEnum e : JavaCodeExceptionEnum.values()) {
            String keyword = e.getKeyword();
            if (StringUtils.isBlank(keyword)) {
                continue;
            }
            if (err.contains(keyword)) {
                return "en".equals(lang)? e.enMsg: e.cnMsg;
            }
        }
        return null;
    }
}
