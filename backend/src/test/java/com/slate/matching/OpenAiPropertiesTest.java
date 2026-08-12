package com.slate.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slate.common.SlateException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.client.RestClient;

class OpenAiPropertiesTest {

    @Test
    void bindsEmptyApiKeyWithoutFailing() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("slate.ai.openai.api-key", "")
                .withProperty("slate.ai.openai.base-url", "https://api.openai.com/v1")
                .withProperty("slate.ai.openai.model", "gpt-4o-mini");

        OpenAiProperties properties = Binder.get(environment)
                .bind("slate.ai.openai", OpenAiProperties.class)
                .get();

        assertThat(properties.apiKey()).isEmpty();
        assertThat(properties.hasApiKey()).isFalse();
        assertThat(properties.baseUrl()).isEqualTo("https://api.openai.com/v1");
        assertThat(properties.model()).isEqualTo("gpt-4o-mini");
    }

    @Test
    void usesDefaultsWhenOptionalValuesAreBlank() {
        OpenAiProperties properties = new OpenAiProperties("  test-openai-key  ", " ", " ");

        assertThat(properties.apiKey()).isEqualTo("test-openai-key");
        assertThat(properties.hasApiKey()).isTrue();
        assertThat(properties.baseUrl()).isEqualTo("https://api.openai.com/v1");
        assertThat(properties.model()).isEqualTo("gpt-4o-mini");
    }

    @Test
    void trimsTrailingSlashFromBaseUrl() {
        OpenAiProperties properties = new OpenAiProperties("test-openai-key", "https://api.openai.com/v1///", "test-model");

        assertThat(properties.baseUrl()).isEqualTo("https://api.openai.com/v1");
        assertThat(properties.model()).isEqualTo("test-model");
    }

    @Test
    void requireApiKeyThrowsClearMessageWhenMissing() {
        OpenAiProperties properties = new OpenAiProperties("", "https://api.openai.com/v1", "gpt-4o-mini");

        assertThatThrownBy(properties::requireApiKey)
                .isInstanceOf(SlateException.class)
                .hasMessage(OpenAiProperties.MISSING_API_KEY_MESSAGE);
    }

    @Test
    void clientRequiresApiKeyBeforeCallingOpenAi() {
        OpenAiClient client = new OpenAiClient(
                new OpenAiProperties("", "https://api.openai.com/v1", "gpt-4o-mini"),
                new ObjectMapper(),
                RestClient.builder()
        );

        assertThatThrownBy(() -> client.postJson("/chat/completions", java.util.Map.of()))
                .isInstanceOf(SlateException.class)
                .hasMessage(OpenAiProperties.MISSING_API_KEY_MESSAGE);
    }
}
