package com.slate.boards;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "slate.youtube")
public record YoutubeProperties(String apiKey, String baseUrl) {

    private static final String DEFAULT_BASE_URL = "https://www.googleapis.com/youtube/v3";

    public YoutubeProperties {
        apiKey = StringUtils.hasText(apiKey) ? apiKey.trim() : "";
        baseUrl = StringUtils.hasText(baseUrl) ? baseUrl.trim() : DEFAULT_BASE_URL;
    }

    public boolean hasApiKey() {
        return StringUtils.hasText(apiKey);
    }
}
