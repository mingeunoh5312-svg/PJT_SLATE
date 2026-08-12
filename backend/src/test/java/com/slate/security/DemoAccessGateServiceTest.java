package com.slate.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLSyntaxErrorException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.slate.common.SlateException;
import com.slate.operations.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

class DemoAccessGateServiceTest {

    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Test
    void disabledGateAcceptsWithoutCodeAndDoesNotRequireFilterCheck() {
        Fixture fixture = fixture(false, null);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/references/genres");

        assertThat(fixture.service.requiresDemoCode(request)).isFalse();
        assertThat(fixture.service.allowsRequest(null)).isTrue();
        assertThat(fixture.service.verify(null, null)).isEqualTo(Map.of("enabled", false, "accepted", true));
    }

    @Test
    void enabledGateWithoutEnvOrDbCodeRejectsAnyCode() {
        Fixture fixture = fixture(true, " ");

        assertForbidden(() -> fixture.service.verify(null, "anything"));
        assertThat(fixture.service.allowsRequest("anything")).isFalse();
    }

    @Test
    void envFallbackCodePassesVerificationAndRequestValidation() {
        Fixture fixture = fixture(true, " fallback-code ");

        assertThat(fixture.service.verify(null, "fallback-code")).isEqualTo(Map.of("enabled", true, "accepted", true));
        assertThat(fixture.service.allowsRequest(" fallback-code ")).isTrue();
    }

    @Test
    void envFallbackWrongCodeIsRejected() {
        Fixture fixture = fixture(true, "fallback-code");

        assertForbidden(() -> fixture.service.verify(null, "wrong-code"));
        assertThat(fixture.service.allowsRequest("wrong-code")).isFalse();
    }

    @Test
    void missingDatabaseCodeTableRejectsNonFallbackCodeWithoutServerError() {
        DemoAccessCodeMapper mapper = mock(DemoAccessCodeMapper.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        when(auditLogService.fingerprint(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("fp:wrong-code");
        when(mapper.selectVerificationCandidates(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new BadSqlGrammarException(
                        "select",
                        "SELECT * FROM demo_access_code",
                        new SQLSyntaxErrorException("Table 'slate.demo_access_code' doesn't exist")
                ));
        DemoAccessGateService service = new DemoAccessGateService(
                new DemoAccessProperties(true, "fallback-code"),
                mapper,
                auditLogService,
                passwordEncoder
        );

        assertForbidden(() -> service.verify(null, "wrong-code"));
    }

    @Test
    void databaseCodePassesVerificationAndIncrementsUseOnce() {
        Fixture fixture = fixture(true, "");
        fixture.mapper.addActiveCode(10L, "db-code", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusDays(1), null, 0);

        assertThat(fixture.service.verify(null, "db-code")).isEqualTo(Map.of("enabled", true, "accepted", true));

        assertThat(fixture.mapper.row(10L).get("usedCount")).isEqualTo(1);
    }

    @Test
    void wrongDatabaseCodeIsRejected() {
        Fixture fixture = fixture(true, "");
        fixture.mapper.addActiveCode(10L, "db-code", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusDays(1), null, 0);

        assertForbidden(() -> fixture.service.verify(null, "wrong-code"));
    }

    @Test
    void futureExpiredRevokedAndExhaustedCodesRejectNewVerification() {
        Fixture fixture = fixture(true, "");
        fixture.mapper.addActiveCode(1L, "future", LocalDateTime.now().plusHours(1), LocalDateTime.now().plusDays(1), null, 0);
        fixture.mapper.addActiveCode(2L, "expired", LocalDateTime.now().minusDays(2), LocalDateTime.now().minusMinutes(1), null, 0);
        fixture.mapper.addRevokedCode(3L, "revoked", LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));
        fixture.mapper.addActiveCode(4L, "exhausted", LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1), 1, 1);

        assertForbidden(() -> fixture.service.verify(null, "future"));
        assertForbidden(() -> fixture.service.verify(null, "expired"));
        assertForbidden(() -> fixture.service.verify(null, "revoked"));
        assertForbidden(() -> fixture.service.verify(null, "exhausted"));
    }

    @Test
    void filterRequestValidationDoesNotIncrementUseAndAllowsAlreadyVerifiedExhaustedCode() {
        Fixture fixture = fixture(true, "");
        fixture.mapper.addActiveCode(4L, "exhausted", LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1), 1, 1);

        assertThat(fixture.service.allowsRequest("exhausted")).isTrue();
        assertThat(fixture.mapper.row(4L).get("usedCount")).isEqualTo(1);
    }

    @Test
    void postVerificationEndpointAndOptionsDoNotRequireFilterCheck() {
        Fixture fixture = fixture(true, "");
        HttpServletRequest verification = new MockHttpServletRequest("POST", "/api/demo/access");
        HttpServletRequest options = new MockHttpServletRequest("OPTIONS", "/api/references/genres");
        HttpServletRequest getVerification = new MockHttpServletRequest("GET", "/api/demo/access");

        assertThat(fixture.service.requiresDemoCode(verification)).isFalse();
        assertThat(fixture.service.requiresDemoCode(options)).isFalse();
        assertThat(fixture.service.requiresDemoCode(getVerification)).isTrue();
    }

    @Test
    void publicMediaImageGetDoesNotRequireDemoCode() {
        Fixture fixture = fixture(true, "");
        HttpServletRequest image = new MockHttpServletRequest("GET", "/api/media/images/team/8");

        assertThat(fixture.service.requiresDemoCode(image)).isFalse();
    }

    @Test
    void headerCandidateTakesPriorityOverBodyCandidate() {
        Fixture fixture = fixture(true, "fallback-code");

        assertForbidden(() -> fixture.service.verify("wrong-code", "fallback-code"));
    }

    private Fixture fixture(boolean enabled, String fallbackCode) {
        FakeDemoAccessCodeMapper mapper = new FakeDemoAccessCodeMapper(passwordEncoder);
        AuditLogService auditLogService = mock(AuditLogService.class);
        when(auditLogService.fingerprint(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> "fp:" + invocation.getArgument(0, String.class).trim());
        return new Fixture(
                new DemoAccessGateService(
                        new DemoAccessProperties(enabled, fallbackCode),
                        mapper,
                        auditLogService,
                        passwordEncoder
                ),
                mapper,
                auditLogService
        );
    }

    private void assertForbidden(Runnable request) {
        assertThatThrownBy(request::run)
                .isInstanceOfSatisfying(SlateException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception).hasMessage("접속 코드가 올바르지 않습니다.");
                });
    }

    private record Fixture(
            DemoAccessGateService service,
            FakeDemoAccessCodeMapper mapper,
            AuditLogService auditLogService
    ) {
    }

    private static final class FakeDemoAccessCodeMapper implements DemoAccessCodeMapper {
        private final PasswordEncoder passwordEncoder;
        private final List<Map<String, Object>> rows = new ArrayList<>();

        private FakeDemoAccessCodeMapper(PasswordEncoder passwordEncoder) {
            this.passwordEncoder = passwordEncoder;
        }

        private void addActiveCode(Long id, String plainCode, LocalDateTime startsAt, LocalDateTime expiresAt, Integer maxUses, int usedCount) {
            addCode(id, plainCode, "ACTIVE", startsAt, expiresAt, maxUses, usedCount);
        }

        private void addRevokedCode(Long id, String plainCode, LocalDateTime startsAt, LocalDateTime expiresAt) {
            addCode(id, plainCode, "REVOKED", startsAt, expiresAt, null, 0);
        }

        private void addCode(Long id, String plainCode, String status, LocalDateTime startsAt, LocalDateTime expiresAt, Integer maxUses, int usedCount) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("codeId", id);
            row.put("codeHash", passwordEncoder.encode(plainCode));
            row.put("codeFingerprint", "fp:" + plainCode.trim());
            row.put("status", status);
            row.put("startsAt", startsAt);
            row.put("expiresAt", expiresAt);
            row.put("maxUses", maxUses);
            row.put("usedCount", usedCount);
            rows.add(row);
        }

        private Map<String, Object> row(Long id) {
            return rows.stream()
                    .filter(row -> id.equals(((Number) row.get("codeId")).longValue()))
                    .findFirst()
                    .orElseThrow();
        }

        @Override
        public List<Map<String, Object>> selectVerificationCandidates(String fingerprint) {
            return validRows(fingerprint, true);
        }

        @Override
        public List<Map<String, Object>> selectRequestCandidates(String fingerprint) {
            return validRows(fingerprint, false);
        }

        private List<Map<String, Object>> validRows(String fingerprint, boolean enforceUsageLimit) {
            LocalDateTime now = LocalDateTime.now();
            return rows.stream()
                    .filter(row -> fingerprint.equals(row.get("codeFingerprint")))
                    .filter(row -> "ACTIVE".equals(row.get("status")))
                    .filter(row -> row.get("startsAt") == null || !((LocalDateTime) row.get("startsAt")).isAfter(now))
                    .filter(row -> ((LocalDateTime) row.get("expiresAt")).isAfter(now))
                    .filter(row -> !enforceUsageLimit || row.get("maxUses") == null
                            || ((Number) row.get("usedCount")).intValue() < ((Number) row.get("maxUses")).intValue())
                    .toList();
        }

        @Override
        public int incrementUse(Long codeId) {
            Map<String, Object> row = row(codeId);
            Integer maxUses = (Integer) row.get("maxUses");
            int usedCount = ((Number) row.get("usedCount")).intValue();
            if (maxUses != null && usedCount >= maxUses) {
                return 0;
            }
            row.put("usedCount", usedCount + 1);
            return 1;
        }

        @Override
        public List<Map<String, Object>> selectAdminCodes() {
            return List.of();
        }

        @Override
        public Map<String, Object> selectAdminCodeById(Long codeId) {
            return null;
        }

        @Override
        public int insertCode(Map<String, Object> code) {
            return 0;
        }

        @Override
        public int updateCode(Map<String, Object> code) {
            return 0;
        }

        @Override
        public int revokeCode(Long codeId, Long revokedBy, String reason) {
            return 0;
        }
    }
}
