package com.slate.notifications;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotificationMapper {

    List<Map<String, Object>> selectNotifications(
            @Param("userId") Long userId,
            @Param("unreadOnly") boolean unreadOnly,
            @Param("limit") int limit
    );

    int countUnread(@Param("userId") Long userId);

    int markRead(@Param("userId") Long userId, @Param("notificationId") Long notificationId);

    int markAllRead(@Param("userId") Long userId);

    int hide(@Param("userId") Long userId, @Param("notificationId") Long notificationId);

    int insertNotification(Map<String, Object> notification);

    int insertNotifications(@Param("notifications") List<Map<String, Object>> notifications);

    List<Map<String, Object>> selectTemplates();

    Map<String, Object> selectTemplateById(@Param("templateId") Long templateId);

    int insertDeliveryBatch(Map<String, Object> batch);

    int completeDeliveryBatch(
            @Param("batchId") Long batchId,
            @Param("sentCount") int sentCount,
            @Param("chunkCount") int chunkCount
    );

    List<Map<String, Object>> selectDeliveryBatches(@Param("limit") int limit);

    List<Long> selectRecipientsAll();

    List<Long> selectRecipientsByAccountType(@Param("accountType") String accountType);

    List<Long> selectRecipientsByUserIds(@Param("userIds") List<Long> userIds);

    List<Long> selectRecipientsByTeamId(@Param("teamId") Long teamId);

    List<Long> selectTeamManagers(@Param("teamId") Long teamId);
}
