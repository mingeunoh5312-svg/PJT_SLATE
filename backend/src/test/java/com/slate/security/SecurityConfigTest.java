package com.slate.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class SecurityConfigTest {

    @Test
    void corsAllowsDemoAccessHeader() {
        SecurityConfig securityConfig = new SecurityConfig("http://localhost:5174");
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();

        CorsConfiguration configuration = source.getCorsConfiguration(
                new MockHttpServletRequest("OPTIONS", "/api/boards")
        );

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedHeaders()).contains(DemoAccessFilter.HEADER_NAME);
        assertThat(configuration.getAllowedMethods()).contains("OPTIONS");
    }
}
