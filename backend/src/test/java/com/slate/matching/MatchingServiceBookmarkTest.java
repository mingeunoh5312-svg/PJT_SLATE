package com.slate.matching;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import com.slate.matching.MatchingController.BookmarkRequest;
import com.slate.operations.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MatchingServiceBookmarkTest {

    private int insertResult;
    private int deleteResult;
    private Long queriedUserId;
    private Long deletedUserId;
    private MatchingService service;
    private TrackingAuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new TrackingAuditLogService();
        MatchingMapper mapper = (MatchingMapper) Proxy.newProxyInstance(
                MatchingMapper.class.getClassLoader(),
                new Class<?>[]{MatchingMapper.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "insertBookmark" -> insertResult;
                    case "selectTeamBookmarks" -> {
                        queriedUserId = (Long) args[0];
                        yield List.of(Map.of(
                                "bookmarkId", 91L,
                                "teamId", 10L,
                                "teamName", "남산 새벽팀"
                        ));
                    }
                    case "selectTeamGenres" -> List.of(Map.of("genreId", 1L, "name", "드라마"));
                    case "selectOpenRecruitmentSlotsByTeamId" -> List.of(Map.of(
                            "recruitmentId", 20L,
                            "slotId", 30L,
                            "roleName", "촬영"
                    ));
                    case "deleteBookmark" -> {
                        deletedUserId = (Long) args[0];
                        yield deleteResult;
                    }
                    default -> defaultValue(method.getReturnType());
                }
        );
        service = new MatchingService(mapper, null, auditLogService);
    }

    @Test
    void duplicateBookmarkReportsAlreadySavedWithoutDuplicateAudit() {
        insertResult = 0;

        Map<String, Object> result = service.bookmark(7L, new BookmarkRequest("TEAM", 10L));

        assertThat(result)
                .containsEntry("saved", true)
                .containsEntry("created", false)
                .containsEntry("alreadySaved", true);
        assertThat(auditLogService.calls).isZero();
    }

    @Test
    void savedTeamsAreLoadedForCurrentUserWithGenresAndOpenRoles() {
        List<Map<String, Object>> result = service.bookmarks(7L, "team");

        assertThat(queriedUserId).isEqualTo(7L);
        assertThat(result).singleElement().satisfies(team -> {
            assertThat(team).containsEntry("teamId", 10L).containsEntry("savedByCurrentUser", true);
            assertThat((List<?>) team.get("genres")).hasSize(1);
            assertThat((List<?>) team.get("openRoles")).hasSize(1);
        });
    }

    @Test
    void bookmarkDeletionUsesCurrentUserOwnership() {
        deleteResult = 1;

        Map<String, Object> result = service.deleteBookmark(7L, "TEAM", 10L);

        assertThat(deletedUserId).isEqualTo(7L);
        assertThat(result).containsEntry("saved", false).containsEntry("removed", true);
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) return false;
        if (returnType == int.class) return 0;
        if (returnType == long.class) return 0L;
        return null;
    }

    private static final class TrackingAuditLogService extends AuditLogService {
        private int calls;

        private TrackingAuditLogService() {
            super(null, null, null);
        }

        @Override
        public void recordAudit(Long actorUserId, String actionType, String targetType, Long targetId, Object before, Object after) {
            calls += 1;
        }
    }
}
