package com.slate.matching;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.slate.common.ApiResponse;
import com.slate.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
@RequestMapping("/api/admin/matching/policies")
@PreAuthorize("hasRole('ADMIN')")
public class ScorePolicyAdminController {

    private final ScorePolicyAdminService scorePolicyAdminService;

    public ScorePolicyAdminController(ScorePolicyAdminService scorePolicyAdminService) {
        this.scorePolicyAdminService = scorePolicyAdminService;
    }

    @GetMapping("/active")
    public ApiResponse<Map<String, Object>> activePolicy(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(scorePolicyAdminService.activePolicy(currentUser.userId()));
    }

    @PutMapping("/active")
    public ApiResponse<Map<String, Object>> publish(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody PolicyUpdateRequest request
    ) {
        return ApiResponse.ok(scorePolicyAdminService.publish(currentUser.userId(), request), "매칭 점수 정책을 발행했습니다.");
    }

    @PostMapping("/preview")
    public ApiResponse<Map<String, Object>> preview(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody PolicyPreviewRequest request
    ) {
        return ApiResponse.ok(scorePolicyAdminService.preview(currentUser.userId(), request));
    }

    @PostMapping("/{policyId}/rollback")
    public ApiResponse<Map<String, Object>> rollback(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long policyId,
            @Valid @RequestBody PolicyRollbackRequest request
    ) {
        return ApiResponse.ok(scorePolicyAdminService.rollback(currentUser.userId(), policyId, request), "매칭 점수 정책을 롤백했습니다.");
    }

    @GetMapping("/history")
    public ApiResponse<List<Map<String, Object>>> history(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(defaultValue = "20") Integer limit
    ) {
        return ApiResponse.ok(scorePolicyAdminService.history(currentUser.userId(), limit));
    }

    public record PolicyUpdateRequest(
            @NotBlank @Size(max = 100) String policyName,
            @Size(max = 255) String description,
            @NotBlank @Size(max = 500) String changeReason,
            @NotEmpty List<@Valid PolicyItemRequest> items
    ) {
    }

    public record PolicyItemRequest(
            @NotBlank String scoreGroup,
            @NotBlank String elementCode,
            @Size(max = 100) String displayName,
            @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal weight
    ) {
    }

    public record PolicyPreviewRequest(
            @NotEmpty List<@Valid PolicyItemRequest> items,
            Integer limit
    ) {
    }

    public record PolicyRollbackRequest(
            @Size(max = 100) String policyName,
            @Size(max = 500) String reason
    ) {
    }
}
