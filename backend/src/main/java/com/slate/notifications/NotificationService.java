package com.slate.notifications;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slate.admin.AdminPermissionCatalog;
import com.slate.admin.AdminPermissionService;
import com.slate.common.SlateException;
import com.slate.notifications.NotificationController.AdminNotificationRequest;
import com.slate.operations.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class NotificationService {

    private static final int ADMIN_SEND_CHUNK_SIZE = 200;

    private final NotificationMapper notificationMapper;
    private final AdminPermissionService adminPermissionService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public NotificationService(
            NotificationMapper notificationMapper,
            AdminPermissionService adminPermissionService,
            AuditLogService auditLogService,
            ObjectMapper objectMapper
    ) {
        this.notificationMapper = notificationMapper;
        this.adminPermissionService = adminPermissionService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> notifications(Long userId, boolean unreadOnly, Integer limit) {
        return notificationMapper.selectNotifications(userId, unreadOnly, safeLimit(limit));
    }

    public Map<String, Object> unreadCount(Long userId) {
        return Map.of("unreadCount", notificationMapper.countUnread(userId));
    }

    @Transactional
    public Map<String, Object> markRead(Long userId, Long notificationId) {
        notificationMapper.markRead(userId, notificationId);
        return unreadCount(userId);
    }

    @Transactional
    public Map<String, Object> markAllRead(Long userId) {
        int updated = notificationMapper.markAllRead(userId);
        Map<String, Object> result = new LinkedHashMap<>(unreadCount(userId));
        result.put("updated", updated);
        return result;
    }

    @Transactional
    public Map<String, Object> hide(Long userId, Long notificationId) {
        notificationMapper.hide(userId, notificationId);
        return unreadCount(userId);
    }

    @Transactional
    public void send(Long recipientUserId, Long senderUserId, String notificationType, String title, String body, String targetType, Long targetId) {
        if (recipientUserId == null || !StringUtils.hasText(title) || !StringUtils.hasText(body)) {
            return;
        }
        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("recipientUserId", recipientUserId);
        notification.put("senderUserId", senderUserId);
        notification.put("notificationType", textOrDefault(notificationType, "SYSTEM"));
        notification.put("title", title.trim());
        notification.put("body", body.trim());
        notification.put("targetType", textOrNull(targetType));
        notification.put("targetId", targetId);
        notification.put("batchId", null);
        notificationMapper.insertNotification(notification);
    }

    @Transactional
    public void sendToTeamManagers(Long teamId, Long senderUserId, String title, String body, String targetType, Long targetId) {
        for (Long recipientUserId : notificationMapper.selectTeamManagers(teamId)) {
            send(recipientUserId, senderUserId, "TEAM", title, body, targetType, targetId);
        }
    }

    @Transactional
    public Map<String, Object> adminSend(Long adminUserId, AdminNotificationRequest request) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.NOTIFICATION_SEND);
        List<Long> recipients = recipients(request);
        if (recipients.isEmpty()) {
            throw new SlateException("알림 수신 대상이 없습니다.");
        }
        Map<String, Object> template = templateOrNull(request.templateId());
        NotificationContent content = resolveContent(request, template);
        Map<String, Object> batch = deliveryBatch(adminUserId, request, template, content, recipients.size());
        notificationMapper.insertDeliveryBatch(batch);
        Long batchId = ((Number) batch.get("batchId")).longValue();
        int sentCount = 0;
        int chunkCount = 0;
        for (int start = 0; start < recipients.size(); start += ADMIN_SEND_CHUNK_SIZE) {
            List<Long> chunk = recipients.subList(start, Math.min(start + ADMIN_SEND_CHUNK_SIZE, recipients.size()));
            List<Map<String, Object>> notifications = chunk.stream()
                    .map(recipientUserId -> notificationRow(batchId, recipientUserId, adminUserId, content))
                    .toList();
            notificationMapper.insertNotifications(notifications);
            sentCount += notifications.size();
            chunkCount++;
        }
        notificationMapper.completeDeliveryBatch(batchId, sentCount, chunkCount);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batchId", batchId);
        result.put("recipientCount", recipients.size());
        result.put("sentCount", sentCount);
        result.put("chunkCount", chunkCount);
        result.put("templateId", template == null ? null : template.get("templateId"));
        result.put("templateCode", template == null ? null : template.get("templateCode"));
        Map<String, Object> auditAfter = new LinkedHashMap<>();
        auditAfter.put("targetScope", textOrDefault(request.targetScope(), "ALL").toUpperCase(Locale.ROOT));
        auditAfter.put("recipientCount", recipients.size());
        auditAfter.put("sentCount", sentCount);
        auditAfter.put("chunkCount", chunkCount);
        auditAfter.put("templateId", template == null ? null : template.get("templateId"));
        auditAfter.put("title", content.title());
        auditLogService.recordAudit(
                adminUserId,
                "ADMIN_NOTIFICATION_SENT",
                "NOTIFICATION_DELIVERY_BATCH",
                batchId,
                null,
                auditAfter
        );
        auditLogService.recordOperation(
                "INFO",
                "ADMIN_NOTIFICATION_SENT",
                "관리자 알림이 발송되었습니다.",
                Map.of("adminUserId", adminUserId, "batchId", batchId, "sentCount", sentCount, "chunkCount", chunkCount)
        );
        return result;
    }

    public List<Map<String, Object>> adminTemplates(Long adminUserId) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.NOTIFICATION_SEND);
        return notificationMapper.selectTemplates();
    }

    public Map<String, Object> adminRecipientPreview(Long adminUserId, AdminNotificationRequest request) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.NOTIFICATION_SEND);
        List<Long> recipients = recipients(request);
        Map<String, Object> template = templateOrNull(request.templateId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("targetScope", textOrDefault(request.targetScope(), "ALL").toUpperCase(Locale.ROOT));
        result.put("recipientCount", recipients.size());
        result.put("sampleRecipientIds", recipients.stream().limit(10).toList());
        if (template != null) {
            result.put("templateId", template.get("templateId"));
            result.put("templateCode", template.get("templateCode"));
            result.put("templateName", template.get("displayName"));
        }
        return result;
    }

    public List<Map<String, Object>> adminDeliveryBatches(Long adminUserId, Integer limit) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.NOTIFICATION_SEND);
        return notificationMapper.selectDeliveryBatches(Math.max(1, Math.min(limit == null ? 10 : limit, 30)));
    }

    private List<Long> recipients(AdminNotificationRequest request) {
        String scope = textOrDefault(request.targetScope(), "ALL").toUpperCase(Locale.ROOT);
        List<Long> raw = switch (scope) {
            case "ACCOUNT_TYPE" -> notificationMapper.selectRecipientsByAccountType(textOrDefault(request.accountType(), "USER").toUpperCase(Locale.ROOT));
            case "USER" -> notificationMapper.selectRecipientsByUserIds(request.userIds() == null ? List.of() : request.userIds());
            case "TEAM" -> notificationMapper.selectRecipientsByTeamId(request.teamId());
            default -> notificationMapper.selectRecipientsAll();
        };
        Set<Long> unique = new LinkedHashSet<>(raw.stream().filter(Objects::nonNull).toList());
        return unique.stream().toList();
    }

    private NotificationContent resolveContent(AdminNotificationRequest request, Map<String, Object> template) {
        String title = textOrDefault(request.title(), template == null ? null : Objects.toString(template.get("titleTemplate"), null));
        String body = textOrDefault(request.body(), template == null ? null : Objects.toString(template.get("bodyTemplate"), null));
        String notificationType = textOrDefault(
                request.notificationType(),
                template == null ? "ADMIN" : Objects.toString(template.get("notificationType"), "ADMIN")
        );
        String targetType = textOrDefault(
                request.targetType(),
                template == null ? null : Objects.toString(template.get("targetType"), null)
        );
        if (!StringUtils.hasText(title) || !StringUtils.hasText(body)) {
            throw new SlateException("알림 제목과 본문이 필요합니다.");
        }
        if (title.length() > 150) {
            throw new SlateException("알림 제목은 150자 이하로 입력해주세요.");
        }
        if (body.length() > 500) {
            throw new SlateException("알림 본문은 500자 이하로 입력해주세요.");
        }
        return new NotificationContent(notificationType, title, body, targetType, request.targetId());
    }

    private Map<String, Object> notificationRow(Long batchId, Long recipientUserId, Long adminUserId, NotificationContent content) {
        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("batchId", batchId);
        notification.put("recipientUserId", recipientUserId);
        notification.put("senderUserId", adminUserId);
        notification.put("notificationType", content.notificationType());
        notification.put("title", content.title());
        notification.put("body", content.body());
        notification.put("targetType", textOrNull(content.targetType()));
        notification.put("targetId", content.targetId());
        return notification;
    }

    private Map<String, Object> deliveryBatch(
            Long adminUserId,
            AdminNotificationRequest request,
            Map<String, Object> template,
            NotificationContent content,
            int recipientCount
    ) {
        Map<String, Object> batch = new LinkedHashMap<>();
        String scope = textOrDefault(request.targetScope(), "ALL").toUpperCase(Locale.ROOT);
        batch.put("senderUserId", adminUserId);
        batch.put("templateId", template == null ? null : template.get("templateId"));
        batch.put("targetScope", scope);
        batch.put("accountType", "ACCOUNT_TYPE".equals(scope) ? textOrNull(request.accountType()) : null);
        batch.put("teamId", "TEAM".equals(scope) ? request.teamId() : null);
        batch.put("recipientCount", recipientCount);
        batch.put("title", content.title());
        batch.put("body", content.body());
        batch.put("notificationType", content.notificationType());
        batch.put("targetType", textOrNull(content.targetType()));
        batch.put("targetId", content.targetId());
        batch.put("contextJson", toJson(Map.of(
                "userIdsCount", request.userIds() == null ? 0 : request.userIds().size(),
                "chunkSize", ADMIN_SEND_CHUNK_SIZE
        )));
        return batch;
    }

    private Map<String, Object> templateOrNull(Long templateId) {
        if (templateId == null) {
            return null;
        }
        Map<String, Object> template = notificationMapper.selectTemplateById(templateId);
        if (template == null) {
            throw new SlateException("사용할 수 없는 알림 템플릿입니다.");
        }
        return template;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new SlateException(HttpStatus.INTERNAL_SERVER_ERROR, "알림 발송 배치 JSON 변환 중 오류가 발생했습니다.");
        }
    }

    private int safeLimit(Integer limit) {
        return Math.max(1, Math.min(limit == null ? 20 : limit, 50));
    }

    private String textOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String textOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private record NotificationContent(String notificationType, String title, String body, String targetType, Long targetId) {
    }
}
