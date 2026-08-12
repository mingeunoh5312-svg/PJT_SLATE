package com.slate.locations;

import java.util.List;
import java.util.Map;

import com.slate.common.ApiResponse;
import com.slate.common.SlateException;
import com.slate.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping("/api/locations/ai/recommendations")
    public ApiResponse<Map<String, Object>> aiRecommendations(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody LocationRecommendationRequest request
    ) {
        return ApiResponse.ok(locationService.recommend(requireUser(currentUser).userId(), request));
    }

    @GetMapping("/api/locations/sessions/{sessionId}")
    public ApiResponse<Map<String, Object>> session(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long sessionId
    ) {
        return ApiResponse.ok(locationService.session(requireUser(currentUser).userId(), sessionId));
    }

    @PostMapping("/api/locations/candidates")
    public ApiResponse<Map<String, Object>> saveCandidate(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody SaveLocationCandidateRequest request
    ) {
        Map<String, Object> result = locationService.saveCandidate(requireUser(currentUser).userId(), request);
        String message = Boolean.TRUE.equals(result.get("created")) ? "로케이션 후보를 저장했습니다." : "이미 저장된 로케이션 후보입니다.";
        return ApiResponse.ok(result, message);
    }

    @GetMapping("/api/locations/candidates")
    public ApiResponse<List<Map<String, Object>>> myCandidates(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(locationService.personalCandidates(requireUser(currentUser).userId()));
    }

    @GetMapping("/api/teams/{teamId}/locations")
    public ApiResponse<List<Map<String, Object>>> teamCandidates(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long teamId
    ) {
        return ApiResponse.ok(locationService.teamCandidates(requireUser(currentUser).userId(), teamId));
    }

    private CurrentUser requireUser(CurrentUser currentUser) {
        if (currentUser == null) {
            throw new SlateException("로그인이 필요합니다.");
        }
        return currentUser;
    }

    public record LocationRecommendationRequest(
            @NotBlank @Size(max = 2000) String prompt,
            Long teamId,
            @Size(max = 50) String sido,
            @Size(max = 80) String sigungu,
            List<@Size(max = 50) String> sidos,
            List<LocationRegionFilterRequest> regions,
            Boolean includeTeamContext,
            @Min(1) @Max(5) Integer limit
    ) {
        public LocationRecommendationRequest(
                String prompt,
                Long teamId,
                String sido,
                String sigungu,
                Boolean includeTeamContext,
                Integer limit
        ) {
            this(prompt, teamId, sido, sigungu, null, null, includeTeamContext, limit);
        }
    }

    public record LocationRegionFilterRequest(
            @Size(max = 50) String sido,
            @Size(max = 80) String sigungu
    ) {
    }

    public record SaveLocationCandidateRequest(
            @NotNull Long locationId,
            Long sessionId,
            Long recommendationId,
            Long teamId,
            @NotBlank @Size(max = 150) String title,
            @Size(max = 1000) String memo
    ) {
    }
}
