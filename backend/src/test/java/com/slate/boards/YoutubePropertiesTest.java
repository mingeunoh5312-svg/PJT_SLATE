package com.slate.boards;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

class YoutubePropertiesTest {

    @Test
    void bindsEmptyApiKeyWithoutFailing() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("slate.youtube.api-key", "")
                .withProperty("slate.youtube.base-url", "https://www.googleapis.com/youtube/v3");

        YoutubeProperties properties = Binder.get(environment)
                .bind("slate.youtube", YoutubeProperties.class)
                .get();

        assertThat(properties.apiKey()).isEmpty();
        assertThat(properties.hasApiKey()).isFalse();
        assertThat(properties.baseUrl()).isEqualTo("https://www.googleapis.com/youtube/v3");
    }

    @Test
    void usesDefaultBaseUrlWhenBlank() {
        YoutubeProperties properties = new YoutubeProperties("  test-key  ", " ");

        assertThat(properties.apiKey()).isEqualTo("test-key");
        assertThat(properties.hasApiKey()).isTrue();
        assertThat(properties.baseUrl()).isEqualTo("https://www.googleapis.com/youtube/v3");
    }
}
