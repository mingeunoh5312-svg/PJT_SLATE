package com.slate.boards;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slate.common.SlateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class YoutubeClientTest {

    private YoutubeClient youtubeClient;

    @BeforeEach
    void setUp() {
        youtubeClient = new YoutubeClient(
                new YoutubeProperties("", "https://www.googleapis.com/youtube/v3"),
                new ObjectMapper(),
                RestClient.builder()
        );
    }

    @Test
    void extractsVideoIdFromWatchUrl() {
        assertThat(youtubeClient.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=10s"))
                .isEqualTo("dQw4w9WgXcQ");
    }

    @Test
    void extractsVideoIdFromShortUrl() {
        assertThat(youtubeClient.extractVideoId("https://youtu.be/dQw4w9WgXcQ?si=test"))
                .isEqualTo("dQw4w9WgXcQ");
    }

    @Test
    void extractsVideoIdFromEmbedUrl() {
        assertThat(youtubeClient.extractVideoId("https://www.youtube.com/embed/dQw4w9WgXcQ"))
                .isEqualTo("dQw4w9WgXcQ");
    }

    @Test
    void extractsVideoIdFromShortsUrl() {
        assertThat(youtubeClient.extractVideoId("https://www.youtube.com/shorts/dQw4w9WgXcQ?feature=share"))
                .isEqualTo("dQw4w9WgXcQ");
    }

    @Test
    void rejectsInvalidUrl() {
        assertThatThrownBy(() -> youtubeClient.extractVideoId("https://example.com/watch?v=dQw4w9WgXcQ"))
                .isInstanceOf(SlateException.class)
                .hasMessage("유튜브 URL 형식이 올바르지 않습니다.");
    }

    @Test
    void parsesIsoDurationToSeconds() {
        assertThat(youtubeClient.parseDurationSeconds("PT1M30S")).isEqualTo(90);
        assertThat(youtubeClient.parseDurationSeconds("PT2H5M10S")).isEqualTo(7510);
        assertThat(youtubeClient.parseDurationSeconds("PT45S")).isEqualTo(45);
    }

    @Test
    void fetchMetadataRequiresApiKeyAfterUrlValidation() {
        assertThatThrownBy(() -> youtubeClient.fetchMetadata("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
                .isInstanceOf(SlateException.class)
                .hasMessage("YouTube API Key가 설정되어 있지 않습니다.");
    }
}
