package com.slate.matching;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.lang.reflect.Array;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slate.admin.AdminPermissionCatalog;
import com.slate.admin.AdminPermissionService;
import com.slate.common.SlateException;
import com.slate.matching.ScorePolicyAdminController.PolicyItemRequest;
import com.slate.matching.ScorePolicyAdminController.PolicyPreviewRequest;
import com.slate.matching.ScorePolicyAdminController.PolicyRollbackRequest;
import com.slate.matching.ScorePolicyAdminController.PolicyUpdateRequest;
import com.slate.operations.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ScorePolicyAdminService {

    private static final Map<String, List<String>> EXPECTED_ELEMENTS = Map.of(
            "FINAL_RATIO", List.of("first_filter", "internal"),
            "FIRST_FILTER", List.of("role", "region_distance", "join_time", "collaboration_condition", "genre", "experience"),
            "INTERNAL", List.of("collaboration_status", "travel_range", "duration_fit")
    );

    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("FINAL_RATIO:first_filter", "1차 필터 반영 비율"),
            Map.entry("FINAL_RATIO:internal", "내부 점수 반영 비율"),
            Map.entry("FIRST_FILTER:role", "역할"),
            Map.entry("FIRST_FILTER:region_distance", "지역/거리"),
            Map.entry("FIRST_FILTER:join_time", "합류 가능 시점"),
            Map.entry("FIRST_FILTER:collaboration_condition", "협업 조건"),
            Map.entry("FIRST_FILTER:genre", "장르"),
            Map.entry("FIRST_FILTER:experience", "경력"),
            Map.entry("INTERNAL:collaboration_status", "협업 가능 상태"),
            Map.entry("INTERNAL:travel_range", "이동 가능 범위"),
            Map.entry("INTERNAL:duration_fit", "작업 기간 적합도")
    );

    private final ScorePolicyAdminMapper scorePolicyAdminMapper;
    private final MatchingService matchingService;
    private final AdminPermissionService adminPermissionService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public ScorePolicyAdminService(
            ScorePolicyAdminMapper scorePolicyAdminMapper,
            MatchingService matchingService,
            AdminPermissionService adminPermissionService,
            AuditLogService auditLogService,
            ObjectMapper objectMapper
    ) {
        this.scorePolicyAdminMapper = scorePolicyAdminMapper;
        this.matchingService = matchingService;
        this.adminPermissionService = adminPermissionService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> activePolicy(Long adminUserId) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.SCORE_POLICY);
        Map<String, Object> policy = scorePolicyAdminMapper.selectActivePolicy();
        if (policy == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "활성 점수 정책이 없습니다.");
        }
        return policyEnvelope(policy);
    }

    public List<Map<String, Object>> history(Long adminUserId, Integer limit) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.SCORE_POLICY);
        return scorePolicyAdminMapper.selectPolicyHistory(Math.max(1, Math.min(limit == null ? 20 : limit, 50)));
    }

    @Transactional
    public Map<String, Object> publish(Long adminUserId, PolicyUpdateRequest request) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.SCORE_POLICY);
        Map<String, Object> current = scorePolicyAdminMapper.selectActivePolicy();
        if (current == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "활성 점수 정책이 없습니다.");
        }
        Map<String, Object> before = policyEnvelope(current);
        List<Map<String, Object>> items = sanitizeItems(request.items());
        int nextVersion = number(before.get("version")).intValue() + 1;

        scorePolicyAdminMapper.archiveActivePolicies(adminUserId);
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("policyName", request.policyName().trim());
        policy.put("status", "ACTIVE");
        policy.put("version", nextVersion);
        policy.put("description", textOrNull(request.description()));
        policy.put("createdBy", adminUserId);
        policy.put("updatedBy", adminUserId);
        scorePolicyAdminMapper.insertPolicy(policy);
        Long policyId = number(policy.get("policyId")).longValue();

        for (Map<String, Object> item : items) {
            item.put("policyId", policyId);
            scorePolicyAdminMapper.insertPolicyItem(item);
        }

        Map<String, Object> after = policyEnvelope(scorePolicyAdminMapper.selectPolicyById(policyId));
        String reason = request.changeReason().trim();
        scorePolicyAdminMapper.insertPolicyHistory(policyId, adminUserId, toJson(before), toJson(after), reason);
        auditLogService.recordAudit(adminUserId, "MATCHING_SCORE_POLICY_PUBLISHED", "MATCHING_SCORE_POLICY", policyId, before, after);
        auditLogService.recordOperation(
                "INFO",
                "SCORE_POLICY_PUBLISHED",
                "매칭 점수 정책 새 버전이 발행되었습니다.",
                Map.of("policyId", policyId, "version", nextVersion, "reason", reason)
        );
        return after;
    }

    public Map<String, Object> preview(Long adminUserId, PolicyPreviewRequest request) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.SCORE_POLICY);
        List<Map<String, Object>> items = sanitizeItems(request.items());
        Map<String, Object> result = new LinkedHashMap<>(matchingService.previewPolicyImpact(items, safeLimit(request.limit(), 10, 30)));
        result.put("valid", true);
        return result;
    }

    @Transactional
    public Map<String, Object> rollback(Long adminUserId, Long sourcePolicyId, PolicyRollbackRequest request) {
        adminPermissionService.require(adminUserId, AdminPermissionCatalog.SCORE_POLICY);
        Map<String, Object> current = scorePolicyAdminMapper.selectActivePolicy();
        if (current == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "활성 점수 정책이 없습니다.");
        }
        Map<String, Object> source = scorePolicyAdminMapper.selectPolicyById(sourcePolicyId);
        if (source == null) {
            throw new SlateException(HttpStatus.NOT_FOUND, "롤백할 점수 정책을 찾을 수 없습니다.");
        }
        if (Objects.equals(number(current.get("policyId")).longValue(), sourcePolicyId)) {
            throw new SlateException("이미 활성화된 정책으로는 롤백할 수 없습니다.");
        }

        Map<String, Object> before = policyEnvelope(current);
        List<Map<String, Object>> items = sanitizeExistingItems(scorePolicyAdminMapper.selectPolicyItems(sourcePolicyId));
        int nextVersion = number(before.get("version")).intValue() + 1;
        String sourceName = textOrDefault((String) source.get("policyName"), "정책 #" + sourcePolicyId);
        String reason = textOrDefault(request.reason(), sourceName + " 기준 롤백");

        scorePolicyAdminMapper.archiveActivePolicies(adminUserId);
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("policyName", truncate(textOrDefault(request.policyName(), "Rollback: " + sourceName), 100));
        policy.put("status", "ACTIVE");
        policy.put("version", nextVersion);
        policy.put("description", truncate("Rolled back from v" + source.get("version") + " / policy #" + sourcePolicyId, 255));
        policy.put("createdBy", adminUserId);
        policy.put("updatedBy", adminUserId);
        scorePolicyAdminMapper.insertPolicy(policy);
        Long policyId = number(policy.get("policyId")).longValue();

        for (Map<String, Object> item : items) {
            item.put("policyId", policyId);
            scorePolicyAdminMapper.insertPolicyItem(item);
        }

        Map<String, Object> after = policyEnvelope(scorePolicyAdminMapper.selectPolicyById(policyId));
        after.put("rolledBackFromPolicyId", sourcePolicyId);
        after.put("rolledBackFromVersion", source.get("version"));
        scorePolicyAdminMapper.insertPolicyHistory(policyId, adminUserId, toJson(before), toJson(after), reason);
        auditLogService.recordAudit(adminUserId, "MATCHING_SCORE_POLICY_ROLLED_BACK", "MATCHING_SCORE_POLICY", policyId, before, after);
        Map<String, Object> operationPayload = new LinkedHashMap<>();
        operationPayload.put("policyId", policyId);
        operationPayload.put("version", nextVersion);
        operationPayload.put("sourcePolicyId", sourcePolicyId);
        operationPayload.put("sourceVersion", source.get("version"));
        operationPayload.put("reason", reason);
        auditLogService.recordOperation(
                "INFO",
                "SCORE_POLICY_ROLLED_BACK",
                "이전 매칭 점수 정책이 새 ACTIVE 버전으로 복제 발행되었습니다.",
                operationPayload
        );
        return after;
    }

    private Map<String, Object> policyEnvelope(Map<String, Object> policy) {
        Long policyId = number(policy.get("policyId")).longValue();
        List<Map<String, Object>> items = scorePolicyAdminMapper.selectPolicyItems(policyId);
        Map<String, Object> result = new LinkedHashMap<>(policy);
        result.put("items", items);
        result.put("finalRatio", weights(items, "FINAL_RATIO"));
        result.put("firstFilterWeights", weights(items, "FIRST_FILTER"));
        result.put("internalWeights", weights(items, "INTERNAL"));
        return result;
    }

    private List<Map<String, Object>> sanitizeItems(List<PolicyItemRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new SlateException("점수 정책 항목이 필요합니다.");
        }
        Map<String, PolicyItemRequest> byKey = new LinkedHashMap<>();
        for (PolicyItemRequest request : requests) {
            String group = textOrDefault(request.scoreGroup(), "").toUpperCase();
            String element = textOrDefault(request.elementCode(), "").toLowerCase();
            if (!EXPECTED_ELEMENTS.containsKey(group) || !EXPECTED_ELEMENTS.get(group).contains(element)) {
                throw new SlateException("지원하지 않는 점수 항목입니다: " + group + "/" + element);
            }
            String key = group + ":" + element;
            if (byKey.put(key, request) != null) {
                throw new SlateException("중복 점수 항목입니다: " + key);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> group : EXPECTED_ELEMENTS.entrySet()) {
            Set<String> missing = new LinkedHashSet<>(group.getValue());
            BigDecimal sum = BigDecimal.ZERO;
            int order = 1;
            for (String element : group.getValue()) {
                PolicyItemRequest request = byKey.get(group.getKey() + ":" + element);
                if (request == null) {
                    continue;
                }
                missing.remove(element);
                BigDecimal weight = normalizeWeight(request.weight(), group.getKey(), element);
                sum = sum.add(weight);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("scoreGroup", group.getKey());
                item.put("elementCode", element);
                item.put("displayName", textOrDefault(request.displayName(), LABELS.get(group.getKey() + ":" + element)));
                item.put("weight", weight);
                item.put("sortOrder", order++);
                result.add(item);
            }
            if (!missing.isEmpty()) {
                throw new SlateException(group.getKey() + " 항목이 부족합니다: " + missing);
            }
            assertGroupSum(group.getKey(), sum);
        }
        return result;
    }

    private List<Map<String, Object>> sanitizeExistingItems(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            throw new SlateException("롤백 대상 정책의 점수 항목이 없습니다.");
        }
        List<PolicyItemRequest> requests = items.stream()
                .map(item -> new PolicyItemRequest(
                        Objects.toString(item.get("scoreGroup"), ""),
                        Objects.toString(item.get("elementCode"), ""),
                        Objects.toString(item.get("displayName"), null),
                        new BigDecimal(Objects.toString(item.get("weight"), "0"))
                ))
                .toList();
        return sanitizeItems(requests);
    }

    private void assertGroupSum(String scoreGroup, BigDecimal sum) {
        BigDecimal difference = sum.subtract(BigDecimal.valueOf(100)).abs();
        if (difference.compareTo(BigDecimal.valueOf(0.01)) > 0) {
            throw new SlateException(scoreGroup + " 가중치 합계는 100이어야 합니다. 현재: " + sum);
        }
    }

    private BigDecimal normalizeWeight(BigDecimal weight, String group, String element) {
        if (weight == null) {
            throw new SlateException("가중치가 필요합니다: " + group + "/" + element);
        }
        if (weight.compareTo(BigDecimal.ZERO) < 0 || weight.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new SlateException("가중치는 0 이상 100 이하만 가능합니다.");
        }
        return weight.setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Double> weights(List<Map<String, Object>> items, String group) {
        Map<String, Double> result = new LinkedHashMap<>();
        for (Map<String, Object> item : items) {
            if (group.equals(item.get("scoreGroup"))) {
                result.put(String.valueOf(item.get("elementCode")), number(item.get("weight")).doubleValue());
            }
        }
        return result;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(jsonSafe(value));
        } catch (Exception ex) {
            throw new SlateException(HttpStatus.INTERNAL_SERVER_ERROR, "점수 정책 이력 JSON 변환 중 오류가 발생했습니다.");
        }
    }

    private Object jsonSafe(Object value) {
        if (value == null
                || value instanceof String
                || value instanceof Number
                || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        if (value instanceof TemporalAccessor || value instanceof Date) {
            return value.toString();
        }
        if (value instanceof byte[] bytes) {
            return Base64.getEncoder().encodeToString(bytes);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> result.put(String.valueOf(key), jsonSafe(item)));
            return result;
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> result = new ArrayList<>();
            iterable.forEach(item -> result.add(jsonSafe(item)));
            return result;
        }
        if (value.getClass().isArray()) {
            List<Object> result = new ArrayList<>();
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                result.add(jsonSafe(Array.get(value, index)));
            }
            return result;
        }
        return String.valueOf(value);
    }

    private Number number(Object value) {
        if (value instanceof Number number) {
            return number;
        }
        return new BigDecimal(Objects.toString(value, "0"));
    }

    private String textOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String textOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private int safeLimit(Integer limit, int fallback, int max) {
        return Math.max(1, Math.min(limit == null ? fallback : limit, max));
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
