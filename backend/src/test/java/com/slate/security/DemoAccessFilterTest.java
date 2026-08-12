package com.slate.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class DemoAccessFilterTest {

    @Test
    void disabledGateAllowsApiRequestWithoutCode() throws Exception {
        FilterResult result = filter(false, false, "GET", "/api/boards", null);

        assertThat(result.chainCalled()).isTrue();
        assertThat(result.response().getStatus()).isEqualTo(200);
    }

    @Test
    void enabledGateWithNoValidCodeRejectsApiRequest() throws Exception {
        FilterResult result = filter(true, false, "GET", "/api/boards", null);

        assertThat(result.chainCalled()).isFalse();
        assertThat(result.response().getStatus()).isEqualTo(403);
    }

    @Test
    void configuredGateRejectsMissingCodeWithJsonUtf8Response() throws Exception {
        FilterResult result = filter(true, false, "GET", "/api/boards", null);

        assertThat(result.chainCalled()).isFalse();
        assertThat(result.response().getStatus()).isEqualTo(403);
        assertThat(MediaType.APPLICATION_JSON.isCompatibleWith(
                MediaType.parseMediaType(result.response().getContentType())
        )).isTrue();
        assertThat(result.response().getCharacterEncoding()).isEqualTo(StandardCharsets.UTF_8.name());
        assertThat(result.response().getContentAsString(StandardCharsets.UTF_8))
                .isEqualTo("{\"success\":false,\"message\":\"Slate 접속 코드가 필요합니다.\"}");
    }

    @Test
    void configuredGateRejectsWrongCodeWithoutContinuingChain() throws Exception {
        FilterResult result = filter(
                true,
                false,
                "GET",
                "/api/references/genres",
                "wrong-code"
        );

        assertThat(result.chainCalled()).isFalse();
        assertThat(result.response().getStatus()).isEqualTo(403);
    }

    @Test
    void configuredGateAllowsCorrectCode() throws Exception {
        FilterResult result = filter(
                true,
                true,
                "POST",
                "/api/auth/login",
                "configured-code"
        );

        assertThat(result.chainCalled()).isTrue();
    }

    @Test
    void verificationEndpointBypassesFilter() throws Exception {
        FilterResult result = filter(
                false,
                false,
                "POST",
                "/api/demo/access",
                null
        );

        assertThat(result.chainCalled()).isTrue();
    }

    @Test
    void unsupportedVerificationMethodDoesNotBypassFilter() throws Exception {
        FilterResult result = filter(
                true,
                false,
                "GET",
                "/api/demo/access",
                null
        );

        assertThat(result.chainCalled()).isFalse();
        assertThat(result.response().getStatus()).isEqualTo(403);
    }

    @Test
    void similarlyNamedApiPathDoesNotBypassFilter() throws Exception {
        FilterResult result = filter(
                true,
                false,
                "POST",
                "/api/demo/access/extra",
                null
        );

        assertThat(result.chainCalled()).isFalse();
        assertThat(result.response().getStatus()).isEqualTo(403);
    }

    @Test
    void optionsRequestBypassesFilter() throws Exception {
        FilterResult result = filter(
                false,
                false,
                "OPTIONS",
                "/api/boards",
                null
        );

        assertThat(result.chainCalled()).isTrue();
    }

    @Test
    void nonApiRequestBypassesFilter() throws Exception {
        FilterResult result = filter(
                false,
                false,
                "GET",
                "/actuator/health",
                null
        );

        assertThat(result.chainCalled()).isTrue();
    }

    private FilterResult filter(
            boolean requiresDemoCode,
            boolean requestAllowed,
            String method,
            String path,
            String headerCode
    ) throws Exception {
        DemoAccessGateService gateService = mock(DemoAccessGateService.class);
        when(gateService.requiresDemoCode(any())).thenReturn(requiresDemoCode);
        when(gateService.allowsRequest(headerCode)).thenReturn(requestAllowed);
        DemoAccessFilter filter = new DemoAccessFilter(gateService);
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        if (headerCode != null) {
            request.addHeader(DemoAccessFilter.HEADER_NAME, headerCode);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (ignoredRequest, ignoredResponse) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        return new FilterResult(response, chainCalled.get());
    }

    private record FilterResult(MockHttpServletResponse response, boolean chainCalled) {
    }
}
