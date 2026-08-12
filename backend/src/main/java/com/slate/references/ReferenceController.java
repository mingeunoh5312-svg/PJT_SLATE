package com.slate.references;

import java.util.List;
import java.util.Map;

import com.slate.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/references")
public class ReferenceController {

    private final ReferenceService referenceService;

    public ReferenceController(ReferenceService referenceService) {
        this.referenceService = referenceService;
    }

    @GetMapping("/codes")
    public ApiResponse<Map<String, List<Map<String, Object>>>> codes(@RequestParam(required = false) List<String> groups) {
        return ApiResponse.ok(referenceService.codes(groups));
    }

    @GetMapping("/regions")
    public ApiResponse<List<Map<String, Object>>> regions(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "20") Integer limit
    ) {
        return ApiResponse.ok(referenceService.regions(keyword, limit));
    }

    @GetMapping("/roles")
    public ApiResponse<List<Map<String, Object>>> roles() {
        return ApiResponse.ok(referenceService.roles());
    }

    @GetMapping("/genres")
    public ApiResponse<List<Map<String, Object>>> genres() {
        return ApiResponse.ok(referenceService.genres());
    }
}
