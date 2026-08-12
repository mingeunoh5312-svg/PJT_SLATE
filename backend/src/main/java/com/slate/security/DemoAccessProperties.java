package com.slate.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "slate.demo-access")
public record DemoAccessProperties(boolean enabled, String code) {

    public boolean required() {
        return enabled;
    }

    public boolean fallbackConfigured() {
        return StringUtils.hasText(code);
    }

    public boolean matches(String candidate) {
        return enabled && fallbackConfigured() && StringUtils.hasText(candidate) && code.trim().equals(candidate.trim());
    }
}
