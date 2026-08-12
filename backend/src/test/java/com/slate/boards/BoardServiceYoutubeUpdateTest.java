package com.slate.boards;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slate.boards.BoardController.BoardPostRequest;
import com.slate.boards.BoardController.WorkRequest;
import com.slate.common.SlateException;
import com.slate.operations.AuditLogService;
import com.slate.operations.RequestLogContext;
import com.slate.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class BoardServiceYoutubeUpdateTest {

    private static final Long USER_ID = 1L;
    private static final Long POST_ID = 10L;

    private RecordingBoardMapper boardMapper;
    private RecordingWorkFileService workFileService;
    private FakeYoutubeClient youtubeClient;
    private BoardService boardService;

    @BeforeEach
    void setUp() {
        boardMapper = new RecordingBoardMapper();
        workFileService = new RecordingWorkFileService();
        youtubeClient = new FakeYoutubeClient();
        boardService = new BoardService(
                boardMapper.proxy(),
                null,
                new NoopAuditLogService(),
                workFileService,
                null,
                null,
                youtubeClient,
                new RequestLogContext("test-salt")
        );
    }

    @Test
    void updatePostRefreshesYoutubeMetadataWhenYoutubeUrlIsProvided() {
        youtubeClient.metadata = new YoutubeVideoMetadata(
                "newVideo12345",
                "https://www.youtube.com/embed/newVideo12345",
                "https://www.youtube.com/embed/newVideo12345",
                "https://youtu.be/newVideo12345",
                "새 영상 제목",
                "새 채널",
                "https://img.youtube.com/vi/newVideo12345/hqdefault.jpg",
                214
        );

        boardService.updatePost(currentUser(), POST_ID, request(work(null, null, "YOUTUBE", "https://youtu.be/newVideo12345")));

        assertThat(boardMapper.updatedWork)
                .containsEntry("mediaType", "YOUTUBE")
                .containsEntry("fileId", null)
                .containsEntry("youtubeUrl", "https://www.youtube.com/embed/newVideo12345")
                .containsEntry("youtubeVideoId", "newVideo12345")
                .containsEntry("youtubeTitle", "새 영상 제목")
                .containsEntry("youtubeChannelTitle", "새 채널")
                .containsEntry("youtubeThumbnailUrl", "https://img.youtube.com/vi/newVideo12345/hqdefault.jpg")
                .containsEntry("youtubeDurationSeconds", 214);
        assertThat(youtubeClient.requestedUrls).containsExactly("https://youtu.be/newVideo12345");
    }

    @Test
    void updatePostClearsYoutubeMetadataWhenSwitchingToServerUpload() {
        boardService.updatePost(currentUser(), POST_ID, request(work(55L, null, "SERVER_UPLOAD", null)));

        assertThat(boardMapper.updatedWork)
                .containsEntry("mediaType", "SERVER_UPLOAD")
                .containsEntry("fileId", 55L)
                .containsEntry("youtubeUrl", null)
                .containsEntry("youtubeVideoId", null)
                .containsEntry("youtubeTitle", null)
                .containsEntry("youtubeChannelTitle", null)
                .containsEntry("youtubeThumbnailUrl", null)
                .containsEntry("youtubeDurationSeconds", null);
        assertThat(workFileService.lastUserId).isEqualTo(USER_ID);
        assertThat(workFileService.lastTeamId).isNull();
        assertThat(workFileService.lastFileId).isEqualTo(55L);
        assertThat(youtubeClient.requestedUrls).isEmpty();
    }

    @Test
    void updatePostClearsYoutubeMetadataWhenSwitchingToManualWork() {
        boardService.updatePost(currentUser(), POST_ID, request(work(null, null, "MANUAL", null)));

        assertThat(boardMapper.updatedWork)
                .containsEntry("mediaType", "MANUAL")
                .containsEntry("fileId", null)
                .containsEntry("youtubeUrl", null)
                .containsEntry("youtubeVideoId", null)
                .containsEntry("youtubeTitle", null)
                .containsEntry("youtubeChannelTitle", null)
                .containsEntry("youtubeThumbnailUrl", null)
                .containsEntry("youtubeDurationSeconds", null);
        assertThat(youtubeClient.requestedUrls).isEmpty();
    }

    @Test
    void updatePostRejectsYoutubeUrlAndFileIdTogether() {
        assertThatThrownBy(() -> boardService.updatePost(
                currentUser(),
                POST_ID,
                request(work(55L, null, "YOUTUBE", "https://youtu.be/newVideo12345"))
        ))
                .isInstanceOf(SlateException.class)
                .hasMessage("유튜브 URL과 서버 업로드 파일은 동시에 연결할 수 없습니다.");

        assertThat(boardMapper.updatedWork).isNull();
        assertThat(youtubeClient.requestedUrls).isEmpty();
    }

    private BoardPostRequest request(WorkRequest work) {
        return new BoardPostRequest(
                "WORK",
                null,
                "수정된 게시글",
                "수정된 내용",
                "PUBLIC",
                work
        );
    }

    private WorkRequest work(Long fileId, Long teamId, String mediaType, String youtubeUrl) {
        return new WorkRequest(
                teamId,
                fileId,
                "수정된 작업물",
                "수정된 작업물 설명",
                mediaType,
                "MUSIC_VIDEO",
                List.of(),
                youtubeUrl,
                "PUBLIC"
        );
    }

    private CurrentUser currentUser() {
        return new CurrentUser(USER_ID, "user@example.com", "사용자", "USER", List.of("ROLE_USER"));
    }

    private static Map<String, Object> existingPost() {
        Map<String, Object> post = new LinkedHashMap<>();
        post.put("postId", POST_ID);
        post.put("authorUserId", USER_ID);
        post.put("category", "WORK");
        post.put("title", "기존 게시글");
        post.put("content", "기존 내용");
        post.put("visibility", "PUBLIC");
        post.put("status", "PUBLISHED");
        return post;
    }

    private static Map<String, Object> existingYoutubeWork() {
        Map<String, Object> work = new LinkedHashMap<>();
        work.put("workId", 100L);
        work.put("boardPostId", POST_ID);
        work.put("ownerUserId", USER_ID);
        work.put("mediaType", "YOUTUBE");
        work.put("youtubeUrl", "https://www.youtube.com/embed/oldVideo12345");
        work.put("youtubeVideoId", "oldVideo12345");
        work.put("youtubeTitle", "기존 영상 제목");
        work.put("youtubeChannelTitle", "기존 채널");
        work.put("youtubeThumbnailUrl", "https://example.com/old-thumb.jpg");
        work.put("youtubeDurationSeconds", 120);
        work.put("status", "PUBLISHED");
        return work;
    }

    private static final class RecordingBoardMapper implements InvocationHandler {

        private Map<String, Object> updatedWork;

        BoardMapper proxy() {
            return (BoardMapper) Proxy.newProxyInstance(
                    BoardMapper.class.getClassLoader(),
                    new Class<?>[] {BoardMapper.class},
                    this
            );
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "selectPostById" -> existingPost();
                case "updatePost" -> 1;
                case "selectWorkByPostId" -> updatedWork == null ? existingYoutubeWork() : updatedWork;
                case "updateWork" -> {
                    updatedWork = new LinkedHashMap<>(castMap(args[0]));
                    yield 1;
                }
                case "selectReviewsByPostId" -> new ArrayList<>();
                default -> defaultValue(method.getReturnType());
            };
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> castMap(Object value) {
            return (Map<String, Object>) value;
        }

        private Object defaultValue(Class<?> returnType) {
            if (returnType == int.class) {
                return 0;
            }
            if (returnType == boolean.class) {
                return false;
            }
            if (List.class.isAssignableFrom(returnType)) {
                return List.of();
            }
            return null;
        }
    }

    private static final class RecordingWorkFileService extends WorkFileService {

        private Long lastUserId;
        private Long lastTeamId;
        private Long lastFileId;

        RecordingWorkFileService() {
            super(null, null, null, null, "uploads", "ffprobe");
        }

        @Override
        public void assertFileUsable(Long userId, Long teamId, Long fileId) {
            lastUserId = userId;
            lastTeamId = teamId;
            lastFileId = fileId;
        }
    }

    private static final class FakeYoutubeClient extends YoutubeClient {

        private final List<String> requestedUrls = new ArrayList<>();
        private YoutubeVideoMetadata metadata;

        FakeYoutubeClient() {
            super(new YoutubeProperties("test-key", "https://www.googleapis.com/youtube/v3"), new ObjectMapper(), RestClient.builder());
        }

        @Override
        public YoutubeVideoMetadata fetchMetadata(String youtubeUrl) {
            requestedUrls.add(youtubeUrl);
            return metadata;
        }
    }

    private static final class NoopAuditLogService extends AuditLogService {

        NoopAuditLogService() {
            super(null, null, null);
        }

        @Override
        public void recordAudit(Long actorUserId, String actionType, String targetType, Long targetId, Object before, Object after) {
        }

        @Override
        public void recordOperation(String logLevel, String eventCode, String message, Object context) {
        }
    }
}
