package com.slate.boards;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slate.admin.AdminPermissionService;
import com.slate.operations.AuditLogService;
import com.slate.operations.RequestLogContext;
import com.slate.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class BoardServiceYoutubeDeleteTest {

    private static final Long USER_ID = 1L;
    private static final Long ADMIN_ID = 9L;
    private static final Long POST_ID = 10L;

    private RecordingBoardMapper boardMapper;
    private FakeYoutubeClient youtubeClient;
    private BoardService boardService;
    private AdminBoardService adminBoardService;

    @BeforeEach
    void setUp() {
        boardMapper = new RecordingBoardMapper();
        youtubeClient = new FakeYoutubeClient();
        NoopAuditLogService auditLogService = new NoopAuditLogService();
        NoopAdminPermissionService adminPermissionService = new NoopAdminPermissionService(auditLogService);
        boardService = new BoardService(
                boardMapper.proxy(),
                adminPermissionService,
                auditLogService,
                null,
                null,
                null,
                youtubeClient,
                new RequestLogContext("test-salt")
        );
        adminBoardService = new AdminBoardService(
                boardMapper.proxy(),
                adminPermissionService,
                auditLogService
        );
    }

    @Test
    void deletePostSoftDeletesYoutubeWorkAndPreservesMetadata() {
        boardService.deletePost(currentUser(), POST_ID);

        assertThat(boardMapper.post)
                .containsEntry("status", "AUTHOR_DELETED")
                .containsKey("deletedAt");
        assertThat(boardMapper.work)
                .containsEntry("status", "DELETED")
                .containsEntry("youtubeUrl", "https://www.youtube.com/embed/oldVideo12345")
                .containsEntry("youtubeVideoId", "oldVideo12345")
                .containsEntry("youtubeTitle", "기존 영상 제목")
                .containsEntry("youtubeChannelTitle", "기존 채널")
                .containsEntry("youtubeThumbnailUrl", "https://example.com/old-thumb.jpg")
                .containsEntry("youtubeDurationSeconds", 120);
        assertThat(boardMapper.softDeletedWorkPostIds).containsExactly(POST_ID);
        assertThat(youtubeClient.requestedUrls).isEmpty();
    }

    @Test
    void adminDeletePostSoftDeletesYoutubeWorkAndPreservesMetadata() {
        adminBoardService.deletePost(ADMIN_ID, POST_ID, "운영 정책 위반");

        assertThat(boardMapper.post)
                .containsEntry("status", "ADMIN_DELETED")
                .containsKey("deletedAt");
        assertThat(boardMapper.work)
                .containsEntry("status", "DELETED")
                .containsEntry("youtubeUrl", "https://www.youtube.com/embed/oldVideo12345")
                .containsEntry("youtubeVideoId", "oldVideo12345")
                .containsEntry("youtubeTitle", "기존 영상 제목")
                .containsEntry("youtubeChannelTitle", "기존 채널")
                .containsEntry("youtubeThumbnailUrl", "https://example.com/old-thumb.jpg")
                .containsEntry("youtubeDurationSeconds", 120);
        assertThat(boardMapper.softDeletedWorkPostIds).containsExactly(POST_ID);
        assertThat(youtubeClient.requestedUrls).isEmpty();
    }

    @Test
    void adminRestorePostRestoresDeletedYoutubeWorkAndPreservesMetadata() {
        adminBoardService.deletePost(ADMIN_ID, POST_ID, "운영 정책 위반");
        adminBoardService.restorePost(ADMIN_ID, POST_ID, "오처리 복구");

        assertThat(boardMapper.post)
                .containsEntry("status", "PUBLISHED")
                .containsEntry("deletedAt", null);
        assertThat(boardMapper.work)
                .containsEntry("status", "PUBLISHED")
                .containsEntry("youtubeUrl", "https://www.youtube.com/embed/oldVideo12345")
                .containsEntry("youtubeVideoId", "oldVideo12345")
                .containsEntry("youtubeTitle", "기존 영상 제목")
                .containsEntry("youtubeChannelTitle", "기존 채널")
                .containsEntry("youtubeThumbnailUrl", "https://example.com/old-thumb.jpg")
                .containsEntry("youtubeDurationSeconds", 120);
        assertThat(boardMapper.restoredWorkPostIds).containsExactly(POST_ID);
        assertThat(youtubeClient.requestedUrls).isEmpty();
    }

    private CurrentUser currentUser() {
        return new CurrentUser(USER_ID, "user@example.com", "사용자", "USER", List.of("ROLE_USER"));
    }

    private static Map<String, Object> initialPost() {
        Map<String, Object> post = new LinkedHashMap<>();
        post.put("postId", POST_ID);
        post.put("authorUserId", USER_ID);
        post.put("authorNickname", "사용자");
        post.put("category", "WORK");
        post.put("title", "기존 게시글");
        post.put("content", "기존 내용");
        post.put("visibility", "PUBLIC");
        post.put("status", "PUBLISHED");
        post.put("deletedAt", null);
        return post;
    }

    private static Map<String, Object> initialYoutubeWork() {
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

        private final Map<String, Object> post = initialPost();
        private final Map<String, Object> work = initialYoutubeWork();
        private final List<Long> softDeletedWorkPostIds = new java.util.ArrayList<>();
        private final List<Long> restoredWorkPostIds = new java.util.ArrayList<>();

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
                case "selectPostById", "selectAdminPostById" -> new LinkedHashMap<>(post);
                case "softDeletePost" -> {
                    post.put("status", args[1]);
                    post.put("deletedAt", LocalDateTime.now());
                    yield 1;
                }
                case "softDeleteWorkByPostId" -> {
                    softDeletedWorkPostIds.add((Long) args[0]);
                    work.put("status", "DELETED");
                    work.put("updatedAt", LocalDateTime.now());
                    yield 1;
                }
                case "restorePostAsAdmin" -> {
                    post.put("status", "PUBLISHED");
                    post.put("deletedAt", null);
                    yield 1;
                }
                case "restoreWorkByPostId" -> {
                    restoredWorkPostIds.add((Long) args[0]);
                    work.put("status", "PUBLISHED");
                    work.put("updatedAt", LocalDateTime.now());
                    yield 1;
                }
                default -> defaultValue(method.getReturnType());
            };
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

    private static final class FakeYoutubeClient extends YoutubeClient {

        private final List<String> requestedUrls = new java.util.ArrayList<>();

        FakeYoutubeClient() {
            super(new YoutubeProperties("test-key", "https://www.googleapis.com/youtube/v3"), new ObjectMapper(), RestClient.builder());
        }

        @Override
        public YoutubeVideoMetadata fetchMetadata(String youtubeUrl) {
            requestedUrls.add(youtubeUrl);
            return null;
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

    private static final class NoopAdminPermissionService extends AdminPermissionService {

        NoopAdminPermissionService(AuditLogService auditLogService) {
            super(null, auditLogService);
        }

        @Override
        public void require(Long userId, String permissionCode) {
        }
    }
}
