package com.slate.references;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.slate.common.ApiResponse;
import com.slate.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/regions")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRegionController {

    private final AdminRegionService adminRegionService;

    public AdminRegionController(AdminRegionService adminRegionService) {
        this.adminRegionService = adminRegionService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> regions(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sidoName,
            @RequestParam(required = false) String activeYn,
            @RequestParam(defaultValue = "300") Integer limit
    ) {
        return ApiResponse.ok(adminRegionService.regions(currentUser.userId(), keyword, sidoName, activeYn, limit));
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(adminRegionService.summary(currentUser.userId()));
    }

    @PutMapping("/{regionId}")
    public ApiResponse<Map<String, Object>> updateRegion(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long regionId,
            @Valid @RequestBody AdminRegionUpdateRequest request
    ) {
        return ApiResponse.ok(adminRegionService.updateRegion(currentUser.userId(), regionId, request), "지역 정보를 수정했습니다.");
    }

    public record AdminRegionUpdateRequest(
            @NotBlank @Size(max = 50) String sidoName,
            @NotBlank @Size(max = 80) String sigunguName,
            @Size(max = 80) String dongName,
            @NotNull BigDecimal centerLat,
            @NotNull BigDecimal centerLng,
            @NotBlank @Size(max = 150) String publicDisplayName,
            String activeYn,
            @NotBlank @Size(max = 1000) String reason
    ) {
    }
}
