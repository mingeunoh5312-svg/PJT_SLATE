package com.slate.follows;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.slate.common.SlateException;
import com.slate.notifications.NotificationService;
import com.slate.operations.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class FollowServiceTest {

    private static final Long ACTOR_USER_ID = 1L;
    private static final Long ACTOR_PROFILE_ID = 10L;
    private static final Long TARGET_USER_ID = 2L;
    private static final Long TARGET_PROFILE_ID = 20L;

    private RecordingFollowMapper mapper;
    private RecordingNotificationService notificationService;
    private RecordingAuditLogService auditLogService;
    private FollowService followService;

    @BeforeEach
    void setUp() {
        mapper = new RecordingFollowMapper();
        mapper.actors.put(ACTOR_USER_ID, actor());
        mapper.targets.put(TARGET_PROFILE_ID, target());
        mapper.followerCount = 8;
        mapper.followingCount = 3;
        notificationService = new RecordingNotificationService();
        auditLogService = new RecordingAuditLogService();
        followService = new FollowService(mapper.proxy(), notificationService, auditLogService);
    }

    @Test
    void followCreatesRelationAndSendsNotificationAndAuditOnce() {
        mapper.insertResult = 1;

        Map<String, Object> result = followService.follow(ACTOR_USER_ID, TARGET_PROFILE_ID);

        assertThat(result)
                .containsEntry("profileId", TARGET_PROFILE_ID)
                .containsEntry("userId", TARGET_USER_ID)
                .containsEntry("following", true)
                .containsEntry("changed", true)
                .containsEntry("followerCount", 8)
                .containsEntry("followingCount", 3);
        assertThat(notificationService.sendCount).isEqualTo(1);
        assertThat(notificationService.recipientUserId).isEqualTo(TARGET_USER_ID);
        assertThat(notificationService.senderUserId).isEqualTo(ACTOR_USER_ID);
        assertThat(notificationService.notificationType).isEqualTo("SOCIAL");
        assertThat(notificationService.body).isEqualTo("촬영감독님이 회원님을 팔로우하기 시작했습니다.");
        assertThat(notificationService.targetId).isEqualTo(ACTOR_PROFILE_ID);
        assertThat(auditLogService.recordCount).isEqualTo(1);
        assertThat(auditLogService.actionType).isEqualTo("USER_FOLLOW_CREATED");
        assertThat(auditLogService.targetId).isEqualTo(TARGET_PROFILE_ID);
    }

    @Test
    void duplicateFollowIsIdempotentWithoutNotificationOrAudit() {
        mapper.insertResult = 0;

        Map<String, Object> result = followService.follow(ACTOR_USER_ID, TARGET_PROFILE_ID);

        assertThat(result)
                .containsEntry("following", true)
                .containsEntry("changed", false);
        assertThat(notificationService.sendCount).isZero();
        assertThat(auditLogService.recordCount).isZero();
    }

    @Test
    void unfollowDeletesRelationAndRecordsAudit() {
        mapper.deleteResult = 1;

        Map<String, Object> result = followService.unfollow(ACTOR_USER_ID, TARGET_PROFILE_ID);

        assertThat(result)
                .containsEntry("following", false)
                .containsEntry("changed", true);
        assertThat(auditLogService.recordCount).isEqualTo(1);
        assertThat(auditLogService.actionType).isEqualTo("USER_FOLLOW_DELETED");
        assertThat(notificationService.sendCount).isZero();
    }

    @Test
    void unfollowMissingRelationIsIdempotent() {
        mapper.deleteResult = 0;

        Map<String, Object> result = followService.unfollow(ACTOR_USER_ID, TARGET_PROFILE_ID);

        assertThat(result)
                .containsEntry("following", false)
                .containsEntry("changed", false);
        assertThat(notificationService.sendCount).isZero();
        assertThat(auditLogService.recordCount).isZero();
    }

    @Test
    void selfFollowIsRejected() {
        mapper.targets.put(ACTOR_PROFILE_ID, actor());

        assertStatus(() -> followService.follow(ACTOR_USER_ID, ACTOR_PROFILE_ID), HttpStatus.BAD_REQUEST);
        assertThat(mapper.callCount("insertIgnoreFollow")).isZero();
    }

    @Test
    void missingTargetIsNotFound() {
        assertStatus(() -> followService.follow(ACTOR_USER_ID, 999L), HttpStatus.NOT_FOUND);
    }

    @Test
    void privateOrInactiveTargetIsNotFoundWithoutDisclosingItsExistence() {
        assertStatus(() -> followService.follow(ACTOR_USER_ID, 30L), HttpStatus.NOT_FOUND);
        assertStatus(() -> followService.follow(ACTOR_USER_ID, 31L), HttpStatus.NOT_FOUND);
    }

    @Test
    void invalidActorIsForbidden() {
        assertStatus(() -> followService.follow(3L, TARGET_PROFILE_ID), HttpStatus.FORBIDDEN);
        assertThat(mapper.callCount("selectPublicTargetByProfileId")).isZero();
    }

    @Test
    void missingAuthenticationIsUnauthorized() {
        assertStatus(() -> followService.follow(null, TARGET_PROFILE_ID), HttpStatus.UNAUTHORIZED);
        assertThat(mapper.totalCalls()).isZero();
        assertThat(notificationService.sendCount).isZero();
        assertThat(auditLogService.recordCount).isZero();
    }

    @Test
    void statusForOwnProfileNeverReportsFollowing() {
        mapper.targets.put(ACTOR_PROFILE_ID, actor());
        mapper.followerCount = 4;
        mapper.followingCount = 6;

        Map<String, Object> result = followService.status(ACTOR_USER_ID, ACTOR_PROFILE_ID);

        assertThat(result)
                .containsEntry("ownProfile", true)
                .containsEntry("following", false)
                .doesNotContainKey("changed");
        assertThat(mapper.callCount("countRelation")).isZero();
    }

    @Test
    void followersNormalizesPaginationAndCalculatesHasMore() {
        for (int index = 0; index < 50; index++) {
            mapper.followers.add(Map.of("profileId", 100L + index, "userId", 200L + index));
        }
        mapper.followerCount = 51;

        Map<String, Object> result = followService.followers(ACTOR_USER_ID, TARGET_PROFILE_ID, 200, -5);

        assertThat(result)
                .containsEntry("totalCount", 51)
                .containsEntry("limit", 50)
                .containsEntry("offset", 0)
                .containsEntry("hasMore", true);
        assertThat(mapper.lastLimit).isEqualTo(50);
        assertThat(mapper.lastOffset).isZero();
    }

    @Test
    void followingListResponseDoesNotExposePrivateAccountFields() {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("profileId", 21L);
        item.put("userId", 22L);
        item.put("nickname", "영화인");
        item.put("displayName", "영화인");
        item.put("shortIntro", "촬영과 조명을 맡고 있습니다.");
        item.put("publicRegionName", "서울");
        item.put("experienceLevel", "Y3_10");
        item.put("followingByCurrentUser", true);
        mapper.following.add(item);
        mapper.followingCount = 1;

        Map<String, Object> result = followService.following(ACTOR_USER_ID, TARGET_PROFILE_ID, null, null);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertThat(items).singleElement().satisfies(row -> assertThat(row)
                .doesNotContainKeys("email", "phone", "loginId", "passwordHash"));
        assertThat(result).containsEntry("hasMore", false);
    }

    private Map<String, Object> actor() {
        return Map.of(
                "profileId", ACTOR_PROFILE_ID,
                "userId", ACTOR_USER_ID,
                "nickname", "카메라맨",
                "displayName", "촬영감독"
        );
    }

    private Map<String, Object> target() {
        return Map.of(
                "profileId", TARGET_PROFILE_ID,
                "userId", TARGET_USER_ID,
                "nickname", "영화인",
                "displayName", "영화인"
        );
    }

    private void assertStatus(Runnable action, HttpStatus status) {
        assertThatThrownBy(action::run)
                .isInstanceOf(SlateException.class)
                .satisfies(error -> assertThat(((SlateException) error).status()).isEqualTo(status));
    }

    private static final class RecordingFollowMapper implements InvocationHandler {

        private final Map<Long, Map<String, Object>> actors = new HashMap<>();
        private final Map<Long, Map<String, Object>> targets = new HashMap<>();
        private final Map<String, Integer> calls = new HashMap<>();
        private final List<Map<String, Object>> followers = new ArrayList<>();
        private final List<Map<String, Object>> following = new ArrayList<>();
        private int insertResult;
        private int deleteResult;
        private int relationCount;
        private int followerCount;
        private int followingCount;
        private int lastLimit;
        private int lastOffset;

        FollowMapper proxy() {
            return (FollowMapper) Proxy.newProxyInstance(
                    FollowMapper.class.getClassLoader(),
                    new Class<?>[] {FollowMapper.class},
                    this
            );
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            calls.merge(method.getName(), 1, Integer::sum);
            return switch (method.getName()) {
                case "selectActiveActorByUserId" -> actors.get((Long) args[0]);
                case "selectPublicTargetByProfileId" -> targets.get((Long) args[0]);
                case "countRelation" -> relationCount;
                case "insertIgnoreFollow" -> insertResult;
                case "deleteFollow" -> deleteResult;
                case "countPublicFollowers" -> followerCount;
                case "countPublicFollowing" -> followingCount;
                case "selectPublicFollowers" -> {
                    lastLimit = (Integer) args[2];
                    lastOffset = (Integer) args[3];
                    yield List.copyOf(followers);
                }
                case "selectPublicFollowing" -> {
                    lastLimit = (Integer) args[2];
                    lastOffset = (Integer) args[3];
                    yield List.copyOf(following);
                }
                default -> null;
            };
        }

        int callCount(String methodName) {
            return calls.getOrDefault(methodName, 0);
        }

        int totalCalls() {
            return calls.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    private static final class RecordingNotificationService extends NotificationService {

        private int sendCount;
        private Long recipientUserId;
        private Long senderUserId;
        private String notificationType;
        private String body;
        private Long targetId;

        RecordingNotificationService() {
            super(null, null, null, null);
        }

        @Override
        public void send(
                Long recipientUserId,
                Long senderUserId,
                String notificationType,
                String title,
                String body,
                String targetType,
                Long targetId
        ) {
            sendCount++;
            this.recipientUserId = recipientUserId;
            this.senderUserId = senderUserId;
            this.notificationType = notificationType;
            this.body = body;
            this.targetId = targetId;
        }
    }

    private static final class RecordingAuditLogService extends AuditLogService {

        private int recordCount;
        private String actionType;
        private Long targetId;

        RecordingAuditLogService() {
            super(null, null, null);
        }

        @Override
        public void recordAudit(
                Long actorUserId,
                String actionType,
                String targetType,
                Long targetId,
                Object before,
                Object after
        ) {
            recordCount++;
            this.actionType = actionType;
            this.targetId = targetId;
        }
    }
}
