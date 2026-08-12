package com.slate.follows;

import java.util.Map;

import com.slate.common.ApiResponse;
import com.slate.common.SlateException;
import com.slate.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/{profileId}/follow")
    public ApiResponse<Map<String, Object>> follow(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long profileId
    ) {
        return ApiResponse.ok(followService.follow(userId(currentUser), profileId), "팔로우 상태를 반영했습니다.");
    }

    @DeleteMapping("/{profileId}/follow")
    public ApiResponse<Map<String, Object>> unfollow(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long profileId
    ) {
        return ApiResponse.ok(followService.unfollow(userId(currentUser), profileId), "팔로우 상태를 반영했습니다.");
    }

    @GetMapping("/{profileId}/follow-status")
    public ApiResponse<Map<String, Object>> status(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long profileId
    ) {
        return ApiResponse.ok(followService.status(userId(currentUser), profileId));
    }

    @GetMapping("/{profileId}/followers")
    public ApiResponse<Map<String, Object>> followers(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long profileId,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(defaultValue = "0") Integer offset
    ) {
        return ApiResponse.ok(followService.followers(userId(currentUser), profileId, limit, offset));
    }

    @GetMapping("/{profileId}/following")
    public ApiResponse<Map<String, Object>> following(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long profileId,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(defaultValue = "0") Integer offset
    ) {
        return ApiResponse.ok(followService.following(userId(currentUser), profileId, limit, offset));
    }

    private Long userId(CurrentUser currentUser) {
        if (currentUser == null || currentUser.userId() == null) {
            throw new SlateException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return currentUser.userId();
    }
}
