package com.slate.moderation;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.slate.accounts.AccountMapper;
import com.slate.admin.AdminPermissionCatalog;
import com.slate.admin.AdminPermissionService;
import com.slate.boards.BoardController.ReportRequest;
import com.slate.boards.BoardMapper;
import com.slate.common.SlateException;
import com.slate.moderation.ModerationController.ReportDecisionRequest;
import com.slate.moderation.ModerationController.RevokeSanctionRequest;
import com.slate.moderation.ModerationController.SanctionRequest;
import com.slate.notifications.NotificationService;
import com.slate.operations.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ModerationService {

    private static final List<String> REPORT_REASONS = List.of("SPAM", "ABUSE", "ILLEGAL", "PRIVACY", "OTHER");
    private static final List<String> REPORT_TARGET_TYPES = List.of("BOARD_POST", "BOARD_REVIEW");
    private static final List<String> REPORT_STATUSES = List.of("PENDING", "ACCEPTED", "REJECTED");
    private static final List<String> MODERATION_ACTIONS = List.of("NONE", "BLIND_POST", "DELETE_POST", "BLIND_REVIEW", "DELETE_REVIEW");
    private static final List<String> SANCTION_TYPES = List.of("TEMP_SUSPENDED", "PERM_SUSPENDED");
    private static final List<String> SANCTION_STATUSES = List.of("ACTIVE", "REVOKED", "EXPIRED");

    private final ModerationMapper moderationMapper;
    private final BoardMapper boardMapper;
    private final AccountMapper accountMapper;
    private final AdminPermissionService adminPermissionService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public ModerationService(
            ModerationMapper moderationMapper,
            BoardMapper boardMapper,
            AccountMapper accountMapper,
            AdminPermissionService adminPermissionService,
            NotificationService notificationService,
            AuditLogService auditLogService
    ) {
        this.moderationMapper = moderationMapper;
        this.boardMapper = boardMapper;
        this.accountMapper = accountMapper;
        this.adminPermissionService = adminPermissionService;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Map<String, Object> reportPost(Long reporterUserId, Long postId, ReportRequest request) {
        Map<String, Object> post = boardMapper.selectPostForModeration(postId);
        if (post == null || !"PUBLISHED".equals(post.get("status"))) {
            throw new SlateException(HttpStatus.NOT_FOUND, "신고할 게시글을 찾을 수 없습니다.");
        }
        assertNotOwnContent(reporterUserId, post);
        return createReport(reporterUserId, "BOARD_POST", postId, normalizeReason(request.reasonCode()), request.detail(), post);
    }

    @Transactional
    public Map<String, Object> reportReview(Long reporterUserId, Long reviewId, ReportRequest request) {
        Map<String, Object> review = boardMapper.selectReviewForModeration(reviewId);
        if (review == null || !"PUBLISHED".equals(review.get("status"))) {
            throw new SlateException(HttpStatus.NOT_FOUND, "신고할 리뷰를 찾을 수 없습니다.");
        }
        assertNotOwnContent(reporterUserId, review);
        return createReport(reporterUserId, "BOARD_REVIEW", reviewId, normalizeReason(request.reasonCode()), request.detail(), review);
    }

    public List<Map<String, Object>> reports(Long adminUserId, String status, String targetType, Integer limit) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.CONTENT_MODERATION);
        return moderationMapper.selectReports(normalizeNullable(status, REPORT_STATUSES), normalizeNullable(targetType, REPORT_TARGET_TYPES), safeLimit(limit));
    }

    @Transactional
    public Map<String, Object> decideReport(Long adminUserId, Long reportId, ReportDecisionRequest request) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.CONTENT_MODERATION);
        Map<String, Object> before = requireReport(reportId);
        if (!"PENDING".equals(before.get("status"))) {
            throw new SlateException("이미 처리된 신고입니다.");
        }
        String decision = normalizeRequired(request.decision(), List.of("ACCEPTED", "REJECTED"), "decision");
        String action = "REJECTED".equals(decision) ? "NONE" : normalizeAction(request.moderationAction());
        validateActionMatchesTarget(action, Objects.toString(before.get("targetType"), ""));
        applyModerationAction(adminUserId, before, action);
        if (moderationMapper.updateReportDecision(reportId, decision, action, request.note().trim(), adminUserId) == 0) {
            throw new SlateException("신고 처리 결과를 저장하지 못했습니다.");
        }
        Map<String, Object> after = requireReport(reportId);
        auditLogService.recordAudit(adminUserId, "CONTENT_REPORT_" + decision, "CONTENT_REPORT", reportId, before, after);
        auditLogService.recordOperation(
                "WARN",
                "CONTENT_REPORT_DECIDED",
                "콘텐츠 신고 처리 결과가 저장되었습니다.",
                Map.of("reportId", reportId, "decision", decision, "moderationAction", action)
        );
        return after;
    }

    public List<Map<String, Object>> users(Long adminUserId, String keyword, String accountStatus, Integer limit) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.USER_SANCTION);
        return moderationMapper.selectUsers(textOrNull(keyword), normalizeNullable(accountStatus, List.of("ACTIVE", "PENDING_APPROVAL", "TEMP_SUSPENDED", "PERM_SUSPENDED", "WITHDRAWN")), safeLimit(limit));
    }

    public List<Map<String, Object>> sanctions(Long adminUserId, String status, Integer limit) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.USER_SANCTION);
        return moderationMapper.selectSanctions(normalizeNullable(status, SANCTION_STATUSES), safeLimit(limit));
    }

    @Transactional
    public Map<String, Object> createSanction(Long adminUserId, Long targetUserId, SanctionRequest request) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.USER_SANCTION);
        refreshExpiredSanctions(targetUserId);
        Map<String, Object> account = accountMapper.selectAccountById(targetUserId);
        if (account == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "제재할 사용자를 찾을 수 없습니다.");
        }
        if (adminUserId.equals(targetUserId)) {
            throw new SlateException("자기 자신은 제재할 수 없습니다.");
        }
        if ("ADMIN".equals(account.get("accountType"))) {
            throw new SlateException("관리자 계정은 이 화면에서 제재할 수 없습니다.");
        }
        if (moderationMapper.selectActiveSanctionByUserId(targetUserId) != null) {
            throw new SlateException("이미 활성 제재가 있는 사용자입니다.");
        }
        String sanctionType = normalizeRequired(request.sanctionType(), SANCTION_TYPES, "sanctionType");
        if ("TEMP_SUSPENDED".equals(sanctionType) && (request.sanctionUntil() == null || !request.sanctionUntil().isAfter(LocalDateTime.now()))) {
            throw new SlateException("임시 정지는 현재 이후의 종료 시간이 필요합니다.");
        }
        Map<String, Object> sanction = new LinkedHashMap<>();
        sanction.put("userId", targetUserId);
        sanction.put("sanctionType", sanctionType);
        sanction.put("reason", request.reason().trim());
        sanction.put("sanctionUntil", "PERM_SUSPENDED".equals(sanctionType) ? null : request.sanctionUntil());
        sanction.put("createdBy", adminUserId);
        moderationMapper.insertSanction(sanction);
        accountMapper.updateAccountStatus(targetUserId, sanctionType);
        notificationService.send(
                targetUserId,
                adminUserId,
                "ADMIN",
                "계정 이용이 제한되었습니다.",
                request.reason().trim(),
                "USER_SANCTION",
                ((Number) sanction.get("sanctionId")).longValue()
        );
        Map<String, Object> created = moderationMapper.selectSanctionById(((Number) sanction.get("sanctionId")).longValue());
        auditLogService.recordAudit(adminUserId, "USER_SANCTION_CREATED", "USER_SANCTION", ((Number) sanction.get("sanctionId")).longValue(), account, created);
        auditLogService.recordOperation(
                "WARN",
                "USER_SANCTIONED",
                "관리자가 사용자 제재를 적용했습니다.",
                Map.of("targetUserId", targetUserId, "sanctionType", sanctionType, "adminUserId", adminUserId)
        );
        return created;
    }

    @Transactional
    public Map<String, Object> revokeSanction(Long adminUserId, Long sanctionId, RevokeSanctionRequest request) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.USER_SANCTION);
        Map<String, Object> before = requireSanction(sanctionId);
        if (!"ACTIVE".equals(before.get("status"))) {
            throw new SlateException("활성 제재만 해제할 수 있습니다.");
        }
        if (moderationMapper.revokeSanction(sanctionId, adminUserId, request.reason().trim()) == 0) {
            throw new SlateException("제재 해제 결과를 저장하지 못했습니다.");
        }
        Long targetUserId = ((Number) before.get("userId")).longValue();
        if (moderationMapper.countActiveSanctionsByUserId(targetUserId) == 0) {
            accountMapper.updateAccountStatus(targetUserId, "ACTIVE");
        }
        notificationService.send(
                targetUserId,
                adminUserId,
                "ADMIN",
                "계정 이용 제한이 해제되었습니다.",
                request.reason().trim(),
                "USER_SANCTION",
                sanctionId
        );
        Map<String, Object> after = requireSanction(sanctionId);
        auditLogService.recordAudit(adminUserId, "USER_SANCTION_REVOKED", "USER_SANCTION", sanctionId, before, after);
        auditLogService.recordOperation(
                "INFO",
                "USER_SANCTION_REVOKED",
                "관리자가 사용자 제재를 해제했습니다.",
                Map.of("targetUserId", targetUserId, "sanctionId", sanctionId, "adminUserId", adminUserId)
        );
        return after;
    }

    @Transactional
    public void refreshExpiredSanctions(Long userId) {
        if (userId == null) {
            return;
        }
        int expired = moderationMapper.expireSanctionsByUserId(userId);
        if (expired > 0 && moderationMapper.countActiveSanctionsByUserId(userId) == 0) {
            Map<String, Object> account = accountMapper.selectAccountById(userId);
            if (account != null && "TEMP_SUSPENDED".equals(account.get("accountStatus"))) {
                accountMapper.updateAccountStatus(userId, "ACTIVE");
            }
        }
    }

    @Transactional
    public boolean isAuthenticationAllowed(Long userId) {
        refreshExpiredSanctions(userId);
        Map<String, Object> account = accountMapper.selectAccountById(userId);
        return account != null && "ACTIVE".equals(account.get("accountStatus"));
    }

    private Map<String, Object> createReport(Long reporterUserId, String targetType, Long targetId, String reasonCode, String detail, Map<String, Object> target) {
        if (moderationMapper.countPendingReport(reporterUserId, targetType, targetId) > 0) {
            throw new SlateException("이미 검토 중인 신고가 있습니다.");
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reporterUserId", reporterUserId);
        report.put("targetType", targetType);
        report.put("targetId", targetId);
        report.put("reasonCode", reasonCode);
        report.put("detail", textOrNull(detail));
        moderationMapper.insertReport(report);
        Map<String, Object> created = requireReport(((Number) report.get("reportId")).longValue());
        auditLogService.recordAudit(reporterUserId, "CONTENT_REPORT_CREATED", "CONTENT_REPORT", ((Number) report.get("reportId")).longValue(), target, created);
        return created;
    }

    private void applyModerationAction(Long adminUserId, Map<String, Object> report, String action) {
        if ("NONE".equals(action)) {
            return;
        }
        Long targetId = ((Number) report.get("targetId")).longValue();
        if ("BLIND_POST".equals(action)) {
            Map<String, Object> before = boardMapper.selectPostForModeration(targetId);
            boardMapper.blindPost(targetId);
            Map<String, Object> after = boardMapper.selectPostForModeration(targetId);
            auditLogService.recordAudit(adminUserId, "BOARD_POST_BLINDED", "BOARD_POST", targetId, before, after);
            return;
        }
        if ("DELETE_POST".equals(action)) {
            Map<String, Object> before = boardMapper.selectPostForModeration(targetId);
            boardMapper.softDeletePost(targetId, "ADMIN_DELETED");
            boardMapper.softDeleteWorkByPostId(targetId);
            Map<String, Object> after = boardMapper.selectPostForModeration(targetId);
            auditLogService.recordAudit(adminUserId, "BOARD_POST_ADMIN_DELETED", "BOARD_POST", targetId, before, after);
            return;
        }
        if ("BLIND_REVIEW".equals(action)) {
            Map<String, Object> before = boardMapper.selectReviewForModeration(targetId);
            boardMapper.blindReview(targetId, "운영 정책에 따라 숨김 처리된 리뷰입니다.");
            boardMapper.recountReviewCount(((Number) before.get("postId")).longValue());
            Map<String, Object> after = boardMapper.selectReviewForModeration(targetId);
            auditLogService.recordAudit(adminUserId, "BOARD_REVIEW_BLINDED", "BOARD_REVIEW", targetId, before, after);
            return;
        }
        if ("DELETE_REVIEW".equals(action)) {
            Map<String, Object> before = boardMapper.selectReviewForModeration(targetId);
            boardMapper.softDeleteReview(targetId, "ADMIN_DELETED", "관리자에 의해 삭제된 댓글입니다.");
            boardMapper.recountReviewCount(((Number) before.get("postId")).longValue());
            Map<String, Object> after = boardMapper.selectReviewForModeration(targetId);
            auditLogService.recordAudit(adminUserId, "BOARD_REVIEW_ADMIN_DELETED", "BOARD_REVIEW", targetId, before, after);
        }
    }

    private void assertNotOwnContent(Long userId, Map<String, Object> target) {
        Long authorUserId = ((Number) target.get("authorUserId")).longValue();
        if (authorUserId.equals(userId)) {
            throw new SlateException("자신의 콘텐츠는 신고할 수 없습니다.");
        }
    }

    private Map<String, Object> requireReport(Long reportId) {
        Map<String, Object> report = moderationMapper.selectReportById(reportId);
        if (report == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "신고를 찾을 수 없습니다.");
        }
        return report;
    }

    private Map<String, Object> requireSanction(Long sanctionId) {
        Map<String, Object> sanction = moderationMapper.selectSanctionById(sanctionId);
        if (sanction == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "사용자 제재를 찾을 수 없습니다.");
        }
        return sanction;
    }

    private String normalizeReason(String reasonCode) {
        return normalizeRequired(reasonCode, REPORT_REASONS, "reasonCode");
    }

    private String normalizeAction(String action) {
        return normalizeRequired(StringUtils.hasText(action) ? action : "NONE", MODERATION_ACTIONS, "moderationAction");
    }

    private void validateActionMatchesTarget(String action, String targetType) {
        if ("NONE".equals(action)) {
            return;
        }
        if ("BOARD_POST".equals(targetType) && List.of("BLIND_POST", "DELETE_POST").contains(action)) {
            return;
        }
        if ("BOARD_REVIEW".equals(targetType) && List.of("BLIND_REVIEW", "DELETE_REVIEW").contains(action)) {
            return;
        }
        throw new SlateException("신고 대상과 처리 조치가 맞지 않습니다.");
    }

    private String normalizeNullable(String value, List<String> allowed) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return normalizeRequired(value, allowed, "filter");
    }

    private String normalizeRequired(String value, List<String> allowed, String fieldName) {
        String normalized = Objects.toString(value, "").trim().toUpperCase();
        if (!allowed.contains(normalized)) {
            throw new SlateException(fieldName + " 값이 올바르지 않습니다.");
        }
        return normalized;
    }

    private int safeLimit(Integer limit) {
        return Math.max(1, Math.min(limit == null ? 30 : limit, 100));
    }

    private String textOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
