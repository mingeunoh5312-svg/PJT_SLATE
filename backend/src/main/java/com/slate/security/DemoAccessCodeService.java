package com.slate.security;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.slate.admin.AdminPermissionCatalog;
import com.slate.admin.AdminPermissionService;
import com.slate.common.SlateException;
import com.slate.operations.AuditLogService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DemoAccessCodeService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_BYTES = 18;

    private final DemoAccessCodeMapper demoAccessCodeMapper;
    private final AdminPermissionService adminPermissionService;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    public DemoAccessCodeService(
            DemoAccessCodeMapper demoAccessCodeMapper,
            AdminPermissionService adminPermissionService,
            AuditLogService auditLogService,
            PasswordEncoder passwordEncoder
    ) {
        this.demoAccessCodeMapper = demoAccessCodeMapper;
        this.adminPermissionService = adminPermissionService;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> codes(Long adminUserId) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.DEMO_ACCESS_MANAGE);
        return demoAccessCodeMapper.selectAdminCodes().stream()
                .map(this::adminView)
                .toList();
    }

    @Transactional
    public Map<String, Object> create(Long adminUserId, DemoAccessCodeCreateRequest request) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.DEMO_ACCESS_MANAGE);
        String label = cleanLabel(request.label());
        validateTimes(request.startsAt(), request.expiresAt());
        Integer maxUses = cleanMaxUses(request.maxUses());

        String plainCode = generatePlainCode();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("label", label);
        params.put("codeHash", passwordEncoder.encode(plainCode));
        params.put("codeFingerprint", auditLogService.fingerprint(plainCode));
        params.put("startsAt", request.startsAt());
        params.put("expiresAt", request.expiresAt());
        params.put("maxUses", maxUses);
        params.put("createdBy", adminUserId);
        demoAccessCodeMapper.insertCode(params);

        Long codeId = longValue(params.get("codeId"));
        Map<String, Object> created = requireCode(codeId);
        Map<String, Object> safeCreated = adminView(created);
        auditLogService.recordAudit(adminUserId, "DEMO_ACCESS_CODE_CREATED", "DEMO_ACCESS_CODE", codeId, null, safeCreated);
        auditLogService.recordOperation(
                "INFO",
                "DEMO_ACCESS_CODE_CREATED",
                "Demo Access 접근 코드가 생성되었습니다.",
                Map.of("codeId", codeId, "source", "DB")
        );

        Map<String, Object> result = new LinkedHashMap<>(safeCreated);
        result.put("plainCode", plainCode);
        return result;
    }

    @Transactional
    public Map<String, Object> update(Long adminUserId, Long codeId, DemoAccessCodeUpdateRequest request) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.DEMO_ACCESS_MANAGE);
        Map<String, Object> before = requireCode(codeId);
        String label = cleanLabel(request.label());
        validateTimes(request.startsAt(), request.expiresAt());
        Integer maxUses = cleanMaxUses(request.maxUses());

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("codeId", codeId);
        params.put("label", label);
        params.put("startsAt", request.startsAt());
        params.put("expiresAt", request.expiresAt());
        params.put("maxUses", maxUses);
        params.put("updatedBy", adminUserId);
        demoAccessCodeMapper.updateCode(params);
        Map<String, Object> after = requireCode(codeId);
        auditLogService.recordAudit(adminUserId, "DEMO_ACCESS_CODE_UPDATED", "DEMO_ACCESS_CODE", codeId, adminView(before), adminView(after));
        auditLogService.recordOperation(
                "INFO",
                "DEMO_ACCESS_CODE_UPDATED",
                "Demo Access 접근 코드 설정이 수정되었습니다.",
                Map.of("codeId", codeId)
        );
        return adminView(after);
    }

    @Transactional
    public Map<String, Object> revoke(Long adminUserId, Long codeId, DemoAccessCodeRevokeRequest request) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.DEMO_ACCESS_MANAGE);
        Map<String, Object> before = requireCode(codeId);
        String reason = StringUtils.hasText(request.reason()) ? request.reason().trim() : "관리자 폐기";
        if (reason.length() > 500) {
            throw new SlateException("폐기 사유는 500자 이내로 입력해주세요.");
        }
        demoAccessCodeMapper.revokeCode(codeId, adminUserId, reason);
        Map<String, Object> after = requireCode(codeId);
        auditLogService.recordAudit(adminUserId, "DEMO_ACCESS_CODE_REVOKED", "DEMO_ACCESS_CODE", codeId, adminView(before), adminView(after));
        auditLogService.recordOperation(
                "INFO",
                "DEMO_ACCESS_CODE_REVOKED",
                "Demo Access 접근 코드가 폐기되었습니다.",
                Map.of("codeId", codeId)
        );
        return adminView(after);
    }

    Map<String, Object> adminView(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>(row);
        result.put("effectiveStatus", effectiveStatus(row, LocalDateTime.now()));
        result.remove("codeHash");
        result.remove("codeFingerprint");
        return result;
    }

    String effectiveStatus(Map<String, Object> row, LocalDateTime now) {
        String status = text(row.get("status"));
        if ("REVOKED".equals(status)) {
            return "REVOKED";
        }
        LocalDateTime startsAt = localDateTime(row.get("startsAt"));
        if (startsAt != null && startsAt.isAfter(now)) {
            return "SCHEDULED";
        }
        LocalDateTime expiresAt = localDateTime(row.get("expiresAt"));
        if (expiresAt != null && !expiresAt.isAfter(now)) {
            return "EXPIRED";
        }
        Integer maxUses = intValue(row.get("maxUses"));
        int usedCount = intValue(row.get("usedCount"), 0);
        if (maxUses != null && usedCount >= maxUses) {
            return "EXHAUSTED";
        }
        return "ACTIVE";
    }

    private Map<String, Object> requireCode(Long codeId) {
        Map<String, Object> row = demoAccessCodeMapper.selectAdminCodeById(codeId);
        if (row == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "접근 코드를 찾을 수 없습니다.");
        }
        return row;
    }

    private String generatePlainCode() {
        byte[] bytes = new byte[CODE_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String cleanLabel(String value) {
        if (!StringUtils.hasText(value)) {
            throw new SlateException("코드 표시 이름을 입력해주세요.");
        }
        String label = value.trim();
        if (label.length() > 100) {
            throw new SlateException("코드 표시 이름은 100자 이내로 입력해주세요.");
        }
        return label;
    }

    private Integer cleanMaxUses(Integer value) {
        if (value == null) {
            return null;
        }
        if (value < 1) {
            throw new SlateException("최대 사용 횟수는 1 이상이어야 합니다.");
        }
        return value;
    }

    private void validateTimes(LocalDateTime startsAt, LocalDateTime expiresAt) {
        if (expiresAt == null) {
            throw new SlateException("만료 시각을 입력해주세요.");
        }
        if (startsAt != null && !startsAt.isBefore(expiresAt)) {
            throw new SlateException("시작 시각은 만료 시각보다 이전이어야 합니다.");
        }
        if (!expiresAt.isAfter(LocalDateTime.now())) {
            throw new SlateException("만료 시각은 현재 이후여야 합니다.");
        }
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null && StringUtils.hasText(String.valueOf(value))) {
            return Long.parseLong(String.valueOf(value));
        }
        return null;
    }

    private Integer intValue(Object value) {
        return intValue(value, null);
    }

    private Integer intValue(Object value, Integer fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null && StringUtils.hasText(String.valueOf(value))) {
            return Integer.parseInt(String.valueOf(value));
        }
        return fallback;
    }

    private LocalDateTime localDateTime(Object value) {
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof java.util.Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        }
        if (value != null && StringUtils.hasText(String.valueOf(value))) {
            return LocalDateTime.parse(String.valueOf(value).replace(' ', 'T'));
        }
        return null;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    public record DemoAccessCodeCreateRequest(
            @NotBlank @Size(max = 100) String label,
            LocalDateTime startsAt,
            @NotNull LocalDateTime expiresAt,
            @Min(1) Integer maxUses
    ) {
    }

    public record DemoAccessCodeUpdateRequest(
            @NotBlank @Size(max = 100) String label,
            LocalDateTime startsAt,
            @NotNull LocalDateTime expiresAt,
            @Min(1) Integer maxUses
    ) {
    }

    public record DemoAccessCodeRevokeRequest(@Size(max = 500) String reason) {
    }
}
