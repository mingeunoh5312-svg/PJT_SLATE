package com.slate.accounts;

import java.util.List;
import java.util.Map;

import com.slate.common.ApiResponse;
import com.slate.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> users(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) String accountStatus,
            @RequestParam(defaultValue = "50") Integer limit
    ) {
        return ApiResponse.ok(adminUserService.users(currentUser.userId(), keyword, accountType, accountStatus, limit));
    }

    @GetMapping("/{userId}")
    public ApiResponse<Map<String, Object>> user(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long userId
    ) {
        return ApiResponse.ok(adminUserService.user(currentUser.userId(), userId));
    }

    @PutMapping("/{userId}")
    public ApiResponse<Map<String, Object>> updateUser(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserUpdateRequest request
    ) {
        return ApiResponse.ok(adminUserService.updateUser(currentUser.userId(), userId, request), "회원 정보를 수정했습니다.");
    }

    @PostMapping("/{userId}/deactivate")
    public ApiResponse<Map<String, Object>> deactivateUser(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long userId,
            @Valid @RequestBody ReasonRequest request
    ) {
        return ApiResponse.ok(adminUserService.deactivateUser(currentUser.userId(), userId, request.reason()), "회원 계정을 비활성화했습니다.");
    }

    @PostMapping("/{userId}/restore")
    public ApiResponse<Map<String, Object>> restoreUser(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long userId,
            @Valid @RequestBody ReasonRequest request
    ) {
        return ApiResponse.ok(adminUserService.restoreUser(currentUser.userId(), userId, request.reason()), "회원 계정을 복구했습니다.");
    }

    public record AdminUserUpdateRequest(
            @Size(max = 50) String nickname,
            @Size(max = 30) String phone,
            String accountType,
            String accountStatus,
            @NotBlank @Size(max = 1000) String reason
    ) {
    }

    public record ReasonRequest(@NotBlank @Size(max = 1000) String reason) {
    }
}
