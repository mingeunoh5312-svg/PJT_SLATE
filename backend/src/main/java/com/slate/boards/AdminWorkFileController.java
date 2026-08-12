package com.slate.boards;

import java.util.List;
import java.util.Map;

import com.slate.common.ApiResponse;
import com.slate.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/work-files")
@PreAuthorize("hasRole('ADMIN')")
public class AdminWorkFileController {

    private final WorkFileService workFileService;

    public AdminWorkFileController(WorkFileService workFileService) {
        this.workFileService = workFileService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> files(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long uploaderUserId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(defaultValue = "50") Integer limit
    ) {
        return ApiResponse.ok(workFileService.adminFiles(currentUser.userId(), status, keyword, uploaderUserId, teamId, limit));
    }

    @GetMapping("/storage-summary")
    public ApiResponse<Map<String, Object>> storageSummary(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(workFileService.adminStorageSummary(currentUser.userId()));
    }

    @PostMapping("/{fileId}/hold")
    public ApiResponse<Map<String, Object>> holdFile(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long fileId,
            @Valid @RequestBody(required = false) FileReasonRequest request
    ) {
        return ApiResponse.ok(workFileService.adminHoldFile(currentUser.userId(), fileId, reason(request)), "파일을 보관 처리했습니다.");
    }

    @PostMapping("/{fileId}/restore")
    public ApiResponse<Map<String, Object>> restoreFile(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long fileId,
            @Valid @RequestBody(required = false) FileReasonRequest request
    ) {
        return ApiResponse.ok(workFileService.adminRestoreFile(currentUser.userId(), fileId, reason(request)), "파일을 복구했습니다.");
    }

    @DeleteMapping("/{fileId}")
    public ApiResponse<Map<String, Object>> deleteFile(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long fileId,
            @Valid @RequestBody(required = false) FileReasonRequest request
    ) {
        return ApiResponse.ok(workFileService.adminDeleteFile(currentUser.userId(), fileId, reason(request)), "파일을 삭제 상태로 전환했습니다.");
    }

    private String reason(FileReasonRequest request) {
        return request == null ? null : request.reason();
    }

    public record FileReasonRequest(@Size(max = 500) String reason) {
    }
}
