package com.slate.admin;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.slate.common.SlateException;
import com.slate.operations.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminPermissionService {

    private final AdminPermissionMapper adminPermissionMapper;
    private final AuditLogService auditLogService;

    public AdminPermissionService(AdminPermissionMapper adminPermissionMapper, AuditLogService auditLogService) {
        this.adminPermissionMapper = adminPermissionMapper;
        this.auditLogService = auditLogService;
    }

    public List<Map<String, String>> catalog() {
        return AdminPermissionCatalog.ITEMS;
    }

    public Map<String, Object> me(Long adminUserId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("permissions", permissions(adminUserId));
        result.put("catalog", catalog());
        return result;
    }

    public List<String> permissions(Long userId) {
        return adminPermissionMapper.selectActivePermissionCodes(userId);
    }

    public void require(Long userId, String permissionCode) {
        if (userId == null || !AdminPermissionCatalog.CODES.contains(permissionCode)
                || adminPermissionMapper.countActivePermission(userId, permissionCode) == 0) {
            throw new SlateException(HttpStatus.FORBIDDEN, "관리자 세부 권한이 없습니다: " + permissionCode);
        }
    }

    public List<Map<String, Object>> adminUsers(Long requesterUserId) {
        require(requesterUserId, AdminPermissionCatalog.ADMIN_PERMISSION_MANAGE);
        return adminPermissionMapper.selectAdminUsers().stream().map(this::withPermissions).toList();
    }

    @Transactional
    public Map<String, Object> updatePermissions(Long actorUserId, Long targetUserId, PermissionUpdateRequest request) {
        require(actorUserId, AdminPermissionCatalog.ADMIN_PERMISSION_MANAGE);
        Map<String, Object> target = adminPermissionMapper.selectAdminUserById(targetUserId);
        if (target == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "관리자 계정을 찾을 수 없습니다.");
        }
        List<String> before = permissions(targetUserId);
        List<String> nextPermissions = sanitize(request.permissions());
        adminPermissionMapper.deactivatePermissions(targetUserId);
        for (String permissionCode : nextPermissions) {
            adminPermissionMapper.upsertPermission(targetUserId, permissionCode, "Y", actorUserId);
        }
        Map<String, Object> after = withPermissions(adminPermissionMapper.selectAdminUserById(targetUserId));
        auditLogService.recordAudit(
                actorUserId,
                "ADMIN_PERMISSION_UPDATED",
                "USER",
                targetUserId,
                Map.of("permissions", before),
                Map.of("permissions", nextPermissions)
        );
        auditLogService.recordOperation(
                "INFO",
                "ADMIN_PERMISSION_UPDATED",
                "관리자 세부 권한이 수정되었습니다.",
                Map.of("targetUserId", targetUserId, "permissions", nextPermissions)
        );
        return after;
    }

    private Map<String, Object> withPermissions(Map<String, Object> row) {
        if (row == null) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>(row);
        result.put("permissions", permissions(((Number) row.get("userId")).longValue()));
        return result;
    }

    private List<String> sanitize(List<String> permissions) {
        Set<String> clean = new LinkedHashSet<>();
        if (permissions != null) {
            permissions.stream()
                    .filter(AdminPermissionCatalog.CODES::contains)
                    .forEach(clean::add);
        }
        return clean.stream().toList();
    }

    public record PermissionUpdateRequest(List<String> permissions) {
    }
}
