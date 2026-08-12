package com.slate.matching;

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
import com.slate.matching.MatchingController.AiRecommendationItem;
import com.slate.matching.MatchingController.AiRecommendationRequest;
import com.slate.matching.MatchingController.AiRecommendationResponse;
import com.slate.matching.MatchingController.AiRecommendationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiMatchingRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(AiMatchingRecommendationService.class);

    private static final int MAX_RECOMMENDATIONS = 3;
    private static final int MAX_PROMPT_CANDIDATES = 30;
    private static final String DEFAULT_REASON = "기존 매칭 조건과 비교해 추천 후보로 선정되었습니다.";
    private static final String SYSTEM_PROMPT = """
            당신은 영화 제작 협업 플랫폼의 매칭 추천 도우미입니다.

            아래 후보 목록 안에서만 최대 3개의 추천 대상을 선택하세요.
            후보 목록에 없는 팀이나 팀원은 절대 만들지 마세요.
            모집 역할군이 제공된 경우 역할군은 절대 조건입니다.
            후보의 역할군이 현재 모집 슬롯의 역할군과 일치하지 않으면 추천하지 마세요.
            역할군이 제공되지 않은 요청은 후보의 프로필 완성도, 장르, 지역, 일정 조건을 기준으로 추천하세요.

            추천 기준:
            - 기존 매칭 점수
            - 역할 적합도
            - 선호 장르
            - 지역/활동 범위
            - 참여 가능 일정
            - 협업 조건

            응답은 반드시 JSON 형식으로만 작성하세요.
            각 추천 항목은 targetId, targetType, targetName, reason을 포함해야 합니다.
            reason은 한국어로 1문장만 작성하세요.
            사용자에게 보일 문장이므로 짧고 구체적으로 작성하세요.
            """;

    private final MatchingService matchingService;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    public AiMatchingRecommendationService(MatchingService matchingService, OpenAiClient openAiClient, ObjectMapper objectMapper) {
        this.matchingService = matchingService;
        this.openAiClient = openAiClient;
        this.objectMapper = objectMapper;
    }

    public AiRecommendationResponse recommend(Long currentUserId, AiRecommendationRequest request) {
        if (request.type() == AiRecommendationType.TEAM_TO_MEMBER && request.slotId() == null) {
            throw new SlateException("TEAM_TO_MEMBER 추천에는 slotId가 필요합니다.");
        }
        MatchingResult matchingResult = loadCandidates(currentUserId, request);
        List<Candidate> candidates = matchingResult.candidates();
        if (candidates.isEmpty()) {
            return new AiRecommendationResponse(request.type(), List.of());
        }

        JsonNode response = callOpenAi(request.type(), matchingResult.context(), matchingResult.slot(), candidates);
        List<AiRecommendationItem> recommendations = response == null
                ? fallbackRecommendations(candidates)
                : parseRecommendations(request.type(), candidates, response);
        return new AiRecommendationResponse(request.type(), recommendations);
    }

    private MatchingResult loadCandidates(Long currentUserId, AiRecommendationRequest request) {
        if (request.type() == AiRecommendationType.TEAM_TO_MEMBER) {
            Map<String, Object> query = new LinkedHashMap<>();
            if (request.teamId() != null) {
                query.put("teamId", request.teamId());
            }
            if (request.slotId() != null) {
                query.put("slotId", request.slotId());
            }
            Map<String, Object> result = matchingService.teamToMembers(currentUserId, query);
            return new MatchingResult(candidates(request.type(), result), result.get("context"), result.get("slot"));
        }

        Map<String, Object> query = new LinkedHashMap<>();
        if (request.profileId() != null) {
            query.put("profileId", request.profileId());
        }
        Map<String, Object> result = matchingService.memberToTeams(currentUserId, query);
        return new MatchingResult(candidates(request.type(), result), result.get("context"), result.get("slot"));
    }

    private JsonNode callOpenAi(AiRecommendationType type, Object context, Object slot, List<Candidate> candidates) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", openAiClient.model());
            payload.put("temperature", 0.2);
            payload.put("response_format", Map.of("type", "json_object"));
            payload.put("messages", List.of(
                    Map.of("role", "system", "content", SYSTEM_PROMPT),
                    Map.of("role", "user", "content", userPrompt(type, context, slot, candidates))
            ));
            return openAiClient.postJson("/chat/completions", payload);
        } catch (SlateException ex) {
            if (OpenAiProperties.MISSING_API_KEY_MESSAGE.equals(ex.getMessage())) {
                log.info("AI matching OpenAI key is not configured. Falling back to score-based recommendations.");
                return null;
            }
            log.warn("AI matching OpenAI call failed with SlateException. message={}", ex.getMessage());
            return null;
        } catch (Exception ex) {
            log.warn("AI matching OpenAI call failed. reason={}", ex.toString());
            return null;
        }
    }

    private List<AiRecommendationItem> parseRecommendations(AiRecommendationType type, List<Candidate> candidates, JsonNode openAiResponse) {
        try {
            JsonNode body = recommendationBody(openAiResponse);
            JsonNode items = body.path("recommendations");
            if (!items.isArray()) {
                throw new IllegalArgumentException("OpenAI recommendations field is missing.");
            }

            Map<Long, Candidate> candidateById = new LinkedHashMap<>();
            for (Candidate candidate : candidates) {
                candidateById.putIfAbsent(candidate.targetId(), candidate);
            }

            String expectedTargetType = expectedTargetType(type);
            Set<Long> selectedIds = new LinkedHashSet<>();
            List<AiRecommendationItem> result = new ArrayList<>();
            for (JsonNode item : items) {
                Long targetId = longValue(item.path("targetId"));
                String targetType = text(item.path("targetType"));
                if (targetId == null || !expectedTargetType.equals(targetType) || !selectedIds.add(targetId)) {
                    continue;
                }
                Candidate candidate = candidateById.get(targetId);
                if (candidate == null) {
                    continue;
                }
                result.add(new AiRecommendationItem(
                        candidate.targetId(),
                        candidate.targetType(),
                        candidate.targetName(),
                        defaultString(text(item.path("reason")), DEFAULT_REASON)
                ));
                if (result.size() >= MAX_RECOMMENDATIONS) {
                    break;
                }
            }
            return result;
        } catch (Exception ex) {
            return fallbackRecommendations(candidates);
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

    private List<AiRecommendationItem> fallbackRecommendations(List<Candidate> candidates) {
        return candidates.stream()
                .limit(MAX_RECOMMENDATIONS)
                .map(candidate -> new AiRecommendationItem(
                        candidate.targetId(),
                        candidate.targetType(),
                        candidate.targetName(),
                        DEFAULT_REASON
                ))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<Candidate> candidates(AiRecommendationType type, Map<String, Object> result) {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.addAll(result.get("primary") instanceof List<?> primary ? (List<Map<String, Object>>) primary : List.of());
        rows.addAll(result.get("supplementary") instanceof List<?> supplementary ? (List<Map<String, Object>>) supplementary : List.of());

        String targetType = expectedTargetType(type);
        Map<Long, Candidate> unique = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            if (!matchesRequiredRole(type, result, row)) {
                continue;
            }
            Candidate candidate = candidate(type, targetType, row);
            if (candidate != null) {
                unique.putIfAbsent(candidate.targetId(), candidate);
            }
        }
        return unique.values().stream().limit(MAX_PROMPT_CANDIDATES).toList();
    }

    private boolean matchesRequiredRole(AiRecommendationType type, Map<String, Object> result, Map<String, Object> row) {
        if (type == AiRecommendationType.TEAM_TO_MEMBER) {
            Long requiredRoleId = longValue(map(result.get("slot")).get("roleId"));
            return requiredRoleId == null || idSet(row.get("roles"), "roleId").contains(requiredRoleId);
        }
        Long requiredRoleId = longValue(row.get("roleId"));
        return requiredRoleId != null && idSet(map(result.get("context")).get("roles"), "roleId").contains(requiredRoleId);
    }

    private Candidate candidate(AiRecommendationType type, String targetType, Map<String, Object> row) {
        Long targetId = type == AiRecommendationType.TEAM_TO_MEMBER ? longValue(row.get("profileId")) : longValue(row.get("teamId"));
        if (targetId == null) {
            return null;
        }
        String targetName = type == AiRecommendationType.TEAM_TO_MEMBER
                ? defaultString(row.get("displayName"), defaultString(row.get("nickname"), "후보"))
                : defaultString(row.get("teamName"), defaultString(row.get("name"), "팀"));
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("targetId", targetId);
        summary.put("targetType", targetType);
        summary.put("targetName", targetName);
        summary.put("score", row.get("score"));
        summary.put("scoreBadge", row.get("scoreBadge"));
        summary.put("reasons", row.get("reasons"));
        summary.put("roleId", row.get("roleId"));
        summary.put("roleName", row.get("roleName"));
        summary.put("roles", row.get("roles"));
        summary.put("genres", type == AiRecommendationType.TEAM_TO_MEMBER ? row.get("genres") : row.get("teamGenres"));
        summary.put("region", row.get("publicRegionName"));
        summary.put("experienceLevel", row.get("experienceLevel"));
        summary.put("joinAvailability", row.get("joinAvailability"));
        summary.put("collaborationCondition", row.get("collaborationCondition"));
        summary.put("collaborationConditions", row.get("collaborationConditions"));
        summary.put("preferredDuration", row.get("preferredDuration"));
        summary.put("roleDuration", row.get("roleDuration"));
        summary.put("shortIntro", row.get("shortIntro"));
        summary.put("teamDescription", row.get("teamDescription"));
        summary.put("recruitmentTitle", row.get("recruitmentTitle"));
        return new Candidate(targetId, targetType, targetName, summary);
    }

    private String userPrompt(AiRecommendationType type, Object context, Object slot, List<Candidate> candidates) throws Exception {
        Map<String, Object> prompt = new LinkedHashMap<>();
        prompt.put("requestType", type.name());
        prompt.put("expectedTargetType", expectedTargetType(type));
        prompt.put("context", context);
        prompt.put("slot", slot);
        List<String> hardConstraints = new ArrayList<>();
        if (slot != null) {
            hardConstraints.add("모집 슬롯의 roleId/roleName과 맞는 후보만 추천합니다.");
            hardConstraints.add("역할군이 맞지 않는 후보는 점수, 장르, 지역, 일정 조건이 좋아도 추천하지 않습니다.");
            hardConstraints.add("역할군이 맞는 후보가 없으면 recommendations를 빈 배열로 반환합니다.");
        } else if (type == AiRecommendationType.TEAM_TO_MEMBER) {
            hardConstraints.add("기준 팀과 모집 역할이 없으므로 전체 후보 중 프로필 수정 최신순 후보를 바탕으로 추천합니다.");
            hardConstraints.add("후보의 역할, 장르, 지역, 합류 가능 일정, 협업 조건을 종합해 추천합니다.");
        } else {
            hardConstraints.add("지원 가능한 모집 슬롯 후보 안에서만 추천합니다.");
            hardConstraints.add("프로필 역할군과 모집 역할이 맞는 후보를 우선 추천합니다.");
        }
        prompt.put("hardConstraints", hardConstraints);
        prompt.put("candidates", candidates.stream().map(Candidate::summary).toList());
        prompt.put("responseFormat", Map.of(
                "recommendations", List.of(Map.of(
                        "targetId", 1,
                        "targetType", expectedTargetType(type),
                        "targetName", "추천 대상 이름",
                        "reason", "추천 사유"
                ))
        ));
        return "아래 JSON 데이터의 candidates 안에서만 추천하세요.\n" + objectMapper.writeValueAsString(jsonSafe(prompt));
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

    private String expectedTargetType(AiRecommendationType type) {
        return type == AiRecommendationType.TEAM_TO_MEMBER ? "MEMBER" : "TEAM";
    }

    private Long longValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.longValue();
        }
        String text = node.asText("").trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = Objects.toString(value, "").trim();
        return StringUtils.hasText(text) ? Long.parseLong(text) : null;
    }

    private Map<String, Object> map(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            result.put(Objects.toString(entry.getKey(), ""), entry.getValue());
        }
        return result;
    }

    private Set<Long> idSet(Object value, String key) {
        if (!(value instanceof Collection<?> collection)) {
            return Set.of();
        }
        Set<Long> result = new LinkedHashSet<>();
        for (Object item : collection) {
            Long id = longValue(map(item).get(key));
            if (id != null) {
                result.add(id);
            }
        }
        return result;
    }

    private String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private String defaultString(Object value, String fallback) {
        String text = Objects.toString(value, "").trim();
        return StringUtils.hasText(text) ? text : fallback;
    }

    private record MatchingResult(List<Candidate> candidates, Object context, Object slot) {
    }

    private record Candidate(Long targetId, String targetType, String targetName, Map<String, Object> summary) {
    }
}
