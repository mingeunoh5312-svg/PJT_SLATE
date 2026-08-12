package com.slate.references;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.slate.admin.AdminPermissionCatalog;
import com.slate.admin.AdminPermissionService;
import com.slate.common.SlateException;
import com.slate.operations.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminRegionService {

    private static final BigDecimal MIN_LAT = new BigDecimal("33.0");
    private static final BigDecimal MAX_LAT = new BigDecimal("39.5");
    private static final BigDecimal MIN_LNG = new BigDecimal("124.0");
    private static final BigDecimal MAX_LNG = new BigDecimal("132.5");

    private final AdminRegionMapper adminRegionMapper;
    private final AdminPermissionService adminPermissionService;
    private final AuditLogService auditLogService;

    public AdminRegionService(
            AdminRegionMapper adminRegionMapper,
            AdminPermissionService adminPermissionService,
            AuditLogService auditLogService
    ) {
        this.adminRegionMapper = adminRegionMapper;
        this.adminPermissionService = adminPermissionService;
        this.auditLogService = auditLogService;
    }

    public List<Map<String, Object>> regions(Long adminUserId, String keyword, String sidoName, String activeYn, Integer limit) {
        requireRegionManage(adminUserId);
        return adminRegionMapper.selectRegions(
                textOrNull(keyword),
                textOrNull(sidoName),
                normalizeActiveYnFilter(activeYn),
                safeLimit(limit)
        );
    }

    public Map<String, Object> summary(Long adminUserId) {
        requireRegionManage(adminUserId);
        return adminRegionMapper.selectSummary();
    }

    @Transactional
    public Map<String, Object> updateRegion(Long adminUserId, Long regionId, AdminRegionController.AdminRegionUpdateRequest request) {
        requireRegionManage(adminUserId);
        String reason = requireReason(request.reason());
        Map<String, Object> before = requireRegion(regionId);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("regionId", regionId);
        row.put("sidoName", requireText(request.sidoName(), "sidoName"));
        row.put("sigunguName", requireText(request.sigunguName(), "sigunguName"));
        row.put("dongName", optionalText(request.dongName()));
        row.put("centerLat", requireCoordinate(request.centerLat(), MIN_LAT, MAX_LAT, "centerLat"));
        row.put("centerLng", requireCoordinate(request.centerLng(), MIN_LNG, MAX_LNG, "centerLng"));
        row.put("publicDisplayName", requireText(request.publicDisplayName(), "publicDisplayName"));
        row.put("activeYn", normalizeActiveYn(request.activeYn()));
        if (adminRegionMapper.updateRegion(row) == 0) {
            throw new SlateException(HttpStatus.NOT_FOUND, "지역 정보를 찾을 수 없습니다.");
        }
        Map<String, Object> after = requireRegion(regionId);
        auditLogService.recordAudit(adminUserId, "REGION_UPDATED", "REGION", regionId, auditPayload(before, null), auditPayload(after, reason));
        auditLogService.recordOperation(
                "INFO",
                "REGION_UPDATED",
                "관리자가 지역 DB 정보를 수정했습니다.",
                Map.of("regionId", regionId, "adminUserId", adminUserId, "reason", reason)
        );
        return after;
    }

    private void requireRegionManage(Long adminUserId) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.REGION_MANAGE);
    }

    private Map<String, Object> requireRegion(Long regionId) {
        Map<String, Object> region = adminRegionMapper.selectRegionById(regionId);
        if (region == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "지역 정보를 찾을 수 없습니다.");
        }
        return region;
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new SlateException(fieldName + " 값은 필수입니다.");
        }
        return value.trim();
    }

    private String optionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String requireReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new SlateException("관리자 처리 사유는 필수입니다.");
        }
        return reason.trim();
    }

    private BigDecimal requireCoordinate(BigDecimal value, BigDecimal min, BigDecimal max, String fieldName) {
        if (value == null || value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new SlateException(fieldName + " 좌표가 대한민국 서비스 범위를 벗어났습니다.");
        }
        return value;
    }

    private String normalizeActiveYn(String value) {
        if (!StringUtils.hasText(value)) {
            return "Y";
        }
        String normalized = value.trim().toUpperCase();
        if (!List.of("Y", "N").contains(normalized)) {
            throw new SlateException("activeYn 값은 Y 또는 N이어야 합니다.");
        }
        return normalized;
    }

    private String normalizeActiveYnFilter(String value) {
        if (!StringUtils.hasText(value) || "ALL".equalsIgnoreCase(value.trim())) {
            return null;
        }
        return normalizeActiveYn(value);
    }

    private String textOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private int safeLimit(Integer limit) {
        return Math.max(1, Math.min(limit == null ? 300 : limit, 500));
    }

    private Map<String, Object> auditPayload(Map<String, Object> row, String reason) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : List.of(
                "regionId", "regionCode", "sidoName", "sigunguName", "dongName",
                "centerLat", "centerLng", "publicDisplayName", "activeYn",
                "profileCount", "teamCount"
        )) {
            if (row.containsKey(key)) {
                result.put(key, row.get(key));
            }
        }
        if (reason != null) {
            result.put("reason", reason);
        }
        return result;
    }
}
