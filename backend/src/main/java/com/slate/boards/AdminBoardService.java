package com.slate.boards;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.slate.admin.AdminPermissionCatalog;
import com.slate.admin.AdminPermissionService;
import com.slate.boards.AdminBoardController.AdminPostUpdateRequest;
import com.slate.common.SlateException;
import com.slate.operations.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminBoardService {

    private static final List<String> POST_CATEGORIES = List.of("WORK", "FREE");
    private static final List<String> FREE_CATEGORIES = List.of("NOTICE", "QUESTION", "INFO", "REVIEW", "FREE");
    private static final List<String> WORK_TYPES = List.of(
            "SHORT_FILM", "FEATURE_FILM", "MUSIC_VIDEO", "ADVERTISEMENT", "DOCUMENTARY", "WEB_CONTENT", "OTHER"
    );
    private static final List<String> POST_VISIBILITIES = List.of("PUBLIC", "COMPANY", "PRIVATE");
    private static final List<String> ADMIN_POST_STATUSES = List.of("PUBLISHED", "BLINDED", "ADMIN_DELETED", "AUTHOR_DELETED");
    private static final List<String> EDITABLE_POST_STATUSES = List.of("PUBLISHED", "BLINDED");

    private final BoardMapper boardMapper;
    private final AdminPermissionService adminPermissionService;
    private final AuditLogService auditLogService;

    public AdminBoardService(
            BoardMapper boardMapper,
            AdminPermissionService adminPermissionService,
            AuditLogService auditLogService
    ) {
        this.boardMapper = boardMapper;
        this.adminPermissionService = adminPermissionService;
        this.auditLogService = auditLogService;
    }

    public List<Map<String, Object>> posts(
            Long adminUserId,
            String keyword,
            String category,
            String status,
            String visibility,
            Long authorUserId,
            Integer limit
    ) {
        requireContentModeration(adminUserId);
        return boardMapper.selectAdminPosts(
                textOrNull(keyword),
                normalizeNullable(category, POST_CATEGORIES, "category"),
                normalizeNullable(status, ADMIN_POST_STATUSES, "status"),
                normalizeNullable(visibility, POST_VISIBILITIES, "visibility"),
                authorUserId,
                safeLimit(limit)
        );
    }

    public Map<String, Object> post(Long adminUserId, Long postId) {
        requireContentModeration(adminUserId);
        return requirePost(postId);
    }

    @Transactional
    public Map<String, Object> updatePost(Long adminUserId, Long postId, AdminPostUpdateRequest request) {
        requireContentModeration(adminUserId);
        String reason = requireReason(request.reason());
        Map<String, Object> before = requirePost(postId);
        assertEditable(before);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("postId", postId);
        row.put("title", normalizeText(request.title(), Objects.toString(before.get("title"), ""), "title"));
        row.put("content", normalizeBody(request, Objects.toString(before.get("content"), "")));
        String category = normalizeValue(request.category(), Objects.toString(before.get("category"), ""), POST_CATEGORIES, "category");
        row.put("category", category);
        row.put("freeCategory", "FREE".equals(category)
                ? normalizeValue(request.freeCategory(), Objects.toString(before.get("freeCategory"), "FREE"), FREE_CATEGORIES, "freeCategory")
                : null);
        row.put("visibility", normalizeValue(request.visibility(), Objects.toString(before.get("visibility"), ""), POST_VISIBILITIES, "visibility"));
        row.put("status", normalizeValue(request.status(), Objects.toString(before.get("status"), ""), EDITABLE_POST_STATUSES, "status"));
        if (boardMapper.updatePostAsAdmin(row) == 0) {
            throw new SlateException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }
        if ("WORK".equals(category) && StringUtils.hasText(request.workType())) {
            boardMapper.updateWorkTypeByPostId(
                    postId,
                    normalizeValue(request.workType(), "OTHER", WORK_TYPES, "workType")
            );
        }
        Map<String, Object> after = requirePost(postId);
        auditLogService.recordAudit(adminUserId, "BOARD_POST_ADMIN_UPDATED", "BOARD_POST", postId, auditPayload(before, null), auditPayload(after, reason));
        auditLogService.recordOperation(
                "INFO",
                "BOARD_POST_ADMIN_UPDATED",
                "관리자가 게시글을 수정했습니다.",
                Map.of("postId", postId, "adminUserId", adminUserId, "reason", reason)
        );
        return after;
    }

    @Transactional
    public Map<String, Object> hidePost(Long adminUserId, Long postId, String reason) {
        requireContentModeration(adminUserId);
        String cleanReason = requireReason(reason);
        Map<String, Object> before = requirePost(postId);
        if (before.get("deletedAt") != null) {
            throw new SlateException("삭제 상태 게시글은 숨김 처리할 수 없습니다.");
        }
        if ("BLINDED".equals(before.get("status")) && before.get("deletedAt") == null) {
            throw new SlateException("이미 숨김 처리된 게시글입니다.");
        }
        if (boardMapper.updatePostStatusAsAdmin(postId, "BLINDED") == 0) {
            throw new SlateException("게시글을 숨김 처리하지 못했습니다.");
        }
        Map<String, Object> after = requirePost(postId);
        auditLogService.recordAudit(adminUserId, "BOARD_POST_BLINDED", "BOARD_POST", postId, auditPayload(before, null), auditPayload(after, cleanReason));
        auditLogService.recordOperation(
                "WARN",
                "BOARD_POST_BLINDED",
                "관리자가 게시글을 숨김 처리했습니다.",
                Map.of("postId", postId, "adminUserId", adminUserId, "reason", cleanReason)
        );
        return after;
    }

    @Transactional
    public Map<String, Object> deletePost(Long adminUserId, Long postId, String reason) {
        requireContentModeration(adminUserId);
        String cleanReason = requireReason(reason);
        Map<String, Object> before = requirePost(postId);
        if ("ADMIN_DELETED".equals(before.get("status")) || before.get("deletedAt") != null) {
            throw new SlateException("이미 삭제 상태인 게시글입니다.");
        }
        if (boardMapper.softDeletePost(postId, "ADMIN_DELETED") == 0) {
            throw new SlateException("게시글을 삭제 상태로 전환하지 못했습니다.");
        }
        boardMapper.softDeleteWorkByPostId(postId);
        Map<String, Object> after = requirePost(postId);
        auditLogService.recordAudit(adminUserId, "BOARD_POST_ADMIN_DELETED", "BOARD_POST", postId, auditPayload(before, null), auditPayload(after, cleanReason));
        auditLogService.recordOperation(
                "WARN",
                "BOARD_POST_ADMIN_DELETED",
                "관리자가 게시글을 삭제 상태로 전환했습니다.",
                Map.of("postId", postId, "adminUserId", adminUserId, "reason", cleanReason)
        );
        return after;
    }

    @Transactional
    public Map<String, Object> restorePost(Long adminUserId, Long postId, String reason) {
        requireContentModeration(adminUserId);
        String cleanReason = requireReason(reason);
        Map<String, Object> before = requirePost(postId);
        if ("PUBLISHED".equals(before.get("status")) && before.get("deletedAt") == null) {
            throw new SlateException("이미 노출 중인 게시글입니다.");
        }
        if (boardMapper.restorePostAsAdmin(postId) == 0) {
            throw new SlateException("게시글을 복구하지 못했습니다.");
        }
        boardMapper.restoreWorkByPostId(postId);
        Map<String, Object> after = requirePost(postId);
        auditLogService.recordAudit(adminUserId, "BOARD_POST_ADMIN_RESTORED", "BOARD_POST", postId, auditPayload(before, null), auditPayload(after, cleanReason));
        auditLogService.recordOperation(
                "INFO",
                "BOARD_POST_ADMIN_RESTORED",
                "관리자가 게시글을 복구했습니다.",
                Map.of("postId", postId, "adminUserId", adminUserId, "reason", cleanReason)
        );
        return after;
    }

    private void requireContentModeration(Long adminUserId) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.CONTENT_MODERATION);
    }

    private Map<String, Object> requirePost(Long postId) {
        Map<String, Object> post = boardMapper.selectAdminPostById(postId);
        if (post == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }
        return post;
    }

    private void assertEditable(Map<String, Object> post) {
        if (post.get("deletedAt") != null
                || "ADMIN_DELETED".equals(post.get("status"))
                || "AUTHOR_DELETED".equals(post.get("status"))) {
            throw new SlateException("삭제 상태 게시글은 복구 후 수정할 수 있습니다.");
        }
    }

    private String normalizeBody(AdminPostUpdateRequest request, String fallback) {
        String value = request.body() != null ? request.body() : request.content();
        return normalizeText(value, fallback, "body");
    }

    private String normalizeText(String value, String fallback, String fieldName) {
        if (value != null && !StringUtils.hasText(value)) {
            throw new SlateException(fieldName + " 값은 비워둘 수 없습니다.");
        }
        String result = value != null ? value.trim() : fallback;
        if (!StringUtils.hasText(result)) {
            throw new SlateException(fieldName + " 값은 비워둘 수 없습니다.");
        }
        return result;
    }

    private String normalizeValue(String value, String fallback, List<String> allowed, String fieldName) {
        if (value != null && !StringUtils.hasText(value)) {
            throw new SlateException(fieldName + " 값은 비워둘 수 없습니다.");
        }
        String normalized = value != null ? value.trim().toUpperCase() : fallback;
        if (!allowed.contains(normalized)) {
            throw new SlateException(fieldName + " 값이 올바르지 않습니다.");
        }
        return normalized;
    }

    private String normalizeNullable(String value, List<String> allowed, String fieldName) {
        if (!StringUtils.hasText(value) || "ALL".equalsIgnoreCase(value.trim())) {
            return null;
        }
        return normalizeValue(value, "", allowed, fieldName);
    }

    private String requireReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new SlateException("관리자 처리 사유는 필수입니다.");
        }
        return reason.trim();
    }

    private String textOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private int safeLimit(Integer limit) {
        return Math.max(1, Math.min(limit == null ? 50 : limit, 100));
    }

    private Map<String, Object> auditPayload(Map<String, Object> row, String reason) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : List.of(
                "postId", "authorUserId", "authorNickname", "authorEmail", "category", "freeCategory", "workType", "title",
                "status", "visibility", "likeCount", "reviewCount", "viewCount", "reportCount",
                "pendingReportCount", "createdAt", "updatedAt", "deletedAt"
        )) {
            if (row.containsKey(key)) {
                result.put(key, row.get(key));
            }
        }
        if (reason != null) {
            result.put("reason", reason);
        }
        return result;
    }
}
