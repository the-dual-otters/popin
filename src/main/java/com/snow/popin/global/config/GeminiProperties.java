package com.snow.popin.global.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Component
@ConfigurationProperties(prefix = "ai.gemini")
public class GeminiProperties {

    private Api api = new Api();
    private Integer timeout = 30000; // 기본값 30초

    public void setTimeout(Integer timeout) {
        this.timeout = timeout != null ? timeout : 30000;
    }

    @Getter
    public static class Api {
        private String key;
        private String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

        public void setKey(String key) {
            this.key = key;
        }

        public void setUrl(String url) {
            this.url = url != null ? url : "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
        }
    }
}