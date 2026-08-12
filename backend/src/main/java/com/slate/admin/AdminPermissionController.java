package com.slate.admin;

import java.util.List;
import java.util.Map;

import com.slate.admin.AdminPermissionService.PermissionUpdateRequest;
import com.slate.common.ApiResponse;
import com.slate.security.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/permissions")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPermissionController {

    private final AdminPermissionService adminPermissionService;

    public AdminPermissionController(AdminPermissionService adminPermissionService) {
        this.adminPermissionService = adminPermissionService;
    }

    @GetMapping("/catalog")
    public ApiResponse<List<Map<String, String>>> catalog() {
        return ApiResponse.ok(adminPermissionService.catalog());
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(adminPermissionService.me(currentUser.userId()));
    }

    @GetMapping("/users")
    public ApiResponse<List<Map<String, Object>>> adminUsers(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(adminPermissionService.adminUsers(currentUser.userId()));
    }

    @PutMapping("/users/{userId}")
    public ApiResponse<Map<String, Object>> updatePermissions(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long userId,
            @RequestBody PermissionUpdateRequest request
    ) {
        return ApiResponse.ok(adminPermissionService.updatePermissions(currentUser.userId(), userId, request), "관리자 권한을 저장했습니다.");
    }
}
