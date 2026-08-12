package com.slate.locations;

import java.math.BigDecimal;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slate.common.SlateException;
import com.slate.matching.OpenAiClient;
import com.slate.matching.OpenAiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiLocationRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(AiLocationRecommendationService.class);

    private static final String DEFAULT_SUMMARY = "요청한 장면 조건과 실제 DB 후보 정보를 기준으로 추천 후보에 포함되었습니다.";
    private static final String DEFAULT_REASON = "지역, 장소명, 주소, 촬영 이력 정보를 기준으로 검토 가능한 후보입니다.";
    private static final String DEFAULT_USAGE = "현장 답사 후 동선, 조명, 소음 조건을 확인해 장면 구성에 활용할 수 있습니다.";
    private static final String DEFAULT_BASIS = "DB에 저장된 장소 정보와 촬영 이력, 요청 키워드의 기본 점수를 기준으로 후보화되었습니다.";
    private static final List<String> DEFAULT_CHECK_POINTS = List.of(
            "촬영 허가 가능 여부 확인",
            "현장 안전 동선 확인",
            "소음과 통행 민원 가능성 확인"
    );
    private static final String SYSTEM_PROMPT = """
            당신은 영화 제작 플랫폼의 로케이션 추천 AI입니다.

            아래 후보 목록 안에서만 추천하세요.
            후보 목록에 없는 장소, 주소, 좌표, locationId를 만들지 마세요.
            missingRequiredSceneTags가 비어 있지 않은 후보는 추천하지 마세요.
            matchedRequiredSceneTags와 matchedOptionalSceneTags를 우선해 장면 적합도를 판단하세요.
            dataWarnings가 있는 후보는 추천 사유에서 현재 상태 확인이 필요하다고 분명히 표시하세요.
            실제 허가 가능 여부, 비용, 주차, 소음, 운영 상태를 확정적으로 말하지 마세요.
            알 수 없는 내용은 현장 체크 포인트로 변환하세요.
            응답은 JSON으로만 작성하세요.

            각 추천 항목은 locationId, score, aiSummary, matchReason, usageIdea, recommendationBasis, checkPoints를 포함해야 합니다.
            checkPoints는 문자열 배열이고, 사용자에게 보이는 한국어 문장으로 작성하세요.
            """;

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    public AiLocationRecommendationService(OpenAiClient openAiClient, ObjectMapper objectMapper) {
        this.openAiClient = openAiClient;
        this.objectMapper = objectMapper;
    }

    public RecommendationOutcome recommend(String prompt, String contextType, int limit, List<Map<String, Object>> candidates) {
        if (candidates.isEmpty()) {
            return new RecommendationOutcome(false, null, List.of());
        }
        OpenAiResult openAiResult = callOpenAi(prompt, contextType, limit, candidates);
        if (openAiResult.response() == null) {
            return fallback(limit, candidates, openAiResult.failureReason());
        }
        List<RecommendationItem> parsed = parseRecommendations(limit, candidates, openAiResult.response());
        if (parsed.isEmpty()) {
            return fallback(limit, candidates, "OpenAI 응답에서 사용할 수 있는 후보를 찾지 못했습니다.");
        }
        return new RecommendationOutcome(false, null, parsed);
    }

    private OpenAiResult callOpenAi(String prompt, String contextType, int limit, List<Map<String, Object>> candidates) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", openAiClient.model());
            payload.put("temperature", 0.2);
            payload.put("response_format", Map.of("type", "json_object"));
            payload.put("messages", List.of(
                    Map.of("role", "system", "content", SYSTEM_PROMPT),
                    Map.of("role", "user", "content", userPrompt(prompt, contextType, limit, candidates))
            ));
            return new OpenAiResult(openAiClient.postJson("/chat/completions", payload), null);
        } catch (SlateException ex) {
            if (OpenAiProperties.MISSING_API_KEY_MESSAGE.equals(ex.getMessage())) {
                log.info("AI location OpenAI key is not configured. Falling back to score-based recommendations.");
                return new OpenAiResult(null, "OpenAI API key is not configured.");
            }
            log.warn("AI location OpenAI call failed with SlateException. message={}", ex.getMessage());
            return new OpenAiResult(null, ex.getMessage());
        } catch (Exception ex) {
            log.warn("AI location OpenAI call failed. reason={}", ex.toString());
            return new OpenAiResult(null, ex.toString());
        }
    }

    private String userPrompt(String prompt, String contextType, int limit, List<Map<String, Object>> candidates) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("requestPrompt", prompt);
        body.put("contextType", contextType);
        body.put("recommendationLimit", limit);
        body.put("hardConstraints", List.of(
                "candidates 배열 안의 locationId만 추천합니다.",
                "후보에 없는 장소명, 주소, 좌표를 새로 만들지 않습니다.",
                "missingRequiredSceneTags가 하나라도 있는 후보는 추천하지 않습니다.",
                "장소 유형, 시간대, 분위기 태그가 맞는 후보를 촬영 이력 수보다 우선합니다.",
                "dataWarnings는 추천 제외 또는 현장 확인 체크 포인트로 반영합니다.",
                "허가, 비용, 운영 상태처럼 DB에 없는 사실은 확정하지 않고 체크 포인트로 작성합니다."
        ));
        body.put("candidates", candidates.stream().map(this::candidatePayload).toList());
        body.put("responseFormat", Map.of(
                "recommendations", List.of(Map.of(
                        "locationId", 1,
                        "score", 86.5,
                        "aiSummary", "추천 요약",
                        "matchReason", "추천 사유",
                        "usageIdea", "활용 아이디어",
                        "recommendationBasis", "추천 근거",
                        "checkPoints", List.of("현장 확인 사항")
                ))
        ));
        return "아래 JSON 데이터의 candidates 안에서만 추천하세요.\n" + objectMapper.writeValueAsString(jsonSafe(body));
    }

    private Map<String, Object> candidatePayload(Map<String, Object> candidate) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("locationId", candidate.get("locationId"));
        payload.put("placeName", candidate.get("placeName"));
        payload.put("sido", candidate.get("sido"));
        payload.put("sigungu", candidate.get("sigungu"));
        payload.put("lotAddress", candidate.get("lotAddress"));
        payload.put("roadAddress", candidate.get("roadAddress"));
        payload.put("latitude", candidate.get("latitude"));
        payload.put("longitude", candidate.get("longitude"));
        payload.put("historyCount", candidate.get("historyCount"));
        payload.put("representativeHistories", candidate.get("representativeHistories"));
        payload.put("baseScore", candidate.get("baseScore"));
        payload.put("matchedKeywords", candidate.get("matchedKeywords"));
        payload.put("matchedRequiredSceneTags", candidate.get("matchedRequiredSceneTags"));
        payload.put("matchedOptionalSceneTags", candidate.get("matchedOptionalSceneTags"));
        payload.put("missingRequiredSceneTags", candidate.get("missingRequiredSceneTags"));
        payload.put("dataWarnings", candidate.get("dataWarnings"));
        return payload;
    }

    private List<RecommendationItem> parseRecommendations(int limit, List<Map<String, Object>> candidates, JsonNode openAiResponse) {
        try {
            JsonNode body = recommendationBody(openAiResponse);
            JsonNode items = body.path("recommendations");
            if (!items.isArray()) {
                return List.of();
            }
            Map<Long, Map<String, Object>> candidateById = new LinkedHashMap<>();
            for (Map<String, Object> candidate : candidates) {
                Long locationId = longValue(candidate.get("locationId"));
                if (locationId != null) {
                    candidateById.putIfAbsent(locationId, candidate);
                }
            }
            Set<Long> selectedIds = new LinkedHashSet<>();
            List<RecommendationItem> result = new ArrayList<>();
            for (JsonNode item : items) {
                Long locationId = longValue(item.path("locationId"));
                if (locationId == null || !selectedIds.add(locationId)) {
                    continue;
                }
                Map<String, Object> candidate = candidateById.get(locationId);
                if (candidate == null) {
                    continue;
                }
                result.add(new RecommendationItem(
                        locationId,
                        scoreOrDefault(item.path("score"), candidate),
                        textOrDefault(item.path("aiSummary"), DEFAULT_SUMMARY, 480),
                        textOrDefault(item.path("matchReason"), DEFAULT_REASON, 950),
                        textOrDefault(item.path("usageIdea"), DEFAULT_USAGE, 950),
                        textOrDefault(item.path("recommendationBasis"), DEFAULT_BASIS, 950),
                        checkPoints(item.path("checkPoints")),
                        false,
                        openAiClient.model()
                ));
                if (result.size() >= limit) {
                    break;
                }
            }
            return result;
        } catch (Exception ex) {
            log.warn("AI location response parsing failed. reason={}", ex.toString());
            return List.of();
        }
    }

    private JsonNode recommendationBody(JsonNode openAiResponse) throws Exception {
        if (openAiResponse.has("recommendations")) {
            return openAiResponse;
        }
        String content = text(openAiResponse.path("choices").path(0).path("message").path("content"));
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("OpenAI content is empty.");
        }
        return objectMapper.readTree(content);
    }

    private RecommendationOutcome fallback(int limit, List<Map<String, Object>> candidates, String failureReason) {
        List<RecommendationItem> items = candidates.stream()
                .limit(limit)
                .map(candidate -> new RecommendationItem(
                        longValue(candidate.get("locationId")),
                        scoreFromCandidate(candidate),
                        fallbackSummary(candidate),
                        fallbackReason(candidate),
                        DEFAULT_USAGE,
                        DEFAULT_BASIS,
                        DEFAULT_CHECK_POINTS,
                        true,
                        openAiClient.model()
                ))
                .filter(item -> item.locationId() != null)
                .toList();
        return new RecommendationOutcome(true, failureReason, items);
    }

    private BigDecimal scoreOrDefault(JsonNode scoreNode, Map<String, Object> candidate) {
        if (scoreNode != null && scoreNode.isNumber()) {
            double value = Math.max(0.0, Math.min(100.0, scoreNode.asDouble()));
            return BigDecimal.valueOf(Math.round(value * 100.0) / 100.0);
        }
        return scoreFromCandidate(candidate);
    }

    private BigDecimal scoreFromCandidate(Map<String, Object> candidate) {
        Object value = candidate.get("baseScore");
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(Math.round(number.doubleValue() * 100.0) / 100.0);
        }
        return BigDecimal.valueOf(50.0);
    }

    private List<String> checkPoints(JsonNode node) {
        if (node == null || !node.isArray()) {
            return DEFAULT_CHECK_POINTS;
        }
        List<String> result = new ArrayList<>();
        for (JsonNode item : node) {
            String text = text(item);
            if (StringUtils.hasText(text)) {
                result.add(text);
            }
            if (result.size() >= 5) {
                break;
            }
        }
        return result.isEmpty() ? DEFAULT_CHECK_POINTS : result;
    }

    private String textOrDefault(JsonNode node, String fallback, int maxLength) {
        String text = text(node);
        return truncate(StringUtils.hasText(text) ? text : fallback, maxLength);
    }

    private String fallbackSummary(Map<String, Object> candidate) {
        List<String> matchedRequired = stringList(candidate.get("matchedRequiredSceneTags"));
        List<String> matchedOptional = stringList(candidate.get("matchedOptionalSceneTags"));
        if (!matchedRequired.isEmpty() || !matchedOptional.isEmpty()) {
            List<String> tags = new ArrayList<>();
            tags.addAll(matchedRequired);
            tags.addAll(matchedOptional);
            return "요청한 장면 조건 중 " + String.join(", ", tags) + " 요소가 DB 후보 정보와 맞아 추천 후보에 포함되었습니다.";
        }
        return DEFAULT_SUMMARY;
    }

    private String fallbackReason(Map<String, Object> candidate) {
        List<String> warnings = stringList(candidate.get("dataWarnings"));
        if (!warnings.isEmpty()) {
            return "장면 조건과 일부 일치하지만 " + String.join(" ", warnings) + " 현장 답사 전 현재 상태 확인이 필요합니다.";
        }
        return DEFAULT_REASON;
    }

    private List<String> stringList(Object value) {
        if (value instanceof Iterable<?> iterable) {
            List<String> result = new ArrayList<>();
            for (Object item : iterable) {
                String text = Objects.toString(item, "").trim();
                if (StringUtils.hasText(text)) {
                    result.add(text);
                }
            }
            return result;
        }
        return List.of();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private Long longValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.longValue();
        }
        return longValue(node.asText());
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = Objects.toString(value, "").trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Object jsonSafe(Object value) {
        if (value == null
                || value instanceof String
                || value instanceof Number
                || value instanceof Boolean) {
            return value;
        }
        if (value instanceof TemporalAccessor) {
            return value.toString();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> safe = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                safe.put(Objects.toString(entry.getKey(), ""), jsonSafe(entry.getValue()));
            }
            return safe;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(this::jsonSafe).toList();
        }
        return Objects.toString(value, "");
    }

    private record OpenAiResult(JsonNode response, String failureReason) {
    }

    public record RecommendationOutcome(boolean fallback, String failureReason, List<RecommendationItem> recommendations) {
    }

    public record RecommendationItem(
            Long locationId,
            BigDecimal score,
            String aiSummary,
            String matchReason,
            String usageIdea,
            String recommendationBasis,
            List<String> checkPoints,
            boolean fallback,
            String modelName
    ) {
    }
}
