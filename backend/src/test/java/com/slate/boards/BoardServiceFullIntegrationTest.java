package com.slate.boards;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.slate.boards.BoardController.BoardPostRequest;
import com.slate.boards.BoardController.WorkRequest;
import com.slate.common.SlateException;
import com.slate.operations.AuditLogService;
import com.slate.operations.RequestLogContext;
import com.slate.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BoardServiceFullIntegrationTest {

    @Mock BoardMapper boardMapper;
    @Mock AuditLogService auditLogService;
    @Mock WorkFileService workFileService;

    private BoardService service;

    @BeforeEach
    void setUp() {
        service = new BoardService(
                boardMapper, null, auditLogService, workFileService, null, null, null, new RequestLogContext("test-salt")
        );
    }

    @Test
    void freeFilterAndSortArePassedWithoutCrossingCategory() {
        when(boardMapper.selectPosts(eq("FREE"), eq("likes"), eq("camera"), eq("QUESTION"), eq(null), eq(null), eq(20), eq(7L), eq(false)))
                .thenReturn(List.of());

        service.posts("FREE", "likes", " camera ", "QUESTION", null, null, 20, 7L, false);

        verify(boardMapper).selectPosts("FREE", "likes", "camera", "QUESTION", null, null, 20, 7L, false);
    }

    @Test
    void invalidCategorySortAndCrossCategoryFilterAreRejected() {
        assertThatThrownBy(() -> service.posts("POPULAR", "latest", null, null, null, null, 20, null, false))
                .isInstanceOf(SlateException.class);
        assertThatThrownBy(() -> service.posts("WORK", "random", null, null, null, null, 20, null, false))
                .isInstanceOf(SlateException.class);
        assertThatThrownBy(() -> service.posts("WORK", "latest", null, "NOTICE", null, null, 20, null, false))
                .isInstanceOf(SlateException.class);
    }

    @Test
    void regularUserCannotCreateNoticeButAdminCan() {
        BoardPostRequest notice = new BoardPostRequest("FREE", "NOTICE", "공지", "내용", "PUBLIC", null);

        assertThatThrownBy(() -> service.createPost(user(1L, "USER"), notice))
                .isInstanceOf(SlateException.class)
                .hasMessageContaining("관리자만");

        when(boardMapper.insertPost(any())).thenAnswer(invocation -> {
            Map<String, Object> row = invocation.getArgument(0);
            row.put("postId", 10L);
            return 1;
        });
        when(boardMapper.selectPostById(eq(10L), eq(9L), anyBoolean())).thenReturn(postRow());
        when(boardMapper.selectReviewsByPostId(10L)).thenReturn(List.of());

        service.createPost(user(9L, "ADMIN"), notice);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(boardMapper).insertPost(captor.capture());
        assertThat(captor.getValue()).containsEntry("freeCategory", "NOTICE");
    }

    @Test
    void rankingsUseWorkTypeAndCurrentUserContracts() {
        when(boardMapper.selectWorkRanking("POPULAR_WORK", "MUSIC_VIDEO", null, 7L, 10)).thenReturn(List.of());
        when(boardMapper.selectPopularProfileRanking(7L, 10)).thenReturn(List.of());

        service.rankings("POPULAR_WORK", "MUSIC_VIDEO", null, 10, 7L);
        service.rankings("POPULAR_PROFILE", null, null, 10, 7L);

        verify(boardMapper).selectWorkRanking("POPULAR_WORK", "MUSIC_VIDEO", null, 7L, 10);
        verify(boardMapper).selectPopularProfileRanking(7L, 10);
    }

    @Test
    void weeklyRankingAndGenreFilterUseTheExplicitMapperContract() {
        when(boardMapper.selectWorkRanking("WEEKLY_WORK", null, 3L, null, 5)).thenReturn(List.of());

        service.rankings("WEEKLY_WORK", null, 3L, 5, null);

        verify(boardMapper).selectWorkRanking("WEEKLY_WORK", null, 3L, null, 5);
        assertThatThrownBy(() -> service.rankings("YEARLY_WORK", null, null, 5, null))
                .isInstanceOf(SlateException.class);
    }

    @Test
    void workCreationStoresOrderedGenresAndReturnsThemFromDetail() {
        WorkRequest work = new WorkRequest(
                null, null, "작업물", "설명", "MANUAL", "SHORT_FILM", List.of(3L, 4L), null, "PUBLIC"
        );
        BoardPostRequest request = new BoardPostRequest("WORK", null, "작업물", "설명", "PUBLIC", work);
        when(boardMapper.countActiveGenres(List.of(3L, 4L))).thenReturn(2);
        when(boardMapper.insertPost(any())).thenAnswer(invocation -> {
            Map<String, Object> row = invocation.getArgument(0);
            row.put("postId", 10L);
            return 1;
        });
        when(boardMapper.insertWork(any())).thenAnswer(invocation -> {
            Map<String, Object> row = invocation.getArgument(0);
            row.put("workId", 20L);
            return 1;
        });
        Map<String, Object> post = postRow();
        post.put("category", "WORK");
        post.put("freeCategory", null);
        Map<String, Object> storedWork = new LinkedHashMap<>();
        storedWork.put("workId", 20L);
        storedWork.put("workType", "SHORT_FILM");
        when(boardMapper.selectPostById(10L, 9L, false)).thenReturn(post);
        when(boardMapper.selectWorkByPostId(10L)).thenReturn(storedWork);
        when(boardMapper.selectWorkGenresByWorkId(20L)).thenReturn(List.of(
                Map.of("genreId", 3L, "name", "드라마", "sortOrder", 0),
                Map.of("genreId", 4L, "name", "코미디", "sortOrder", 1)
        ));
        when(boardMapper.selectReviewsByPostId(10L)).thenReturn(List.of());

        Map<String, Object> created = service.createPost(user(9L, "USER"), request);

        verify(boardMapper).insertWorkGenre(20L, 3L, 0);
        verify(boardMapper).insertWorkGenre(20L, 4L, 1);
        assertThat((Map<String, Object>) created.get("work"))
                .containsEntry("genres", boardMapper.selectWorkGenresByWorkId(20L));
    }

    @Test
    void likeToggleRecountsActiveLikesBeforeReturningCount() {
        when(boardMapper.selectPostById(10L, 7L, false)).thenReturn(postRow());
        when(boardMapper.selectLikeActiveYn(10L, 7L)).thenReturn(null);
        when(boardMapper.insertLike(10L, 7L)).thenReturn(1);
        when(boardMapper.recountLikeCount(10L)).thenReturn(1);
        when(boardMapper.selectLikeCount(10L)).thenReturn(3);

        Map<String, Object> result = service.toggleLike(7L, 10L);

        verify(boardMapper).recountLikeCount(10L);
        assertThat(result).containsEntry("active", true).containsEntry("likeCount", 3);
    }

    private CurrentUser user(Long userId, String accountType) {
        return new CurrentUser(userId, "user@example.com", "사용자", accountType, List.of("ROLE_" + accountType));
    }

    private Map<String, Object> postRow() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("postId", 10L);
        row.put("authorUserId", 9L);
        row.put("category", "FREE");
        row.put("freeCategory", "NOTICE");
        row.put("title", "공지");
        row.put("content", "내용");
        row.put("visibility", "PUBLIC");
        row.put("status", "PUBLISHED");
        return row;
    }
}
