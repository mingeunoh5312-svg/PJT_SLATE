package com.slate.operations;

import java.util.List;
import java.util.Map;

import com.slate.admin.AdminPermissionCatalog;
import com.slate.admin.AdminPermissionService;
import com.slate.common.ApiResponse;
import com.slate.security.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/logs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLogController {

    private final AuditLogService auditLogService;
    private final AdminPermissionService adminPermissionService;

    public AdminLogController(AuditLogService auditLogService, AdminPermissionService adminPermissionService) {
        this.auditLogService = auditLogService;
        this.adminPermissionService = adminPermissionService;
    }

    @GetMapping("/audit")
    public ApiResponse<List<Map<String, Object>>> auditLogs(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long actorUserId,
            @RequestParam(defaultValue = "30") Integer limit
    ) {
        adminPermissionService.require(currentUser.userId(), AdminPermissionCatalog.LOG_VIEW);
        return ApiResponse.ok(auditLogService.auditLogs(actionType, targetType, actorUserId, limit));
    }

    @GetMapping("/operations")
    public ApiResponse<List<Map<String, Object>>> operationLogs(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(required = false) String logLevel,
            @RequestParam(required = false) String eventCode,
            @RequestParam(defaultValue = "30") Integer limit
    ) {
        adminPermissionService.require(currentUser.userId(), AdminPermissionCatalog.LOG_VIEW);
        return ApiResponse.ok(auditLogService.operationLogs(logLevel, eventCode, limit));
    }
}
