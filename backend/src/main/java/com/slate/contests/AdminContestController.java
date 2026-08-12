package com.slate.contests;

import java.util.List;
import java.util.Map;

import com.slate.common.ApiResponse;
import com.slate.security.CurrentUser;
import jakarta.validation.Valid;
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
@RequestMapping("/api/admin/contests")
@PreAuthorize("hasRole('ADMIN')")
public class AdminContestController {

    private final ContestService contestService;
    private final AdminContestKoreaCrawlerService contestKoreaCrawlerService;

    public AdminContestController(
            ContestService contestService,
            AdminContestKoreaCrawlerService contestKoreaCrawlerService
    ) {
        this.contestService = contestService;
        this.contestKoreaCrawlerService = contestKoreaCrawlerService;
    }

    @GetMapping("/requests")
    public ApiResponse<List<Map<String, Object>>> openRequests(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "30") Integer limit
    ) {
        return ApiResponse.ok(contestService.adminOpenRequests(currentUser.userId(), status, limit));
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> contests(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(required = false) String contestType,
            @RequestParam(required = false) Long requesterCompanyUserId,
            @RequestParam(defaultValue = "50") Integer limit
    ) {
        return ApiResponse.ok(contestService.adminManagedContests(currentUser.userId(), status, contestType, requesterCompanyUserId, limit));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> createContest(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody ContestPayloadRequest request
    ) {
        return ApiResponse.ok(contestService.adminCreateContest(currentUser.userId(), request), "공모전을 등록했습니다.");
    }

    @PutMapping("/{contestId}")
    public ApiResponse<Map<String, Object>> updateContest(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long contestId,
            @Valid @RequestBody ContestPayloadRequest request
    ) {
        return ApiResponse.ok(contestService.adminUpdateContest(currentUser.userId(), contestId, request), "공모전을 수정했습니다.");
    }

    @PostMapping("/{contestId}/status")
    public ApiResponse<Map<String, Object>> updateContestStatus(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long contestId,
            @Valid @RequestBody ContestStatusRequest request
    ) {
        return ApiResponse.ok(contestService.adminUpdateContestStatus(currentUser.userId(), contestId, request), "공모전 상태를 변경했습니다.");
    }

    @DeleteMapping
    public ApiResponse<Map<String, Object>> deleteContests(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody ContestDeleteRequest request
    ) {
        return ApiResponse.ok(contestService.adminDeleteContests(currentUser.userId(), request), "선택한 공모전을 삭제했습니다.");
    }

    @PostMapping("/requests/{requestId}/decision")
    public ApiResponse<Map<String, Object>> decideOpenRequest(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long requestId,
            @Valid @RequestBody ContestRequestDecisionRequest request
    ) {
        return ApiResponse.ok(contestService.decideOpenRequest(currentUser.userId(), requestId, request), "공모전 개설 요청을 처리했습니다.");
    }

    @PostMapping("/crawl-sources/contest-korea/run")
    public ApiResponse<ContestKoreaCrawlerRunResult> runContestKoreaCrawler(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestBody(required = false) ContestKoreaCrawlerRunRequest request
    ) {
        return ApiResponse.ok(
                contestKoreaCrawlerService.run(currentUser.userId(), request),
                "콘테스트코리아 크롤링 실행이 완료되었습니다."
        );
    }

    public record ContestRequestDecisionRequest(
            @NotBlank String decision,
            @Size(max = 500) String reason
    ) {
    }

    public record ContestStatusRequest(
            @NotBlank String status,
            @Size(max = 500) String reason
    ) {
    }

    public record ContestDeleteRequest(
            List<Long> contestIds,
            @Size(max = 500) String reason
    ) {
    }
}
