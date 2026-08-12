package com.slate.boards;

import java.util.List;
import java.util.Map;

import com.slate.common.ApiResponse;
import com.slate.common.SlateException;
import com.slate.moderation.ModerationService;
import com.slate.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;
    private final ModerationService moderationService;
    private final WorkFileService workFileService;
    private final YoutubeClient youtubeClient;

    public BoardController(
            BoardService boardService,
            ModerationService moderationService,
            WorkFileService workFileService,
            YoutubeClient youtubeClient
    ) {
        this.boardService = boardService;
        this.moderationService = moderationService;
        this.workFileService = workFileService;
        this.youtubeClient = youtubeClient;
    }

    @GetMapping("/posts")
    public ApiResponse<List<Map<String, Object>>> posts(
            @RequestParam(defaultValue = "WORK") String category,
            @RequestParam(defaultValue = "reaction") String sort,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String freeCategory,
            @RequestParam(required = false) String workType,
            @RequestParam(required = false) Long genreId,
            @RequestParam(defaultValue = "20") Integer limit,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ApiResponse.ok(boardService.posts(category, sort, keyword, freeCategory, workType, genreId, limit, userId(currentUser), isAdmin(currentUser)));
    }

    @GetMapping("/posts/my-works")
    public ApiResponse<List<Map<String, Object>>> myWorks(
            @RequestParam(defaultValue = "100") Integer limit,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ApiResponse.ok(boardService.myWorks(currentUser.userId(), limit));
    }

    @GetMapping("/posts/{postId}")
    public ApiResponse<Map<String, Object>> post(
            @PathVariable Long postId,
            @AuthenticationPrincipal CurrentUser currentUser,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(boardService.post(postId, userId(currentUser), isAdmin(currentUser), request));
    }

    @PostMapping("/posts")
    public ApiResponse<Map<String, Object>> createPost(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody BoardPostRequest request
    ) {
        return ApiResponse.ok(boardService.createPost(currentUser, request), "게시글을 작성했습니다.");
    }

    @PutMapping("/posts/{postId}")
    public ApiResponse<Map<String, Object>> updatePost(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long postId,
            @Valid @RequestBody BoardPostRequest request
    ) {
        return ApiResponse.ok(boardService.updatePost(currentUser, postId, request), "게시글을 수정했습니다.");
    }

    @DeleteMapping("/posts/{postId}")
    public ApiResponse<Map<String, Object>> deletePost(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long postId
    ) {
        return ApiResponse.ok(boardService.deletePost(currentUser, postId), "게시글을 삭제했습니다.");
    }

    @GetMapping("/posts/{postId}/reviews")
    public ApiResponse<List<Map<String, Object>>> reviews(@PathVariable Long postId) {
        return ApiResponse.ok(boardService.reviews(postId));
    }

    @PostMapping("/posts/{postId}/reviews")
    public ApiResponse<Map<String, Object>> createReview(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long postId,
            @Valid @RequestBody ReviewRequest request
    ) {
        return ApiResponse.ok(boardService.createReview(currentUser.userId(), postId, request), "리뷰를 작성했습니다.");
    }

    @PutMapping("/reviews/{reviewId}")
    public ApiResponse<Map<String, Object>> updateReview(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRequest request
    ) {
        return ApiResponse.ok(boardService.updateReview(currentUser, reviewId, request), "리뷰를 수정했습니다.");
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ApiResponse<Map<String, Object>> deleteReview(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long reviewId
    ) {
        return ApiResponse.ok(boardService.deleteReview(currentUser, reviewId), "리뷰를 삭제했습니다.");
    }

    @PostMapping("/posts/{postId}/likes/toggle")
    public ApiResponse<Map<String, Object>> toggleLike(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long postId
    ) {
        return ApiResponse.ok(boardService.toggleLike(currentUser.userId(), postId));
    }

    @PostMapping("/work-files")
    public ApiResponse<Map<String, Object>> uploadWorkFile(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Integer clientDurationSeconds
    ) {
        return ApiResponse.ok(workFileService.upload(currentUser.userId(), teamId, clientDurationSeconds, file), "작업물 파일을 업로드했습니다.");
    }

    @GetMapping("/work-files/mine")
    public ApiResponse<Map<String, Object>> myWorkFiles(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "30") Integer limit
    ) {
        return ApiResponse.ok(workFileService.myFiles(currentUser.userId(), status, limit));
    }

    @DeleteMapping("/work-files/{fileId}")
    public ApiResponse<Map<String, Object>> deleteOwnWorkFile(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long fileId
    ) {
        return ApiResponse.ok(workFileService.deleteOwnFile(currentUser.userId(), fileId), "파일을 삭제 상태로 전환했습니다.");
    }

    @PostMapping("/youtube/preview")
    public ApiResponse<YoutubePreviewResponse> previewYoutube(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody YoutubePreviewRequest request
    ) {
        if (currentUser == null) {
            throw new SlateException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        YoutubeVideoMetadata metadata = youtubeClient.fetchMetadata(request.youtubeUrl());
        return ApiResponse.ok(YoutubePreviewResponse.from(metadata));
    }

    @PostMapping("/work-files/{fileId}/restore")
    public ApiResponse<Map<String, Object>> restoreOwnWorkFile(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long fileId
    ) {
        return ApiResponse.ok(workFileService.restoreOwnFile(currentUser.userId(), fileId), "파일을 복구했습니다.");
    }

    @GetMapping("/team-work-requests/mine")
    public ApiResponse<List<Map<String, Object>>> myTeamWorkRequests(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(boardService.myTeamWorkRequests(currentUser.userId()));
    }

    @PostMapping("/team-work-requests")
    public ApiResponse<Map<String, Object>> createTeamWorkRequest(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody BoardPostRequest request
    ) {
        return ApiResponse.ok(boardService.createTeamWorkRequest(currentUser.userId(), request), "팀 작업물 승인 요청을 보냈습니다.");
    }

    @GetMapping("/teams/{teamId}/work-requests")
    public ApiResponse<List<Map<String, Object>>> teamWorkRequests(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long teamId
    ) {
        return ApiResponse.ok(boardService.teamWorkRequests(currentUser.userId(), teamId));
    }

    @PostMapping("/team-work-requests/{requestId}/decision")
    public ApiResponse<Map<String, Object>> decideTeamWorkRequest(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long requestId,
            @Valid @RequestBody TeamWorkDecisionRequest request
    ) {
        return ApiResponse.ok(boardService.decideTeamWorkRequest(currentUser.userId(), requestId, request), "팀 작업물 승인 요청을 처리했습니다.");
    }

    @GetMapping("/work-files/{fileId}/stream")
    public ResponseEntity<Resource> streamWorkFile(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long fileId
    ) {
        return workFileService.stream(fileId, currentUser);
    }

    @PostMapping("/posts/{postId}/reports")
    public ApiResponse<Map<String, Object>> reportPost(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long postId,
            @Valid @RequestBody ReportRequest request
    ) {
        return ApiResponse.ok(moderationService.reportPost(currentUser.userId(), postId, request), "게시글 신고를 접수했습니다.");
    }

    @PostMapping("/reviews/{reviewId}/reports")
    public ApiResponse<Map<String, Object>> reportReview(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReportRequest request
    ) {
        return ApiResponse.ok(moderationService.reportReview(currentUser.userId(), reviewId, request), "리뷰 신고를 접수했습니다.");
    }

    @GetMapping("/rankings")
    public ApiResponse<List<Map<String, Object>>> rankings(
            @RequestParam(defaultValue = "POPULAR_WORK") String type,
            @RequestParam(required = false) String workType,
            @RequestParam(required = false) Long genreId,
            @RequestParam(defaultValue = "10") Integer limit,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ApiResponse.ok(boardService.rankings(type, workType, genreId, limit, userId(currentUser)));
    }

    private Long userId(CurrentUser currentUser) {
        return currentUser == null ? null : currentUser.userId();
    }

    private boolean isAdmin(CurrentUser currentUser) {
        return currentUser != null && currentUser.isAdmin();
    }

    public record BoardPostRequest(
            @NotBlank String category,
            String freeCategory,
            @NotBlank @Size(max = 150) String title,
            @NotBlank @Size(max = 10000) String content,
            String visibility,
            WorkRequest work
    ) {
    }

    public record WorkRequest(
            Long teamId,
            Long fileId,
            @Size(max = 150) String title,
            @Size(max = 2000) String description,
            String mediaType,
            String workType,
            List<Long> genreIds,
            @Size(max = 500) String youtubeUrl,
            String visibility
    ) {
    }

    public record TeamWorkDecisionRequest(
            @NotBlank String decision,
            @Size(max = 500) String reason
    ) {
    }

    public record ReviewRequest(
            Long parentReviewId,
            @NotBlank @Size(min = 2, max = 300) String content
    ) {
    }

    public record ReportRequest(
            @NotBlank String reasonCode,
            @Size(max = 1000) String detail
    ) {
    }

    public record YoutubePreviewRequest(
            @NotBlank @Size(max = 500) String youtubeUrl
    ) {
    }

    public record YoutubePreviewResponse(
            String videoId,
            String embedUrl,
            String youtubeUrl,
            String title,
            String channelTitle,
            String thumbnailUrl,
            Integer durationSeconds
    ) {

        static YoutubePreviewResponse from(YoutubeVideoMetadata metadata) {
            return new YoutubePreviewResponse(
                    metadata.videoId(),
                    metadata.embedUrl(),
                    metadata.originalUrl(),
                    metadata.title(),
                    metadata.channelTitle(),
                    metadata.thumbnailUrl(),
                    metadata.durationSeconds()
            );
        }
    }
}
