package com.slate.accounts;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.slate.accounts.AdminUserController.AdminUserUpdateRequest;
import com.slate.admin.AdminPermissionCatalog;
import com.slate.admin.AdminPermissionService;
import com.slate.common.SlateException;
import com.slate.moderation.ModerationMapper;
import com.slate.operations.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminUserService {

    private static final List<String> ACCOUNT_TYPES = List.of("USER", "COMPANY", "ADMIN");
    private static final List<String> EDITABLE_ACCOUNT_TYPES = List.of("USER", "COMPANY");
    private static final List<String> ACCOUNT_STATUSES = List.of("ACTIVE", "PENDING_APPROVAL", "TEMP_SUSPENDED", "PERM_SUSPENDED", "WITHDRAWN");

    private final AccountMapper accountMapper;
    private final ModerationMapper moderationMapper;
    private final AdminPermissionService adminPermissionService;
    private final AuditLogService auditLogService;

    public AdminUserService(
            AccountMapper accountMapper,
            ModerationMapper moderationMapper,
            AdminPermissionService adminPermissionService,
            AuditLogService auditLogService
    ) {
        this.accountMapper = accountMapper;
        this.moderationMapper = moderationMapper;
        this.adminPermissionService = adminPermissionService;
        this.auditLogService = auditLogService;
    }

    public List<Map<String, Object>> users(Long adminUserId, String keyword, String accountType, String accountStatus, Integer limit) {
        requireUserSanction(adminUserId);
        return accountMapper.selectAdminUsers(
                textOrNull(keyword),
                normalizeNullable(accountType, ACCOUNT_TYPES, "accountType"),
                normalizeNullable(accountStatus, ACCOUNT_STATUSES, "accountStatus"),
                safeLimit(limit)
        );
    }

    public Map<String, Object> user(Long adminUserId, Long userId) {
        requireUserSanction(adminUserId);
        return withSanctionHistory(requireUser(userId));
    }

    @Transactional
    public Map<String, Object> updateUser(Long adminUserId, Long userId, AdminUserUpdateRequest request) {
        requireUserSanction(adminUserId);
        String reason = requireReason(request.reason());
        Map<String, Object> before = requireUser(userId);
        String accountType = normalizeAccountTypeForUpdate(request.accountType(), Objects.toString(before.get("accountType"), ""));
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("userId", userId);
        row.put("nickname", normalizeText(request.nickname(), Objects.toString(before.get("nickname"), ""), "nickname"));
        row.put("phone", normalizeNullableText(request.phone(), Objects.toString(before.get("phone"), null)));
        row.put("accountType", accountType);
        row.put("accountStatus", normalizeValue(request.accountStatus(), Objects.toString(before.get("accountStatus"), ""), ACCOUNT_STATUSES, "accountStatus"));
        row.put("deactivated", "PERM_SUSPENDED".equals(row.get("accountStatus")));
        if (accountMapper.updateAdminUser(row) == 0) {
            throw new SlateException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.");
        }
        Map<String, Object> after = requireUser(userId);
        auditLogService.recordAudit(adminUserId, "USER_ACCOUNT_ADMIN_UPDATED", "USER_ACCOUNT", userId, auditPayload(before, null), auditPayload(after, reason));
        auditLogService.recordOperation(
                "INFO",
                "USER_ACCOUNT_ADMIN_UPDATED",
                "관리자가 회원 정보를 수정했습니다.",
                Map.of("targetUserId", userId, "adminUserId", adminUserId, "reason", reason)
        );
        return withSanctionHistory(after);
    }

    @Transactional
    public Map<String, Object> deactivateUser(Long adminUserId, Long userId, String reason) {
        requireUserSanction(adminUserId);
        String cleanReason = requireReason(reason);
        if (adminUserId.equals(userId)) {
            throw new SlateException("자기 자신은 비활성화할 수 없습니다.");
        }
        Map<String, Object> before = requireUser(userId);
        if ("ADMIN".equals(before.get("accountType"))) {
            throw new SlateException("관리자 계정은 회원관리 API에서 비활성화할 수 없습니다.");
        }
        if ("PERM_SUSPENDED".equals(before.get("accountStatus")) && before.get("deactivatedAt") != null) {
            throw new SlateException("이미 비활성화된 회원입니다.");
        }
        if (accountMapper.deactivateAdminUser(userId, "PERM_SUSPENDED") == 0) {
            throw new SlateException("회원 계정을 비활성화하지 못했습니다.");
        }
        Map<String, Object> after = requireUser(userId);
        auditLogService.recordAudit(adminUserId, "USER_ACCOUNT_DEACTIVATED", "USER_ACCOUNT", userId, auditPayload(before, null), auditPayload(after, cleanReason));
        auditLogService.recordOperation(
                "WARN",
                "USER_ACCOUNT_DEACTIVATED",
                "관리자가 회원 계정을 비활성화했습니다.",
                Map.of("targetUserId", userId, "adminUserId", adminUserId, "reason", cleanReason)
        );
        return withSanctionHistory(after);
    }

    @Transactional
    public Map<String, Object> restoreUser(Long adminUserId, Long userId, String reason) {
        requireUserSanction(adminUserId);
        String cleanReason = requireReason(reason);
        Map<String, Object> before = requireUser(userId);
        if ("ADMIN".equals(before.get("accountType"))) {
            throw new SlateException("관리자 계정은 회원관리 API에서 복구할 수 없습니다.");
        }
        if ("ACTIVE".equals(before.get("accountStatus")) && before.get("deactivatedAt") == null) {
            throw new SlateException("이미 활성 상태인 회원입니다.");
        }
        if (accountMapper.restoreAdminUser(userId) == 0) {
            throw new SlateException("회원 계정을 복구하지 못했습니다.");
        }
        Map<String, Object> after = requireUser(userId);
        auditLogService.recordAudit(adminUserId, "USER_ACCOUNT_RESTORED", "USER_ACCOUNT", userId, auditPayload(before, null), auditPayload(after, cleanReason));
        auditLogService.recordOperation(
                "INFO",
                "USER_ACCOUNT_RESTORED",
                "관리자가 회원 계정을 복구했습니다.",
                Map.of("targetUserId", userId, "adminUserId", adminUserId, "reason", cleanReason)
        );
        return withSanctionHistory(after);
    }

    private void requireUserSanction(Long adminUserId) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.USER_SANCTION);
    }

    private Map<String, Object> requireUser(Long userId) {
        Map<String, Object> user = accountMapper.selectAdminUserById(userId);
        if (user == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.");
        }
        return user;
    }

    private Map<String, Object> withSanctionHistory(Map<String, Object> user) {
        Map<String, Object> result = new LinkedHashMap<>(user);
        result.put("recentSanctions", moderationMapper.selectRecentSanctionsByUserId(longValue(user.get("userId")), 5));
        return result;
    }

    private String normalizeAccountTypeForUpdate(String value, String fallback) {
        String normalized = normalizeValue(value, fallback, ACCOUNT_TYPES, "accountType");
        if ("ADMIN".equals(fallback) || "ADMIN".equals(normalized)) {
            if (!Objects.equals(fallback, normalized)) {
                throw new SlateException("관리자 계정 유형 변경은 권한 관리 기능에서 처리해주세요.");
            }
            throw new SlateException("관리자 계정은 회원관리 API에서 수정할 수 없습니다.");
        }
        if (!EDITABLE_ACCOUNT_TYPES.contains(normalized)) {
            throw new SlateException("회원관리에서 수정 가능한 계정 유형은 USER 또는 COMPANY입니다.");
        }
        return normalized;
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

    private String normalizeNullableText(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return StringUtils.hasText(trimmed) ? trimmed : null;
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

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(value));
    }

    private Map<String, Object> auditPayload(Map<String, Object> row, String reason) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : List.of(
                "userId", "loginId", "nickname", "phone", "accountType", "accountStatus",
                "lastLoginAt", "createdAt", "updatedAt", "deactivatedAt",
                "activeSanctionId", "activeSanctionType", "activeSanctionUntil"
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
