package com.slate.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

class DemoAccessPropertiesTest {

    @Test
    void bindsConfiguredGateValues() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("slate.demo-access.enabled", "true")
                .withProperty("slate.demo-access.code", "configured-code");

        DemoAccessProperties properties = Binder.get(environment)
                .bind("slate.demo-access", DemoAccessProperties.class)
                .get();

        assertThat(properties.enabled()).isTrue();
        assertThat(properties.code()).isEqualTo("configured-code");
        assertThat(properties.required()).isTrue();
        assertThat(properties.fallbackConfigured()).isTrue();
    }

    @Test
    void requiresOnlyEnabledFlagAndTreatsEnvCodeAsFallback() {
        assertThat(new DemoAccessProperties(false, "configured-code").required()).isFalse();
        assertThat(new DemoAccessProperties(true, null).required()).isTrue();
        assertThat(new DemoAccessProperties(true, "").required()).isTrue();
        assertThat(new DemoAccessProperties(true, "  ").required()).isTrue();
        assertThat(new DemoAccessProperties(true, "configured-code").required()).isTrue();

        assertThat(new DemoAccessProperties(true, null).fallbackConfigured()).isFalse();
        assertThat(new DemoAccessProperties(true, " ").fallbackConfigured()).isFalse();
        assertThat(new DemoAccessProperties(true, "configured-code").fallbackConfigured()).isTrue();
    }

    @Test
    void matchesOnlyNonBlankConfiguredCodeAndTrimsOuterWhitespace() {
        DemoAccessProperties properties = new DemoAccessProperties(true, " configured-code ");

        assertThat(properties.matches(null)).isFalse();
        assertThat(properties.matches("")).isFalse();
        assertThat(properties.matches("  ")).isFalse();
        assertThat(properties.matches("wrong-code")).isFalse();
        assertThat(properties.matches("configured-code")).isTrue();
        assertThat(properties.matches(" configured-code ")).isTrue();
    }

    @Test
    void inactiveGateNeverMatchesCandidateCode() {
        assertThat(new DemoAccessProperties(false, "configured-code").matches("configured-code")).isFalse();
        assertThat(new DemoAccessProperties(true, " ").matches(" ")).isFalse();
    }
}
