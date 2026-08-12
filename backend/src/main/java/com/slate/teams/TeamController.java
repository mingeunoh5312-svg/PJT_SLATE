package com.slate.teams;

import java.util.List;
import java.util.Map;

import com.slate.common.ApiResponse;
import com.slate.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/mine")
    public ApiResponse<List<Map<String, Object>>> myTeams(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(teamService.myTeams(currentUser.userId()));
    }

    @GetMapping("/{teamId}")
    public ApiResponse<Map<String, Object>> team(@PathVariable Long teamId) {
        return ApiResponse.ok(teamService.team(teamId));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> createTeam(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody TeamRequest request
    ) {
        return ApiResponse.ok(teamService.createTeam(currentUser.userId(), request), "팀을 생성했습니다.");
    }

    @PutMapping("/{teamId}")
    public ApiResponse<Map<String, Object>> updateTeam(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long teamId,
            @Valid @RequestBody TeamRequest request
    ) {
        return ApiResponse.ok(teamService.updateTeam(currentUser.userId(), teamId, request), "팀을 수정했습니다.");
    }

    @DeleteMapping("/{teamId}")
    public ApiResponse<Map<String, Object>> deleteTeam(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long teamId
    ) {
        return ApiResponse.ok(teamService.deleteTeam(currentUser.userId(), teamId), "팀을 삭제했습니다.");
    }

    @GetMapping("/{teamId}/recruitments")
    public ApiResponse<List<Map<String, Object>>> recruitments(@PathVariable Long teamId) {
        return ApiResponse.ok(teamService.recruitments(teamId));
    }

    @PostMapping("/{teamId}/recruitments")
    public ApiResponse<Map<String, Object>> createRecruitment(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long teamId,
            @Valid @RequestBody RecruitmentRequest request
    ) {
        return ApiResponse.ok(teamService.createRecruitment(currentUser.userId(), teamId, request), "모집 공고를 생성했습니다.");
    }

    @PutMapping("/recruitments/{recruitmentId}")
    public ApiResponse<Map<String, Object>> updateRecruitment(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long recruitmentId,
            @Valid @RequestBody RecruitmentRequest request
    ) {
        return ApiResponse.ok(teamService.updateRecruitment(currentUser.userId(), recruitmentId, request), "모집 공고를 수정했습니다.");
    }

    @DeleteMapping("/recruitments/{recruitmentId}")
    public ApiResponse<Map<String, Object>> deleteRecruitment(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long recruitmentId
    ) {
        return ApiResponse.ok(teamService.deleteRecruitment(currentUser.userId(), recruitmentId), "모집 공고를 삭제했습니다.");
    }

    @PostMapping("/recruitments/{recruitmentId}/slots")
    public ApiResponse<Map<String, Object>> createSlot(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long recruitmentId,
            @Valid @RequestBody SlotRequest request
    ) {
        return ApiResponse.ok(teamService.createSlot(currentUser.userId(), recruitmentId, request), "모집 슬롯을 생성했습니다.");
    }

    @PutMapping("/recruitment-slots/{slotId}")
    public ApiResponse<Map<String, Object>> updateSlot(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long slotId,
            @Valid @RequestBody SlotRequest request
    ) {
        return ApiResponse.ok(teamService.updateSlot(currentUser.userId(), slotId, request), "모집 슬롯을 수정했습니다.");
    }

    @DeleteMapping("/recruitment-slots/{slotId}")
    public ApiResponse<Map<String, Object>> deleteSlot(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long slotId
    ) {
        return ApiResponse.ok(teamService.deleteSlot(currentUser.userId(), slotId), "모집 슬롯을 삭제했습니다.");
    }

    @GetMapping("/{teamId}/applications")
    public ApiResponse<List<Map<String, Object>>> applications(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long teamId
    ) {
        return ApiResponse.ok(teamService.applications(currentUser.userId(), teamId));
    }

    @PostMapping("/applications/{applicationId}/decision")
    public ApiResponse<Map<String, Object>> decideApplication(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long applicationId,
            @Valid @RequestBody RequestDecision request
    ) {
        return ApiResponse.ok(teamService.decideApplication(currentUser.userId(), applicationId, request), "지원 처리 결과를 저장했습니다.");
    }

    @GetMapping("/{teamId}/invitations")
    public ApiResponse<List<Map<String, Object>>> teamInvitations(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long teamId
    ) {
        return ApiResponse.ok(teamService.teamInvitations(currentUser.userId(), teamId));
    }

    @GetMapping("/invitations/mine")
    public ApiResponse<List<Map<String, Object>>> myInvitations(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(teamService.myInvitations(currentUser.userId()));
    }

    @PostMapping("/invitations/{invitationId}/decision")
    public ApiResponse<Map<String, Object>> decideInvitation(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long invitationId,
            @Valid @RequestBody InvitationDecision request
    ) {
        return ApiResponse.ok(teamService.decideInvitation(currentUser.userId(), invitationId, request), "초대 응답을 저장했습니다.");
    }

    @PutMapping("/{teamId}/members/{memberUserId}")
    public ApiResponse<Map<String, Object>> updateMember(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long teamId,
            @PathVariable Long memberUserId,
            @Valid @RequestBody MemberUpdateRequest request
    ) {
        return ApiResponse.ok(teamService.updateMember(currentUser.userId(), teamId, memberUserId, request), "팀원 정보를 저장했습니다.");
    }

    @PostMapping("/{teamId}/leave")
    public ApiResponse<Map<String, Object>> leaveTeam(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long teamId
    ) {
        return ApiResponse.ok(teamService.leaveTeam(currentUser.userId(), teamId), "팀에서 나갔습니다.");
    }

    @PostMapping("/{teamId}/transfer-leader")
    public ApiResponse<Map<String, Object>> transferLeader(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long teamId,
            @Valid @RequestBody TeamLeaderTransferRequest request
    ) {
        return ApiResponse.ok(teamService.transferLeader(currentUser.userId(), teamId, request), "팀장 권한을 이전했습니다.");
    }

    @PostMapping("/{teamId}/close")
    public ApiResponse<Map<String, Object>> closeTeam(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long teamId,
            @Valid @RequestBody TeamCloseRequest request
    ) {
        return ApiResponse.ok(teamService.closeTeam(currentUser.userId(), teamId, request), "팀 종료 스냅샷을 저장했습니다.");
    }

    @GetMapping("/{teamId}/closure-snapshots")
    public ApiResponse<List<Map<String, Object>>> closureSnapshots(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long teamId
    ) {
        return ApiResponse.ok(teamService.closureSnapshots(currentUser.userId(), teamId));
    }

    @PostMapping("/{teamId}/reopen")
    public ApiResponse<Map<String, Object>> reopenTeam(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long teamId,
            @Valid @RequestBody TeamReopenRequest request
    ) {
        return ApiResponse.ok(teamService.reopenTeam(currentUser.userId(), teamId, request), "팀을 재개했습니다.");
    }

    @GetMapping("/{teamId}/plans")
    public ApiResponse<List<Map<String, Object>>> planItems(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long teamId
    ) {
        return ApiResponse.ok(teamService.planItems(currentUser.userId(), teamId));
    }

    @PostMapping("/{teamId}/plans")
    public ApiResponse<Map<String, Object>> createPlanItem(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long teamId,
            @Valid @RequestBody PlanItemRequest request
    ) {
        return ApiResponse.ok(teamService.createPlanItem(currentUser.userId(), teamId, request), "팀 계획을 생성했습니다.");
    }

    @PutMapping("/plans/{planItemId}")
    public ApiResponse<Map<String, Object>> updatePlanItem(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long planItemId,
            @Valid @RequestBody PlanItemRequest request
    ) {
        return ApiResponse.ok(teamService.updatePlanItem(currentUser.userId(), planItemId, request), "팀 계획을 수정했습니다.");
    }

    @PatchMapping("/plans/{planItemId}/status")
    public ApiResponse<Map<String, Object>> updatePlanStatus(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long planItemId,
            @Valid @RequestBody PlanStatusRequest request
    ) {
        return ApiResponse.ok(teamService.updatePlanStatus(currentUser.userId(), planItemId, request), "팀 계획 상태를 저장했습니다.");
    }

    public record TeamRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 2000) String description,
            @NotEmpty List<Long> genreIds,
            Long regionId,
            String regionAnyYn,
            @NotBlank String expectedDuration,
            @Min(1) Integer maxMemberCount,
            String status
    ) {
    }

    public record RecruitmentRequest(
            @NotBlank @Size(max = 120) String title,
            String status,
            String deadlineAt,
            String workStartAt
    ) {
    }

    public record SlotRequest(
            @NotNull Long roleId,
            @Min(1) Integer requiredCount,
            @NotBlank String requiredExperienceLevel,
            @NotBlank String collaborationCondition,
            String requiredYn,
            String roleDuration,
            String equipmentRequiredYn,
            String status
    ) {
    }

    public record RequestDecision(
            @NotBlank String decision,
            @Size(max = 300) String reason
    ) {
    }

    public record InvitationDecision(
            @NotBlank String decision
    ) {
    }

    public record MemberUpdateRequest(
            @NotBlank String teamRole,
            String status
    ) {
    }

    public record PlanItemRequest(
            @NotBlank @Size(max = 150) String title,
            @Size(max = 1000) String description,
            Long assigneeUserId,
            Long roleId,
            String dueAt,
            String status
    ) {
    }

    public record PlanStatusRequest(
            @NotBlank String status
    ) {
    }

    public record TeamCloseRequest(
            @NotBlank String endType,
            @NotBlank @Size(max = 1000) String reason
    ) {
    }

    public record TeamLeaderTransferRequest(
            @NotNull Long newLeaderUserId,
            @Size(max = 500) String reason
    ) {
    }

    public record TeamReopenRequest(
            Long closureSnapshotId,
            String restoreSnapshotYn,
            @Size(max = 1000) String reason
    ) {
    }
}
