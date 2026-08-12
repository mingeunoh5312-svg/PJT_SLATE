package com.slate.operations;

import java.lang.reflect.Array;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slate.common.SlateException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;
    private final RequestLogContext requestLogContext;

    public AuditLogService(AuditLogMapper auditLogMapper, ObjectMapper objectMapper, RequestLogContext requestLogContext) {
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
        this.requestLogContext = requestLogContext;
    }

    @Transactional
    public void recordAudit(Long actorUserId, String actionType, String targetType, Long targetId, Object before, Object after) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("actorUserId", actorUserId);
        row.put("actionType", actionType);
        row.put("targetType", targetType);
        row.put("targetId", targetId);
        row.put("ipHash", requestLogContext.clientIpHash());
        row.put("beforeJson", toJson(before));
        row.put("afterJson", toJson(after));
        auditLogMapper.insertAuditLog(row);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordOperation(String logLevel, String eventCode, String message, Object context) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("logLevel", textOrDefault(logLevel, "INFO").toUpperCase());
        row.put("eventCode", eventCode);
        row.put("message", message);
        row.put("contextJson", toJson(requestLogContext.operationContext(context)));
        auditLogMapper.insertOperationLog(row);
    }

    public String fingerprint(String value) {
        return requestLogContext.hashValue(value);
    }

    public List<Map<String, Object>> auditLogs(String actionType, String targetType, Long actorUserId, Integer limit) {
        return auditLogMapper.selectAuditLogs(textOrNull(actionType), textOrNull(targetType), actorUserId, safeLimit(limit));
    }

    public List<Map<String, Object>> operationLogs(String logLevel, String eventCode, Integer limit) {
        return auditLogMapper.selectOperationLogs(textOrNull(logLevel), textOrNull(eventCode), safeLimit(limit));
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(jsonSafe(value));
        } catch (Exception ex) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("serializationError", ex.getClass().getSimpleName());
            fallback.put("value", String.valueOf(value));
            try {
                return objectMapper.writeValueAsString(fallback);
            } catch (Exception fallbackEx) {
                throw new SlateException(HttpStatus.INTERNAL_SERVER_ERROR, "감사 로그 JSON 변환 중 오류가 발생했습니다.");
            }
        }
    }

    private Object jsonSafe(Object value) {
        if (value == null
                || value instanceof String
                || value instanceof Number
                || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        if (value instanceof TemporalAccessor || value instanceof Date) {
            return value.toString();
        }
        if (value instanceof byte[] bytes) {
            return Base64.getEncoder().encodeToString(bytes);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> result.put(String.valueOf(key), jsonSafe(item)));
            return result;
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> result = new ArrayList<>();
            iterable.forEach(item -> result.add(jsonSafe(item)));
            return result;
        }
        if (value.getClass().isArray()) {
            List<Object> result = new ArrayList<>();
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                result.add(jsonSafe(Array.get(value, index)));
            }
            return result;
        }
        return String.valueOf(value);
    }

    private int safeLimit(Integer limit) {
        return Math.max(1, Math.min(limit == null ? 30 : limit, 100));
    }

    private String textOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String textOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
