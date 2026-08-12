package com.slate.matching;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.slate.common.ApiResponse;
import com.slate.common.SlateException;
import com.slate.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matching")
public class MatchingController {

    private final MatchingService matchingService;
    private final AiMatchingRecommendationService aiMatchingRecommendationService;

    public MatchingController(MatchingService matchingService, AiMatchingRecommendationService aiMatchingRecommendationService) {
        this.matchingService = matchingService;
        this.aiMatchingRecommendationService = aiMatchingRecommendationService;
    }

    @GetMapping("/team-to-members")
    public ApiResponse<Map<String, Object>> teamToMembers(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ApiResponse.ok(matchingService.teamToMembers(currentUser.userId(), queryMap(params)));
    }

    @GetMapping("/member-to-teams")
    public ApiResponse<Map<String, Object>> memberToTeams(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ApiResponse.ok(matchingService.memberToTeams(currentUser.userId(), queryMap(params)));
    }

    @GetMapping("/policies/active")
    public ApiResponse<Map<String, Object>> activePolicy() {
        return ApiResponse.ok(matchingService.activePolicy());
    }

    @PostMapping("/ai/recommendations")
    public ApiResponse<AiRecommendationResponse> aiRecommendations(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody AiRecommendationRequest request
    ) {
        if (currentUser == null) {
            throw new SlateException("로그인이 필요합니다.");
        }
        return ApiResponse.ok(aiMatchingRecommendationService.recommend(currentUser.userId(), request));
    }

    @PostMapping("/bookmarks")
    public ApiResponse<Map<String, Object>> bookmark(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody BookmarkRequest request
    ) {
        Map<String, Object> result = matchingService.bookmark(currentUser.userId(), request);
        String message = Boolean.TRUE.equals(result.get("created")) ? "저장했습니다." : "이미 저장된 항목입니다.";
        return ApiResponse.ok(result, message);
    }

    @GetMapping("/bookmarks")
    public ApiResponse<List<Map<String, Object>>> bookmarks(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(defaultValue = "TEAM") String targetType
    ) {
        return ApiResponse.ok(matchingService.bookmarks(currentUser.userId(), targetType));
    }

    @DeleteMapping("/bookmarks/{targetType}/{targetId}")
    public ApiResponse<Map<String, Object>> deleteBookmark(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String targetType,
            @PathVariable Long targetId
    ) {
        Map<String, Object> result = matchingService.deleteBookmark(currentUser.userId(), targetType, targetId);
        String message = Boolean.TRUE.equals(result.get("removed")) ? "저장을 취소했습니다." : "이미 저장이 취소된 항목입니다.";
        return ApiResponse.ok(result, message);
    }

    @PostMapping("/invitations")
    public ApiResponse<Map<String, Object>> invite(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody InvitationRequest request
    ) {
        return ApiResponse.ok(matchingService.invite(currentUser.userId(), request), "초대를 보냈습니다.");
    }

    @GetMapping("/invitations")
    public ApiResponse<List<Map<String, Object>>> sentInvitations(
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ApiResponse.ok(matchingService.sentInvitations(currentUser.userId()));
    }

    @DeleteMapping("/invitations/{invitationId}")
    public ApiResponse<Map<String, Object>> cancelInvitation(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long invitationId
    ) {
        Map<String, Object> result = matchingService.cancelInvitation(currentUser.userId(), invitationId);
        String message = Boolean.TRUE.equals(result.get("canceled")) ? "초대를 취소했습니다." : "이미 처리된 초대입니다.";
        return ApiResponse.ok(result, message);
    }

    @PostMapping("/applications")
    public ApiResponse<Map<String, Object>> apply(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody ApplicationRequest request
    ) {
        return ApiResponse.ok(matchingService.apply(currentUser.userId(), request), "지원을 보냈습니다.");
    }

    @GetMapping("/applications")
    public ApiResponse<List<Map<String, Object>>> sentApplications(
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ApiResponse.ok(matchingService.sentApplications(currentUser.userId()));
    }

    @DeleteMapping("/applications/{applicationId}")
    public ApiResponse<Map<String, Object>> cancelApplication(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long applicationId
    ) {
        Map<String, Object> result = matchingService.cancelApplication(currentUser.userId(), applicationId);
        String message = Boolean.TRUE.equals(result.get("canceled")) ? "지원을 취소했습니다." : "이미 처리된 지원입니다.";
        return ApiResponse.ok(result, message);
    }

    private Map<String, Object> queryMap(MultiValueMap<String, String> params) {
        Map<String, Object> query = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            List<String> values = entry.getValue().stream()
                    .flatMap(value -> List.of(value.split(",")).stream())
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toList();
            if (values.isEmpty()) {
                continue;
            }
            if (entry.getKey().endsWith("Ids") || values.size() > 1) {
                query.put(entry.getKey(), values);
            } else {
                query.put(entry.getKey(), values.get(0));
            }
        }
        return query;
    }

    public record BookmarkRequest(
            @NotBlank String targetType,
            @NotNull Long targetId
    ) {
    }

    public record InvitationRequest(
            @NotNull Long teamId,
            @NotNull Long recruitmentId,
            @NotNull Long slotId,
            @NotNull Long targetUserId,
            @NotBlank @Size(min = 2, max = 300) String message
    ) {
    }

    public record ApplicationRequest(
            @NotNull Long teamId,
            @NotNull Long recruitmentId,
            @NotNull Long slotId,
            @NotBlank @Size(min = 2, max = 300) String message
    ) {
    }

    public enum AiRecommendationType {
        TEAM_TO_MEMBER,
        MEMBER_TO_TEAM
    }

    public record AiRecommendationRequest(
            @NotNull AiRecommendationType type,
            Long teamId,
            Long slotId,
            Long profileId
    ) {
    }

    public record AiRecommendationResponse(
            AiRecommendationType type,
            List<AiRecommendationItem> recommendations
    ) {
    }

    public record AiRecommendationItem(
            Long targetId,
            String targetType,
            String targetName,
            String reason
    ) {
    }
}
