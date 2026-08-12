package com.slate.security;

import java.util.List;
import java.util.Map;

import com.slate.common.SlateException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DemoAccessGateService {

    private final DemoAccessProperties properties;
    private final DemoAccessCodeMapper demoAccessCodeMapper;
    private final com.slate.operations.AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    public DemoAccessGateService(
            DemoAccessProperties properties,
            DemoAccessCodeMapper demoAccessCodeMapper,
            com.slate.operations.AuditLogService auditLogService,
            PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.demoAccessCodeMapper = demoAccessCodeMapper;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean requiresDemoCode(HttpServletRequest request) {
        if (!properties.required() || HttpMethod.OPTIONS.matches(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        boolean verificationRequest = HttpMethod.POST.matches(request.getMethod())
                && path.equals("/api/demo/access");
        boolean publicImageRequest = HttpMethod.GET.matches(request.getMethod())
                && path.startsWith("/api/media/images/");
        return path.startsWith("/api/") && !verificationRequest && !publicImageRequest;
    }

    public boolean allowsRequest(String candidateCode) {
        if (!properties.required()) {
            return true;
        }
        String code = cleanCode(candidateCode);
        if (!StringUtils.hasText(code)) {
            return false;
        }
        if (properties.matches(code)) {
            return true;
        }
        return matchingDatabaseCandidateId(code, false) != null;
    }

    @Transactional
    public Map<String, Object> verify(String headerCode, String bodyCode) {
        if (!properties.required()) {
            return Map.of("enabled", false, "accepted", true);
        }
        String code = StringUtils.hasText(headerCode) ? headerCode : bodyCode;
        code = cleanCode(code);
        if (!StringUtils.hasText(code)) {
            throw invalidCode();
        }
        if (properties.matches(code)) {
            return Map.of("enabled", true, "accepted", true);
        }

        Long codeId = matchingDatabaseCandidateId(code, true);
        if (codeId != null && demoAccessCodeMapper.incrementUse(codeId) == 1) {
            return Map.of("enabled", true, "accepted", true);
        }
        throw invalidCode();
    }

    private Long matchingDatabaseCandidateId(String code, boolean forVerification) {
        try {
            List<Map<String, Object>> candidates = forVerification
                    ? demoAccessCodeMapper.selectVerificationCandidates(fingerprint(code))
                    : demoAccessCodeMapper.selectRequestCandidates(fingerprint(code));
            return matchingCandidateId(code, candidates);
        } catch (BadSqlGrammarException exception) {
            if (isMissingDemoAccessCodeTable(exception)) {
                return null;
            }
            throw exception;
        }
    }

    Long matchingCandidateId(String code, List<Map<String, Object>> candidates) {
        if (!StringUtils.hasText(code) || candidates == null) {
            return null;
        }
        for (Map<String, Object> candidate : candidates) {
            String hash = text(candidate.get("codeHash"));
            if (StringUtils.hasText(hash) && passwordEncoder.matches(code, hash)) {
                return longValue(candidate.get("codeId"));
            }
        }
        return null;
    }

    private String fingerprint(String code) {
        return auditLogService.fingerprint(code);
    }

    private SlateException invalidCode() {
        return new SlateException(HttpStatus.FORBIDDEN, "접속 코드가 올바르지 않습니다.");
    }

    private boolean isMissingDemoAccessCodeTable(BadSqlGrammarException exception) {
        Throwable cause = exception.getMostSpecificCause();
        String message = cause == null ? exception.getMessage() : cause.getMessage();
        return message != null && message.contains("demo_access_code");
    }

    private String cleanCode(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
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
}
