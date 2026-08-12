package com.slate.profiles;

import java.util.List;
import java.util.Map;

import com.slate.common.ApiResponse;
import com.slate.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> myProfile(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(profileService.byUserId(currentUser.userId()));
    }

    @GetMapping("/{profileId}")
    public ApiResponse<Map<String, Object>> profile(@PathVariable Long profileId) {
        return ApiResponse.ok(profileService.byProfileId(profileId));
    }

    @GetMapping("/public/{profileId}")
    public ApiResponse<Map<String, Object>> publicProfile(@PathVariable Long profileId) {
        return ApiResponse.ok(profileService.publicByProfileId(profileId));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody ProfileRequest request
    ) {
        return ApiResponse.ok(profileService.create(currentUser.userId(), request), "프로필을 생성했습니다.");
    }

    @PutMapping("/{profileId}")
    public ApiResponse<Map<String, Object>> update(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long profileId,
            @Valid @RequestBody ProfileRequest request
    ) {
        return ApiResponse.ok(profileService.update(currentUser.userId(), profileId, request), "프로필을 수정했습니다.");
    }

    @DeleteMapping("/me")
    public ApiResponse<Map<String, Object>> deleteMyProfile(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(profileService.deleteMyProfile(currentUser.userId()), "프로필을 삭제했습니다.");
    }

    @GetMapping("/me/portfolio-items")
    public ApiResponse<List<Map<String, Object>>> myPortfolioItems(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(profileService.myPortfolioItems(currentUser.userId()));
    }

    @PostMapping("/me/portfolio-items")
    public ApiResponse<Map<String, Object>> createPortfolioItem(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody PortfolioItemRequest request
    ) {
        return ApiResponse.ok(profileService.createPortfolioItem(currentUser.userId(), request), "포트폴리오를 추가했습니다.");
    }

    @PutMapping("/me/portfolio-items/{portfolioItemId}")
    public ApiResponse<Map<String, Object>> updatePortfolioItem(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long portfolioItemId,
            @Valid @RequestBody PortfolioItemRequest request
    ) {
        return ApiResponse.ok(profileService.updatePortfolioItem(currentUser.userId(), portfolioItemId, request), "포트폴리오를 수정했습니다.");
    }

    @DeleteMapping("/me/portfolio-items/{portfolioItemId}")
    public ApiResponse<Map<String, Object>> deletePortfolioItem(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long portfolioItemId
    ) {
        return ApiResponse.ok(profileService.deletePortfolioItem(currentUser.userId(), portfolioItemId), "포트폴리오를 삭제했습니다.");
    }

    @GetMapping("/public-data/search")
    public ApiResponse<List<Map<String, Object>>> publicDataSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String itemType,
            @RequestParam(defaultValue = "20") Integer limit
    ) {
        return ApiResponse.ok(profileService.searchPublicData(keyword, itemType, limit));
    }

    @GetMapping("/public-data/kobis/movies")
    public ApiResponse<List<Map<String, Object>>> kobisMovieSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        return ApiResponse.ok(profileService.searchKobisMovies(keyword, limit));
    }

    @PostMapping("/me/portfolio-items/from-public-data")
    public ApiResponse<Map<String, Object>> createPortfolioItemFromPublicData(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody PublicDataPortfolioRequest request
    ) {
        return ApiResponse.ok(profileService.createPortfolioItemFromPublicData(currentUser.userId(), request), "공공데이터 항목을 포트폴리오에 추가했습니다.");
    }

    public record ProfileRequest(
            @NotBlank @Size(max = 50) String displayName,
            @NotBlank @Size(max = 120) String shortIntro,
            @Size(max = 2000) String detailIntro,
            @NotBlank String visibility,
            @NotBlank String activityStatus,
            @NotNull Long regionId,
            @NotEmpty @Size(max = 5) List<Long> roleIds,
            @NotEmpty List<Long> genreIds,
            @NotBlank String experienceLevel,
            @NotBlank String joinAvailability,
            @NotBlank String collaborationStatus,
            @NotEmpty List<String> collaborationConditionCodes,
            @NotBlank String travelRange,
            @NotBlank String preferredDuration,
            String equipmentStatus,
            String ageBand,
            String participationMode
    ) {
    }

    public record PortfolioItemRequest(
            Long publicDataSyncItemId,
            @NotBlank @Size(max = 150) String title,
            @Size(max = 80) String roleName,
            @Size(max = 1000) String description,
            @NotBlank String sourceType,
            @Size(max = 80) String externalSourceName,
            @Size(max = 100) String externalReferenceId,
            @Size(max = 500) String url,
            @Size(max = 500) String thumbnailUrl,
            Integer sortOrder,
            @Size(max = 50) String kobisMovieCd,
            @Size(max = 200) String kobisMovieNm,
            @Size(max = 200) String kobisMovieNmEn,
            @Size(max = 20) String kobisPrdtYear,
            @Size(max = 20) String kobisOpenDt,
            @Size(max = 300) String kobisGenreAlt,
            @Size(max = 120) String creditName
    ) {
    }

    public record PublicDataPortfolioRequest(
            @NotNull Long publicDataSyncItemId,
            @Size(max = 150) String titleOverride,
            @Size(max = 80) String roleName,
            @Size(max = 1000) String description,
            @Size(max = 500) String url,
            Integer sortOrder
    ) {
    }
}
