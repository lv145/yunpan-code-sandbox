package com.lgg.yunpancodesandbox.utils;

import cn.hutool.core.util.StrUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class StdinNormalizeUtil {
    private static final Pattern WHITESPACE_COMMA_PATTERN = Pattern.compile("[\\s,]+");

    /**
     * 【空白、逗号混合归一模式】
     * 把 空格 / tab / 换行 / 逗号 全部当成分隔符，切割出token，再用统一分隔符拼接
     * ⚠️ 注意：这个返回的串，不能直接丢给 Scanner.nextInt！逗号归一后你要自己split解析
     *
     * @param rawInput   原始输入，例如 "1,2 3\n4"
     * @return 归一后的字符串
     */
    public static String formattingParameter(String rawInput) {
        if (StrUtil.isBlank(rawInput)) {
            return rawInput;
        }
        String[] tokens = WHITESPACE_COMMA_PATTERN.split(rawInput.trim());
        List<String> validTokenList = new ArrayList<>();
        for (String token : tokens) {
            if (StrUtil.isNotBlank(token)) {
                validTokenList.add(token);
            }
        }
        String joined = String.join("\r\n" , validTokenList);
        // 开启兜底，并且末尾不等于目标分隔符才追加
        if ( !joined.endsWith("\r\n")) {
            joined = joined + "\r\n";
        }
        return joined;
    }
}
