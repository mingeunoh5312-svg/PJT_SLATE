package com.slate.security;

import java.util.List;
import java.util.Map;

import com.slate.common.ApiResponse;
import com.slate.security.DemoAccessCodeService.DemoAccessCodeCreateRequest;
import com.slate.security.DemoAccessCodeService.DemoAccessCodeRevokeRequest;
import com.slate.security.DemoAccessCodeService.DemoAccessCodeUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/demo-access/codes")
@PreAuthorize("hasRole('ADMIN')")
public class DemoAccessAdminController {

    private final DemoAccessCodeService demoAccessCodeService;

    public DemoAccessAdminController(DemoAccessCodeService demoAccessCodeService) {
        this.demoAccessCodeService = demoAccessCodeService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> codes(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(demoAccessCodeService.codes(currentUser.userId()));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody DemoAccessCodeCreateRequest request
    ) {
        return ApiResponse.ok(demoAccessCodeService.create(currentUser.userId(), request), "접근 코드를 생성했습니다.");
    }

    @PatchMapping("/{codeId}")
    public ApiResponse<Map<String, Object>> update(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long codeId,
            @Valid @RequestBody DemoAccessCodeUpdateRequest request
    ) {
        return ApiResponse.ok(demoAccessCodeService.update(currentUser.userId(), codeId, request), "접근 코드 설정을 저장했습니다.");
    }

    @PostMapping("/{codeId}/revoke")
    public ApiResponse<Map<String, Object>> revoke(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long codeId,
            @Valid @RequestBody DemoAccessCodeRevokeRequest request
    ) {
        return ApiResponse.ok(demoAccessCodeService.revoke(currentUser.userId(), codeId, request), "접근 코드를 폐기했습니다.");
    }
}
