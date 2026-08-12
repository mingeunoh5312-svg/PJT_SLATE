package com.slate.boards;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slate.boards.BoardController.YoutubePreviewRequest;
import com.slate.common.ApiResponse;
import com.slate.common.SlateException;
import com.slate.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class BoardControllerYoutubePreviewTest {

    private YoutubeClient youtubeClient;
    private BoardController boardController;

    @BeforeEach
    void setUp() {
        youtubeClient = new FakeYoutubeClient();
        boardController = new BoardController(
                null,
                null,
                null,
                youtubeClient
        );
    }

    @Test
    void previewsYoutubeMetadataForLoggedInUser() {
        String inputUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
        ((FakeYoutubeClient) youtubeClient).metadata = new YoutubeVideoMetadata(
                "dQw4w9WgXcQ",
                "https://www.youtube.com/embed/dQw4w9WgXcQ",
                "https://www.youtube.com/embed/dQw4w9WgXcQ",
                inputUrl,
                "영상 제목",
                "채널명",
                "https://example.com/thumb.jpg",
                123
        );

        ApiResponse<BoardController.YoutubePreviewResponse> response = boardController.previewYoutube(
                currentUser(),
                new YoutubePreviewRequest(inputUrl)
        );

        assertThat(response.success()).isTrue();
        assertThat(response.data().videoId()).isEqualTo("dQw4w9WgXcQ");
        assertThat(response.data().embedUrl()).isEqualTo("https://www.youtube.com/embed/dQw4w9WgXcQ");
        assertThat(response.data().youtubeUrl()).isEqualTo(inputUrl);
        assertThat(response.data().title()).isEqualTo("영상 제목");
        assertThat(response.data().channelTitle()).isEqualTo("채널명");
        assertThat(response.data().thumbnailUrl()).isEqualTo("https://example.com/thumb.jpg");
        assertThat(response.data().durationSeconds()).isEqualTo(123);
    }

    @Test
    void previewRequiresLogin() {
        assertThatThrownBy(() -> boardController.previewYoutube(
                null,
                new YoutubePreviewRequest("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        ))
                .isInstanceOf(SlateException.class)
                .hasMessage("로그인이 필요합니다.");
    }

    private CurrentUser currentUser() {
        return new CurrentUser(1L, "user@example.com", "사용자", "USER", List.of("ROLE_USER"));
    }

    private static class FakeYoutubeClient extends YoutubeClient {

        private YoutubeVideoMetadata metadata;

        FakeYoutubeClient() {
            super(new YoutubeProperties("test-key", "https://www.googleapis.com/youtube/v3"), new ObjectMapper(), RestClient.builder());
        }

        @Override
        public YoutubeVideoMetadata fetchMetadata(String youtubeUrl) {
            return metadata;
        }
    }
}
