package com.slate.media;

import java.util.Map;

import com.slate.common.ApiResponse;
import com.slate.security.CurrentUser;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media/images")
public class MediaImageController {
    private final MediaImageService service;
    public MediaImageController(MediaImageService service) { this.service = service; }

    @PostMapping("/{entityType}/{entityId}")
    public ApiResponse<Map<String, Object>> upload(@AuthenticationPrincipal CurrentUser user, @PathVariable String entityType,
            @PathVariable Long entityId, @RequestParam MultipartFile file) {
        return ApiResponse.ok(service.upload(user.userId(), entityType, entityId, file), "이미지를 저장했습니다.");
    }
    @DeleteMapping("/{entityType}/{entityId}")
    public ApiResponse<Map<String, Object>> delete(@AuthenticationPrincipal CurrentUser user, @PathVariable String entityType, @PathVariable Long entityId) {
        return ApiResponse.ok(service.delete(user.userId(), entityType, entityId), "이미지를 삭제했습니다.");
    }
    @GetMapping("/{entityType}/{entityId}")
    public ResponseEntity<Resource> stream(@AuthenticationPrincipal CurrentUser user, @PathVariable String entityType, @PathVariable Long entityId) {
        return service.stream(user == null ? null : user.userId(), entityType, entityId);
    }
}
