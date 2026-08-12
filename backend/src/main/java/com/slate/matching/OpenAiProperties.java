package com.slate.matching;

import com.slate.common.SlateException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "slate.ai.openai")
public record OpenAiProperties(String apiKey, String baseUrl, String model) {

    public static final String MISSING_API_KEY_MESSAGE = "AI 추천 기능을 사용하려면 OpenAI API Key 설정이 필요합니다.";

    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private static final String DEFAULT_MODEL = "gpt-4o-mini";

    public OpenAiProperties {
        apiKey = StringUtils.hasText(apiKey) ? apiKey.trim() : "";
        baseUrl = StringUtils.hasText(baseUrl) ? trimTrailingSlash(baseUrl.trim()) : DEFAULT_BASE_URL;
        model = StringUtils.hasText(model) ? model.trim() : DEFAULT_MODEL;
    }

    public boolean hasApiKey() {
        return StringUtils.hasText(apiKey);
    }

    public String requireApiKey() {
        if (!hasApiKey()) {
            throw new SlateException(MISSING_API_KEY_MESSAGE);
        }
        return apiKey;
    }

    private static String trimTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/") && result.length() > 1) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
