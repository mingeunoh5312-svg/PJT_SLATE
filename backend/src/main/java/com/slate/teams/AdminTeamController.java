package com.slate.teams;

import java.util.List;
import java.util.Map;

import com.slate.common.ApiResponse;
import com.slate.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/teams")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTeamController {

    private final AdminTeamService adminTeamService;

    public AdminTeamController(AdminTeamService adminTeamService) {
        this.adminTeamService = adminTeamService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> teams(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Long leaderUserId,
            @RequestParam(defaultValue = "50") Integer limit
    ) {
        return ApiResponse.ok(adminTeamService.teams(
                currentUser.userId(),
                keyword,
                status,
                regionId,
                leaderUserId,
                limit
        ));
    }

    @GetMapping("/{teamId}")
    public ApiResponse<Map<String, Object>> team(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long teamId
    ) {
        return ApiResponse.ok(adminTeamService.team(currentUser.userId(), teamId));
    }

    @PutMapping("/{teamId}")
    public ApiResponse<Map<String, Object>> updateTeam(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long teamId,
            @Valid @RequestBody AdminTeamUpdateRequest request
    ) {
        return ApiResponse.ok(adminTeamService.updateTeam(currentUser.userId(), teamId, request), "팀 정보를 수정했습니다.");
    }

    @PostMapping("/{teamId}/hide")
    public ApiResponse<Map<String, Object>> hideTeam(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long teamId,
            @Valid @RequestBody ReasonRequest request
    ) {
        return ApiResponse.ok(adminTeamService.hideTeam(currentUser.userId(), teamId, request.reason()), "팀을 숨김 처리했습니다.");
    }

    @PostMapping("/{teamId}/close")
    public ApiResponse<Map<String, Object>> closeTeam(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long teamId,
            @Valid @RequestBody AdminTeamCloseRequest request
    ) {
        return ApiResponse.ok(adminTeamService.closeTeam(currentUser.userId(), teamId, request), "팀을 종료했습니다.");
    }

    @PostMapping("/{teamId}/restore")
    public ApiResponse<Map<String, Object>> restoreTeam(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long teamId,
            @Valid @RequestBody AdminTeamRestoreRequest request
    ) {
        return ApiResponse.ok(adminTeamService.restoreTeam(currentUser.userId(), teamId, request), "팀을 복구했습니다.");
    }

    @DeleteMapping("/{teamId}")
    public ApiResponse<Map<String, Object>> deleteTeam(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long teamId,
            @Valid @RequestBody ReasonRequest request
    ) {
        return ApiResponse.ok(adminTeamService.deleteTeam(currentUser.userId(), teamId, request.reason()), "팀을 삭제 상태로 전환했습니다.");
    }

    public record AdminTeamUpdateRequest(
            @Size(max = 100) String name,
            @Size(max = 2000) String description,
            String status,
            Long regionId,
            String regionAnyYn,
            String expectedDuration,
            @Min(1) Integer maxMemberCount,
            @NotBlank @Size(max = 1000) String reason
    ) {
    }

    public record AdminTeamCloseRequest(
            String endType,
            @NotBlank @Size(max = 1000) String reason
    ) {
    }

    public record AdminTeamRestoreRequest(
            Long closureSnapshotId,
            String restoreSnapshotYn,
            String status,
            @NotBlank @Size(max = 1000) String reason
    ) {
    }

    public record ReasonRequest(@NotBlank @Size(max = 1000) String reason) {
    }
}
