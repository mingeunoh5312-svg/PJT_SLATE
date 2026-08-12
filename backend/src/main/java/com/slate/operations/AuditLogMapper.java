package com.slate.operations;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuditLogMapper {

    int insertAuditLog(Map<String, Object> auditLog);

    int insertOperationLog(Map<String, Object> operationLog);

    List<Map<String, Object>> selectAuditLogs(
            @Param("actionType") String actionType,
            @Param("targetType") String targetType,
            @Param("actorUserId") Long actorUserId,
            @Param("limit") int limit
    );

    List<Map<String, Object>> selectOperationLogs(
            @Param("logLevel") String logLevel,
            @Param("eventCode") String eventCode,
            @Param("limit") int limit
    );
}
