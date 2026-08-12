package com.slate.contests;

import java.util.List;
import java.util.Map;

import com.slate.common.ApiResponse;
import com.slate.common.SlateException;
import com.slate.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/contests")
public class ContestController {

    private final ContestService contestService;

    public ContestController(ContestService contestService) {
        this.contestService = contestService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> contests(
            @RequestParam(defaultValue = "OPEN") String status,
            @RequestParam(defaultValue = "deadline") String sort,
            @RequestParam(required = false) String contestType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer deadlineWithinDays,
            @RequestParam(required = false) List<String> target,
            @RequestParam(required = false) List<String> region,
            @RequestParam(required = false) List<String> organizerType,
            @RequestParam(required = false) Long totalPrizeMin,
            @RequestParam(required = false) Long totalPrizeMax,
            @RequestParam(required = false) Long firstPrizeMin,
            @RequestParam(required = false) Long firstPrizeMax,
            @RequestParam(defaultValue = "500") Integer limit,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ApiResponse.ok(contestService.contests(
                status, sort, contestType, keyword, deadlineWithinDays, target, region, organizerType,
                totalPrizeMin, totalPrizeMax, firstPrizeMin, firstPrizeMax,
                limit, userId(currentUser)
        ));
    }

    @GetMapping("/urgent")
    public ApiResponse<List<Map<String, Object>>> urgentContests(
            @RequestParam(defaultValue = "4") Integer limit,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ApiResponse.ok(contestService.urgentContests(limit, userId(currentUser)));
    }

    @GetMapping("/{contestId}")
    public ApiResponse<Map<String, Object>> contest(
            @PathVariable Long contestId,
            @RequestParam(required = false) String basisType,
            @RequestParam(required = false) Long basisId,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ApiResponse.ok(contestService.contest(contestId, basisType, basisId, userId(currentUser)));
    }

    @GetMapping("/bases")
    public ApiResponse<Map<String, Object>> bases(@AuthenticationPrincipal CurrentUser currentUser) {
        if (currentUser == null) {
            throw new SlateException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return ApiResponse.ok(contestService.bases(currentUser.userId()));
    }

    @PostMapping("/{contestId}/save/toggle")
    public ApiResponse<Map<String, Object>> toggleSave(
            @PathVariable Long contestId,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ApiResponse.ok(contestService.toggleSave(contestId, currentUser.userId()));
    }

    @PostMapping("/{contestId}/fit")
    public ApiResponse<Map<String, Object>> calculateFit(
            @PathVariable Long contestId,
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody FitRequest request
    ) {
        return ApiResponse.ok(contestService.calculateFit(contestId, currentUser.userId(), request));
    }

    @PostMapping("/{contestId}/prepare")
    public ApiResponse<Map<String, Object>> savePreparation(
            @PathVariable Long contestId,
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody PrepareRequest request
    ) {
        return ApiResponse.ok(contestService.savePreparation(contestId, currentUser.userId(), request), "제출 준비를 저장했습니다.");
    }

    @GetMapping("/open-requests/mine")
    public ApiResponse<List<Map<String, Object>>> myOpenRequests(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(contestService.myOpenRequests(currentUser));
    }

    @PostMapping("/open-requests")
    public ApiResponse<Map<String, Object>> createOpenRequest(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody ContestPayloadRequest request
    ) {
        return ApiResponse.ok(contestService.createOpenRequest(currentUser, request), "공모전 개설 요청을 접수했습니다.");
    }

    @GetMapping("/manage/mine")
    public ApiResponse<List<Map<String, Object>>> myManagedContests(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(contestService.myManagedContests(currentUser));
    }

    @PutMapping("/manage/{contestId}")
    public ApiResponse<Map<String, Object>> updateManagedContest(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long contestId,
            @Valid @RequestBody ContestPayloadRequest request
    ) {
        return ApiResponse.ok(contestService.updateCompanyManagedContest(currentUser, contestId, request), "공모전을 수정했습니다.");
    }

    @PostMapping("/manage/{contestId}/status")
    public ApiResponse<Map<String, Object>> updateManagedContestStatus(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long contestId,
            @Valid @RequestBody AdminContestController.ContestStatusRequest request
    ) {
        return ApiResponse.ok(contestService.updateCompanyManagedContestStatus(currentUser, contestId, request), "공모전 상태를 변경했습니다.");
    }

    private Long userId(CurrentUser currentUser) {
        return currentUser == null ? null : currentUser.userId();
    }

    public record FitRequest(
            @NotBlank String basisType,
            Long basisId
    ) {
    }

    public record PrepareRequest(
            @NotBlank String basisType,
            Long basisId,
            List<@Size(max = 120) String> checklistItems,
            @Size(max = 1000) String memo
    ) {
    }
}
