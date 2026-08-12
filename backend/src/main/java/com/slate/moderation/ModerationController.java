package com.slate.moderation;

import java.time.LocalDateTime;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/moderation")
@PreAuthorize("hasRole('ADMIN')")
public class ModerationController {

    private final ModerationService moderationService;

    public ModerationController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @GetMapping("/reports")
    public ApiResponse<List<Map<String, Object>>> reports(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "30") Integer limit
    ) {
        return ApiResponse.ok(moderationService.reports(currentUser.userId(), status, targetType, limit));
    }

    @PostMapping("/reports/{reportId}/decision")
    public ApiResponse<Map<String, Object>> decideReport(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long reportId,
            @Valid @RequestBody ReportDecisionRequest request
    ) {
        return ApiResponse.ok(moderationService.decideReport(currentUser.userId(), reportId, request), "신고 처리 결과를 저장했습니다.");
    }

    @GetMapping("/users")
    public ApiResponse<List<Map<String, Object>>> users(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String accountStatus,
            @RequestParam(defaultValue = "30") Integer limit
    ) {
        return ApiResponse.ok(moderationService.users(currentUser.userId(), keyword, accountStatus, limit));
    }

    @GetMapping("/sanctions")
    public ApiResponse<List<Map<String, Object>>> sanctions(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "30") Integer limit
    ) {
        return ApiResponse.ok(moderationService.sanctions(currentUser.userId(), status, limit));
    }

    @PostMapping("/users/{userId}/sanctions")
    public ApiResponse<Map<String, Object>> createSanction(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long userId,
            @Valid @RequestBody SanctionRequest request
    ) {
        return ApiResponse.ok(moderationService.createSanction(currentUser.userId(), userId, request), "사용자 제재를 적용했습니다.");
    }

    @PostMapping("/sanctions/{sanctionId}/revoke")
    public ApiResponse<Map<String, Object>> revokeSanction(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long sanctionId,
            @Valid @RequestBody RevokeSanctionRequest request
    ) {
        return ApiResponse.ok(moderationService.revokeSanction(currentUser.userId(), sanctionId, request), "사용자 제재를 해제했습니다.");
    }

    public record ReportDecisionRequest(
            @NotBlank String decision,
            String moderationAction,
            @NotBlank @Size(max = 1000) String note
    ) {
    }

    public record SanctionRequest(
            @NotBlank String sanctionType,
            @NotBlank @Size(max = 1000) String reason,
            LocalDateTime sanctionUntil
    ) {
    }

    public record RevokeSanctionRequest(
            @NotBlank @Size(max = 1000) String reason
    ) {
    }
}
