package com.slate.accounts;

import java.util.List;
import java.util.Map;

import com.slate.common.ApiResponse;
import com.slate.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/company-applications")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    public AdminAccountController(AdminAccountService adminAccountService) {
        this.adminAccountService = adminAccountService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> applications(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(adminAccountService.companyApplications(currentUser.userId()));
    }

    @PostMapping("/{applicationId}/decision")
    public ApiResponse<Map<String, Object>> decide(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long applicationId,
            @Valid @RequestBody DecisionRequest request
    ) {
        return ApiResponse.ok(adminAccountService.decide(currentUser.userId(), applicationId, request), "회사 계정 검토 결과를 저장했습니다.");
    }

    public record DecisionRequest(
            @NotBlank String decision,
            @NotBlank String reason
    ) {
    }
}
