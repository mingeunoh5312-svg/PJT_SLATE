package com.slate.profiles;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "slate.public-data.kobis")
public record KobisProperties(String apiKey, String baseUrl) {

    private static final String DEFAULT_BASE_URL = "http://www.kobis.or.kr/kobisopenapi/webservice/rest";

    public KobisProperties {
        apiKey = StringUtils.hasText(apiKey) ? apiKey.trim() : "";
        baseUrl = StringUtils.hasText(baseUrl) ? baseUrl.trim() : DEFAULT_BASE_URL;
    }

    public boolean hasApiKey() {
        return StringUtils.hasText(apiKey);
    }
}
