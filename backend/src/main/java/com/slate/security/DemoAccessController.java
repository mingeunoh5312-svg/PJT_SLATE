package com.slate.security;

import com.slate.common.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
public class DemoAccessController {

    private final DemoAccessGateService gateService;

    public DemoAccessController(DemoAccessGateService gateService) {
        this.gateService = gateService;
    }

    @PostMapping("/access")
    public ApiResponse<java.util.Map<String, Object>> verify(
            @RequestHeader(value = DemoAccessFilter.HEADER_NAME, required = false) String headerCode,
            @RequestBody(required = false) DemoAccessRequest request
    ) {
        return ApiResponse.ok(gateService.verify(headerCode, request == null ? null : request.code()));
    }

    public record DemoAccessRequest(String code) {
    }
}
