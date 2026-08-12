package com.slate.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class DemoAccessFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Slate-Demo-Code";

    private final DemoAccessGateService gateService;

    public DemoAccessFilter(DemoAccessGateService gateService) {
        this.gateService = gateService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!gateService.requiresDemoCode(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (gateService.allowsRequest(request.getHeader(HEADER_NAME))) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"success\":false,\"message\":\"Slate 접속 코드가 필요합니다.\"}");
    }
}
