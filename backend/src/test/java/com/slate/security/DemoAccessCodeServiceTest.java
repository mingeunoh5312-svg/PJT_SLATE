package com.slate.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.slate.admin.AdminPermissionCatalog;
import com.slate.admin.AdminPermissionService;
import com.slate.operations.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

class DemoAccessCodeServiceTest {

    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Test
    void createRequiresPermissionStoresOnlyHashAndReturnsPlainCodeOnce() {
        Fixture fixture = fixture();
        DemoAccessCodeService.DemoAccessCodeCreateRequest request = new DemoAccessCodeService.DemoAccessCodeCreateRequest(
                "시연 초대",
                LocalDateTime.now().plusMinutes(10),
                LocalDateTime.now().plusDays(3),
                3
        );

        Map<String, Object> created = fixture.service.create(99L, request);
        String plainCode = String.valueOf(created.get("plainCode"));

        assertThat(plainCode).isNotBlank();
        assertThat(created).doesNotContainKeys("codeHash", "codeFingerprint");
        assertThat(fixture.mapper.internalRow(1L).get("codeHash")).isNotEqualTo(plainCode);
        assertThat(fixture.mapper.internalRow(1L).get("codeHash")).asString().startsWith("{");

        List<Map<String, Object>> list = fixture.service.codes(99L);
        assertThat(list).hasSize(1);
        assertThat(list.get(0)).doesNotContainKeys("plainCode", "codeHash", "codeFingerprint");

        verify(fixture.adminPermissionService, atLeastOnce()).require(99L, AdminPermissionCatalog.DEMO_ACCESS_MANAGE);
        verify(fixture.auditLogService).recordAudit(
                eq(99L),
                eq("DEMO_ACCESS_CODE_CREATED"),
                eq("DEMO_ACCESS_CODE"),
                eq(1L),
                eq(null),
                argThat(value -> value != null && !String.valueOf(value).contains(plainCode))
        );
    }

    @Test
    void updateDoesNotChangeStoredHashOrPlainCode() {
        Fixture fixture = fixture();
        Map<String, Object> created = fixture.service.create(99L, new DemoAccessCodeService.DemoAccessCodeCreateRequest(
                "초기",
                null,
                LocalDateTime.now().plusDays(2),
                null
        ));
        Object hashBefore = fixture.mapper.internalRow(1L).get("codeHash");

        Map<String, Object> updated = fixture.service.update(99L, 1L, new DemoAccessCodeService.DemoAccessCodeUpdateRequest(
                "수정된 이름",
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusDays(5),
                5
        ));

        assertThat(updated.get("label")).isEqualTo("수정된 이름");
        assertThat(updated).doesNotContainKeys("plainCode", "codeHash", "codeFingerprint");
        assertThat(fixture.mapper.internalRow(1L).get("codeHash")).isEqualTo(hashBefore);
        assertThat(String.valueOf(updated)).doesNotContain(String.valueOf(created.get("plainCode")));
    }

    @Test
    void revokeRecordsRevokerReasonAndEffectiveStatus() {
        Fixture fixture = fixture();
        fixture.service.create(99L, new DemoAccessCodeService.DemoAccessCodeCreateRequest(
                "폐기 대상",
                null,
                LocalDateTime.now().plusDays(2),
                null
        ));

        Map<String, Object> revoked = fixture.service.revoke(99L, 1L, new DemoAccessCodeService.DemoAccessCodeRevokeRequest("점검 종료"));

        assertThat(revoked.get("status")).isEqualTo("REVOKED");
        assertThat(revoked.get("effectiveStatus")).isEqualTo("REVOKED");
        assertThat(revoked.get("revokedBy")).isEqualTo(99L);
        assertThat(revoked.get("revokeReason")).isEqualTo("점검 종료");
        verify(fixture.auditLogService).recordAudit(
                eq(99L),
                eq("DEMO_ACCESS_CODE_REVOKED"),
                eq("DEMO_ACCESS_CODE"),
                eq(1L),
                argThat(value -> value != null && !String.valueOf(value).contains("codeHash")),
                argThat(value -> value != null && !String.valueOf(value).contains("codeHash"))
        );
    }

    @Test
    void effectiveStatusReflectsScheduleExpiryAndUsageLimit() {
        Fixture fixture = fixture();
        LocalDateTime now = LocalDateTime.of(2026, 6, 24, 12, 0);

        assertThat(fixture.service.effectiveStatus(row("ACTIVE", now.plusMinutes(1), now.plusDays(1), null, 0), now)).isEqualTo("SCHEDULED");
        assertThat(fixture.service.effectiveStatus(row("ACTIVE", null, now.minusMinutes(1), null, 0), now)).isEqualTo("EXPIRED");
        assertThat(fixture.service.effectiveStatus(row("ACTIVE", null, now.plusDays(1), 1, 1), now)).isEqualTo("EXHAUSTED");
        assertThat(fixture.service.effectiveStatus(row("REVOKED", null, now.plusDays(1), null, 0), now)).isEqualTo("REVOKED");
        assertThat(fixture.service.effectiveStatus(row("ACTIVE", null, now.plusDays(1), null, 0), now)).isEqualTo("ACTIVE");
    }

    private Map<String, Object> row(String status, LocalDateTime startsAt, LocalDateTime expiresAt, Integer maxUses, int usedCount) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("status", status);
        row.put("startsAt", startsAt);
        row.put("expiresAt", expiresAt);
        row.put("maxUses", maxUses);
        row.put("usedCount", usedCount);
        return row;
    }

    private Fixture fixture() {
        FakeDemoAccessCodeMapper mapper = new FakeDemoAccessCodeMapper();
        AdminPermissionService adminPermissionService = mock(AdminPermissionService.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        when(auditLogService.fingerprint(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> "fp:" + invocation.getArgument(0, String.class).trim());
        DemoAccessCodeService service = new DemoAccessCodeService(mapper, adminPermissionService, auditLogService, passwordEncoder);
        return new Fixture(service, mapper, adminPermissionService, auditLogService);
    }

    private record Fixture(
            DemoAccessCodeService service,
            FakeDemoAccessCodeMapper mapper,
            AdminPermissionService adminPermissionService,
            AuditLogService auditLogService
    ) {
    }

    private static final class FakeDemoAccessCodeMapper implements DemoAccessCodeMapper {
        private final List<Map<String, Object>> rows = new ArrayList<>();
        private long nextId = 1L;

        private Map<String, Object> internalRow(Long codeId) {
            return rows.stream()
                    .filter(row -> codeId.equals(row.get("codeId")))
                    .findFirst()
                    .orElseThrow();
        }

        @Override
        public List<Map<String, Object>> selectAdminCodes() {
            return rows.stream()
                    .map(row -> new LinkedHashMap<String, Object>(row))
                    .map(row -> (Map<String, Object>) row)
                    .toList();
        }

        @Override
        public Map<String, Object> selectAdminCodeById(Long codeId) {
            return rows.stream()
                    .filter(row -> codeId.equals(row.get("codeId")))
                    .findFirst()
                    .map(LinkedHashMap::new)
                    .orElse(null);
        }

        @Override
        public int insertCode(Map<String, Object> code) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("codeId", nextId++);
            row.put("label", code.get("label"));
            row.put("codeHash", code.get("codeHash"));
            row.put("codeFingerprint", code.get("codeFingerprint"));
            row.put("status", "ACTIVE");
            row.put("startsAt", code.get("startsAt"));
            row.put("expiresAt", code.get("expiresAt"));
            row.put("maxUses", code.get("maxUses"));
            row.put("usedCount", 0);
            row.put("createdBy", code.get("createdBy"));
            row.put("createdAt", LocalDateTime.now());
            rows.add(row);
            code.put("codeId", row.get("codeId"));
            return 1;
        }

        @Override
        public int updateCode(Map<String, Object> code) {
            Map<String, Object> row = internalRow(((Number) code.get("codeId")).longValue());
            row.put("label", code.get("label"));
            row.put("startsAt", code.get("startsAt"));
            row.put("expiresAt", code.get("expiresAt"));
            row.put("maxUses", code.get("maxUses"));
            row.put("updatedBy", code.get("updatedBy"));
            row.put("updatedAt", LocalDateTime.now());
            return 1;
        }

        @Override
        public int revokeCode(Long codeId, Long revokedBy, String reason) {
            Map<String, Object> row = internalRow(codeId);
            row.put("status", "REVOKED");
            row.put("revokedBy", revokedBy);
            row.put("revokedAt", LocalDateTime.now());
            row.put("revokeReason", reason);
            return 1;
        }

        @Override
        public List<Map<String, Object>> selectVerificationCandidates(String fingerprint) {
            return List.of();
        }

        @Override
        public List<Map<String, Object>> selectRequestCandidates(String fingerprint) {
            return List.of();
        }

        @Override
        public int incrementUse(Long codeId) {
            return 0;
        }
    }
}
