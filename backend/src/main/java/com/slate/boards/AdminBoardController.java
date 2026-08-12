package com.slate.boards;

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
@RequestMapping("/api/admin/boards/posts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBoardController {

    private final AdminBoardService adminBoardService;

    public AdminBoardController(AdminBoardService adminBoardService) {
        this.adminBoardService = adminBoardService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> posts(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) Long authorUserId,
            @RequestParam(defaultValue = "50") Integer limit
    ) {
        return ApiResponse.ok(adminBoardService.posts(
                currentUser.userId(),
                keyword,
                category,
                status,
                visibility,
                authorUserId,
                limit
        ));
    }

    @GetMapping("/{postId}")
    public ApiResponse<Map<String, Object>> post(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long postId
    ) {
        return ApiResponse.ok(adminBoardService.post(currentUser.userId(), postId));
    }

    @PutMapping("/{postId}")
    public ApiResponse<Map<String, Object>> updatePost(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long postId,
            @Valid @RequestBody AdminPostUpdateRequest request
    ) {
        return ApiResponse.ok(adminBoardService.updatePost(currentUser.userId(), postId, request), "게시글을 수정했습니다.");
    }

    @PostMapping("/{postId}/hide")
    public ApiResponse<Map<String, Object>> hidePost(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long postId,
            @Valid @RequestBody ReasonRequest request
    ) {
        return ApiResponse.ok(adminBoardService.hidePost(currentUser.userId(), postId, request.reason()), "게시글을 숨김 처리했습니다.");
    }

    @PostMapping("/{postId}/restore")
    public ApiResponse<Map<String, Object>> restorePost(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long postId,
            @Valid @RequestBody ReasonRequest request
    ) {
        return ApiResponse.ok(adminBoardService.restorePost(currentUser.userId(), postId, request.reason()), "게시글을 복구했습니다.");
    }

    @DeleteMapping("/{postId}")
    public ApiResponse<Map<String, Object>> deletePost(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long postId,
            @Valid @RequestBody ReasonRequest request
    ) {
        return ApiResponse.ok(adminBoardService.deletePost(currentUser.userId(), postId, request.reason()), "게시글을 삭제 상태로 전환했습니다.");
    }

    public record AdminPostUpdateRequest(
            @Size(max = 150) String title,
            @Size(max = 10000) String body,
            @Size(max = 10000) String content,
            String category,
            String freeCategory,
            String workType,
            String visibility,
            String status,
            @NotBlank @Size(max = 1000) String reason
    ) {
    }

    public record ReasonRequest(@NotBlank @Size(max = 1000) String reason) {
    }
}
