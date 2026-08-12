package com.slate.common;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.slate.operations.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final AuditLogService auditLogService;

    public GlobalExceptionHandler(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @ExceptionHandler(SlateException.class)
    public ResponseEntity<ApiResponse<Object>> handleSlate(SlateException ex) {
        if (ex.status().is5xxServerError()) {
            recordException("ERROR", "SLATE_EXCEPTION", "서비스 예외가 발생했습니다.", ex, ex.status());
        }
        return ResponseEntity.status(ex.status()).body(ApiResponse.fail(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        recordException("WARN", "VALIDATION_FAILED", "요청 검증에 실패했습니다.", ex, HttpStatus.BAD_REQUEST);
        return ResponseEntity.badRequest().body(ApiResponse.fail(message));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        recordException("WARN", "ACCESS_DENIED", "접근 권한 검증에 실패했습니다.", ex, HttpStatus.FORBIDDEN);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail("접근 권한이 없습니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnknown(Exception ex) {
        recordException("ERROR", "UNHANDLED_EXCEPTION", "처리되지 않은 서버 예외가 발생했습니다.", ex, HttpStatus.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("서버 처리 중 오류가 발생했습니다."));
    }

    private void recordException(String level, String eventCode, String message, Exception ex, HttpStatus status) {
        try {
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("status", status.value());
            context.put("exceptionType", ex.getClass().getName());
            context.put("exceptionMessage", truncate(ex.getMessage(), 500));
            auditLogService.recordOperation(level, eventCode, message, context);
        } catch (Exception ignored) {
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
