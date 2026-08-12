package com.slate.operations;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

import com.slate.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class RequestLogContext {

    private final String ipHashSalt;

    public RequestLogContext(@Value("${slate.audit.ip-hash-salt:local-dev-audit-salt}") String ipHashSalt) {
        this.ipHashSalt = StringUtils.hasText(ipHashSalt) ? ipHashSalt : "local-dev-audit-salt";
    }

    public String clientIpHash() {
        return currentRequest()
                .map(this::clientIp)
                .filter(StringUtils::hasText)
                .map(this::hashValue)
                .orElse(null);
    }

    public Map<String, Object> operationContext(Object payload) {
        Map<String, Object> context = new LinkedHashMap<>();
        if (payload instanceof Map<?, ?> payloadMap) {
            payloadMap.forEach((key, value) -> context.put(String.valueOf(key), value));
        } else if (payload != null) {
            context.put("payload", payload);
        }
        Long actorUserId = currentActorUserId();
        if (actorUserId != null) {
            context.put("actorUserId", actorUserId);
        }
        Map<String, Object> request = requestMetadata();
        if (!request.isEmpty()) {
            context.put("request", request);
        }
        return context.isEmpty() ? null : context;
    }

    public String hashValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((ipHashSalt + "|" + value.trim()).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("로그 해시 생성에 실패했습니다.", ex);
        }
    }

    private Map<String, Object> requestMetadata() {
        return currentRequest()
                .map(request -> {
                    Map<String, Object> metadata = new LinkedHashMap<>();
                    metadata.put("method", request.getMethod());
                    metadata.put("path", request.getRequestURI());
                    String ipHash = clientIpHash();
                    if (ipHash != null) {
                        metadata.put("ipHash", ipHash);
                    }
                    String userAgent = request.getHeader("User-Agent");
                    if (StringUtils.hasText(userAgent)) {
                        metadata.put("userAgentHash", hashValue(userAgent));
                    }
                    return metadata;
                })
                .orElseGet(LinkedHashMap::new);
    }

    private Long currentActorUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser user)) {
            return null;
        }
        return user.userId();
    }

    private java.util.Optional<HttpServletRequest> currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return java.util.Optional.of(attributes.getRequest());
        }
        return java.util.Optional.empty();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = firstForwardedIp(request.getHeader("X-Forwarded-For"));
        if (StringUtils.hasText(forwarded)) {
            return forwarded;
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String firstForwardedIp(String forwardedFor) {
        if (!StringUtils.hasText(forwardedFor)) {
            return null;
        }
        for (String part : forwardedFor.split(",")) {
            if (StringUtils.hasText(part)) {
                return part.trim();
            }
        }
        return null;
    }
}
