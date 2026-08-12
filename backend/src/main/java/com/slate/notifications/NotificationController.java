package com.slate.notifications;

import java.util.List;
import java.util.Map;

import com.slate.common.ApiResponse;
import com.slate.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> notifications(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "20") Integer limit
    ) {
        return ApiResponse.ok(notificationService.notifications(currentUser.userId(), unreadOnly, limit));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Object>> unreadCount(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(notificationService.unreadCount(currentUser.userId()));
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Map<String, Object>> markRead(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long notificationId
    ) {
        return ApiResponse.ok(notificationService.markRead(currentUser.userId(), notificationId));
    }

    @PatchMapping("/read-all")
    public ApiResponse<Map<String, Object>> markAllRead(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(notificationService.markAllRead(currentUser.userId()));
    }

    @PatchMapping("/{notificationId}/hide")
    public ApiResponse<Map<String, Object>> hide(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long notificationId
    ) {
        return ApiResponse.ok(notificationService.hide(currentUser.userId(), notificationId));
    }

    @PostMapping("/admin/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> adminSend(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody AdminNotificationRequest request
    ) {
        return ApiResponse.ok(notificationService.adminSend(currentUser.userId(), request), "알림을 발송했습니다.");
    }

    @GetMapping("/admin/templates")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Map<String, Object>>> adminTemplates(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(notificationService.adminTemplates(currentUser.userId()));
    }

    @PostMapping("/admin/recipients/preview")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> adminRecipientPreview(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody AdminNotificationRequest request
    ) {
        return ApiResponse.ok(notificationService.adminRecipientPreview(currentUser.userId(), request));
    }

    @GetMapping("/admin/batches")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Map<String, Object>>> adminDeliveryBatches(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        return ApiResponse.ok(notificationService.adminDeliveryBatches(currentUser.userId(), limit));
    }

    public record AdminNotificationRequest(
            @NotBlank String targetScope,
            String accountType,
            List<Long> userIds,
            Long teamId,
            Long templateId,
            @Size(max = 150) String title,
            @Size(max = 500) String body,
            String notificationType,
            String targetType,
            Long targetId
    ) {
    }
}
