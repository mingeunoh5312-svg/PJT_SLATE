package com.slate.follows;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.slate.common.SlateException;
import com.slate.notifications.NotificationService;
import com.slate.operations.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final FollowMapper followMapper;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public FollowService(
            FollowMapper followMapper,
            NotificationService notificationService,
            AuditLogService auditLogService
    ) {
        this.followMapper = followMapper;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Map<String, Object> follow(Long currentUserId, Long profileId) {
        Map<String, Object> actor = requireActor(currentUserId);
        Map<String, Object> target = requirePublicTarget(profileId);
        Long targetUserId = longValue(target.get("userId"));
        rejectSelfFollow(currentUserId, targetUserId);

        boolean changed = followMapper.insertIgnoreFollow(currentUserId, targetUserId) == 1;
        if (changed) {
            Long actorProfileId = longValue(actor.get("profileId"));
            String actorName = displayName(actor);
            notificationService.send(
                    targetUserId,
                    currentUserId,
                    "SOCIAL",
                    "새 팔로워가 생겼습니다.",
                    actorName + "님이 회원님을 팔로우하기 시작했습니다.",
                    "PROFILE",
                    actorProfileId
            );
            auditLogService.recordAudit(
                    currentUserId,
                    "USER_FOLLOW_CREATED",
                    "PROFILE",
                    profileId,
                    null,
                    auditState(currentUserId, targetUserId, true)
            );
        }
        return followState(target, true, changed);
    }

    @Transactional
    public Map<String, Object> unfollow(Long currentUserId, Long profileId) {
        requireActor(currentUserId);
        Map<String, Object> target = requirePublicTarget(profileId);
        Long targetUserId = longValue(target.get("userId"));
        rejectSelfFollow(currentUserId, targetUserId);

        boolean changed = followMapper.deleteFollow(currentUserId, targetUserId) == 1;
        if (changed) {
            auditLogService.recordAudit(
                    currentUserId,
                    "USER_FOLLOW_DELETED",
                    "PROFILE",
                    profileId,
                    auditState(currentUserId, targetUserId, true),
                    auditState(currentUserId, targetUserId, false)
            );
        }
        return followState(target, false, changed);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> status(Long currentUserId, Long profileId) {
        requireActor(currentUserId);
        Map<String, Object> target = requirePublicTarget(profileId);
        Long targetUserId = longValue(target.get("userId"));
        boolean ownProfile = currentUserId.equals(targetUserId);
        boolean following = !ownProfile && followMapper.countRelation(currentUserId, targetUserId) > 0;

        Map<String, Object> result = followState(target, following, null);
        result.put("ownProfile", ownProfile);
        result.remove("changed");
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> followers(Long currentUserId, Long profileId, Integer limit, Integer offset) {
        requireActor(currentUserId);
        Map<String, Object> target = requirePublicTarget(profileId);
        Long targetUserId = longValue(target.get("userId"));
        int safeLimit = safeLimit(limit);
        int safeOffset = safeOffset(offset);
        int totalCount = followMapper.countPublicFollowers(targetUserId);
        List<Map<String, Object>> items = followMapper.selectPublicFollowers(
                targetUserId, currentUserId, safeLimit, safeOffset
        );
        return listResult(items, totalCount, safeLimit, safeOffset);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> following(Long currentUserId, Long profileId, Integer limit, Integer offset) {
        requireActor(currentUserId);
        Map<String, Object> target = requirePublicTarget(profileId);
        Long targetUserId = longValue(target.get("userId"));
        int safeLimit = safeLimit(limit);
        int safeOffset = safeOffset(offset);
        int totalCount = followMapper.countPublicFollowing(targetUserId);
        List<Map<String, Object>> items = followMapper.selectPublicFollowing(
                targetUserId, currentUserId, safeLimit, safeOffset
        );
        return listResult(items, totalCount, safeLimit, safeOffset);
    }

    private Map<String, Object> requireActor(Long currentUserId) {
        if (currentUserId == null) {
            throw new SlateException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        Map<String, Object> actor = followMapper.selectActiveActorByUserId(currentUserId);
        if (actor == null) {
            throw new SlateException(HttpStatus.FORBIDDEN, "팔로우 기능을 사용할 수 없는 계정입니다.");
        }
        return actor;
    }

    private Map<String, Object> requirePublicTarget(Long profileId) {
        Map<String, Object> target = profileId == null ? null : followMapper.selectPublicTargetByProfileId(profileId);
        if (target == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "공개 프로필을 찾을 수 없습니다.");
        }
        return target;
    }

    private void rejectSelfFollow(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new SlateException(HttpStatus.BAD_REQUEST, "자기 자신은 팔로우할 수 없습니다.");
        }
    }

    private Map<String, Object> followState(
            Map<String, Object> target,
            boolean following,
            Boolean changed
    ) {
        Long targetUserId = longValue(target.get("userId"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("profileId", longValue(target.get("profileId")));
        result.put("userId", targetUserId);
        result.put("following", following);
        if (changed != null) {
            result.put("changed", changed);
        }
        result.put("followerCount", followMapper.countPublicFollowers(targetUserId));
        result.put("followingCount", followMapper.countPublicFollowing(targetUserId));
        return result;
    }

    private Map<String, Object> listResult(
            List<Map<String, Object>> items,
            int totalCount,
            int limit,
            int offset
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("totalCount", totalCount);
        result.put("limit", limit);
        result.put("offset", offset);
        result.put("hasMore", (long) offset + items.size() < totalCount);
        return result;
    }

    private Map<String, Object> auditState(Long followerUserId, Long followingUserId, boolean following) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("followerUserId", followerUserId);
        state.put("followingUserId", followingUserId);
        state.put("following", following);
        return state;
    }

    private String displayName(Map<String, Object> actor) {
        Object displayName = actor.get("displayName");
        return displayName == null ? String.valueOf(actor.get("nickname")) : String.valueOf(displayName);
    }

    private int safeLimit(Integer limit) {
        return Math.max(1, Math.min(limit == null ? DEFAULT_LIMIT : limit, MAX_LIMIT));
    }

    private int safeOffset(Integer offset) {
        return Math.max(0, offset == null ? 0 : offset);
    }

    private Long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(value));
    }
}
