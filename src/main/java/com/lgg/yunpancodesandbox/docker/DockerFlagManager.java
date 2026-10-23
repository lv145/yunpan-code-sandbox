package com.lgg.yunpancodesandbox.docker;

import cn.hutool.core.util.StrUtil;
import com.lgg.yunpancodesandbox.enums.LanguageEnum;
import io.netty.util.internal.StringUtil;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Dragon
 */
@Slf4j
@Component
@ToString
public class DockerFlagManager {
    // key:镜像名称，value：是否拉取成功
    private final ConcurrentHashMap<String, Boolean> imagePullFlagMap = new ConcurrentHashMap<>();
    @PostConstruct
    public void init() {
        //默认全部设为false
        for (LanguageEnum value : LanguageEnum.values()) {
            String imageName = value.getImageName();
            markFail(imageName);

        }
    }
    // 设置标记
    public void markSuccess(String imageName) {
        imagePullFlagMap.put(imageName, true);
    }


    public void markFail(String imageName) {
        imagePullFlagMap.put(imageName, false);
    }

    // 【其他类调用这个方法查询】
    public boolean isImageReady(String imageName) {
        return Boolean.TRUE.equals(imagePullFlagMap.get(imageName));
    }

    public void clear() {
        imagePullFlagMap.clear();
    }

}
