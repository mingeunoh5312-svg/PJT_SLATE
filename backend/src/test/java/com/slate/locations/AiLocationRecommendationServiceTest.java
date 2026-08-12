package com.slate.locations;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.slate.common.SlateException;
import com.slate.locations.AiLocationRecommendationService.RecommendationOutcome;
import com.slate.matching.OpenAiClient;
import com.slate.matching.OpenAiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class AiLocationRecommendationServiceTest {

    private ObjectMapper objectMapper;
    private FakeOpenAiClient openAiClient;
    private AiLocationRecommendationService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        openAiClient = new FakeOpenAiClient(objectMapper);
        service = new AiLocationRecommendationService(openAiClient, objectMapper);
    }

    @Test
    void filtersOpenAiRecommendationsToKnownCandidateLocationIds() throws Exception {
        openAiClient.response = objectMapper.readTree("""
                {
                  "recommendations": [
                    {"locationId": 999, "score": 99.0, "aiSummary": "없는 장소"},
                    {"locationId": 11, "score": 87.5, "aiSummary": "도심 추격 후보", "matchReason": "골목감이 있습니다.", "usageIdea": "야간 추격", "recommendationBasis": "장소명", "checkPoints": ["허가 확인"]},
                    {"locationId": 11, "score": 70.0, "aiSummary": "중복"},
                    {"locationId": 12, "score": 81.0, "aiSummary": "", "checkPoints": []}
                  ]
                }
                """);

        RecommendationOutcome outcome = service.recommend("비 오는 밤 골목 추격", "PERSONAL", 5, candidates(11L, 12L));

        assertThat(outcome.fallback()).isFalse();
        assertThat(outcome.recommendations()).hasSize(2);
        assertThat(outcome.recommendations()).extracting("locationId").containsExactly(11L, 12L);
        assertThat(outcome.recommendations().get(0).score()).isEqualByComparingTo(BigDecimal.valueOf(87.5));
        assertThat(outcome.recommendations().get(1).aiSummary()).contains("요청한 장면 조건");
    }

    @Test
    void fallsBackWhenOpenAiCallFails() {
        openAiClient.exception = new SlateException(OpenAiProperties.MISSING_API_KEY_MESSAGE);

        RecommendationOutcome outcome = service.recommend("서울 낡은 골목", "PERSONAL", 2, candidates(11L, 12L, 13L));

        assertThat(outcome.fallback()).isTrue();
        assertThat(outcome.recommendations()).hasSize(2);
        assertThat(outcome.recommendations()).extracting("locationId").containsExactly(11L, 12L);
        assertThat(outcome.recommendations()).extracting("fallback").containsOnly(true);
    }

    @Test
    void limitsRecommendationsToRequestedLimit() throws Exception {
        openAiClient.response = objectMapper.readTree("""
                {
                  "recommendations": [
                    {"locationId": 11, "score": 91.0},
                    {"locationId": 12, "score": 90.0},
                    {"locationId": 13, "score": 89.0}
                  ]
                }
                """);

        RecommendationOutcome outcome = service.recommend("도심 장면", "PERSONAL", 2, candidates(11L, 12L, 13L));

        assertThat(outcome.recommendations()).hasSize(2);
        assertThat(outcome.recommendations()).extracting("locationId").containsExactly(11L, 12L);
    }

    @Test
    void parsesChatCompletionContentJson() throws Exception {
        openAiClient.response = chatResponse("""
                {"recommendations":[{"locationId":12,"score":83.0,"checkPoints":["동선 확인"]}]}
                """);

        RecommendationOutcome outcome = service.recommend("실내 장면", "TEAM", 5, candidates(11L, 12L));

        assertThat(outcome.fallback()).isFalse();
        assertThat(outcome.recommendations()).singleElement().satisfies(item -> {
            assertThat(item.locationId()).isEqualTo(12L);
            assertThat(item.checkPoints()).containsExactly("동선 확인");
        });
    }

    private List<Map<String, Object>> candidates(Long... locationIds) {
        return List.of(locationIds).stream().map(id -> {
            Map<String, Object> candidate = new LinkedHashMap<>();
            candidate.put("locationId", id);
            candidate.put("placeName", "테스트 장소 " + id);
            candidate.put("sido", "서울특별시");
            candidate.put("sigungu", "중구");
            candidate.put("latitude", BigDecimal.valueOf(37.5));
            candidate.put("longitude", BigDecimal.valueOf(126.9));
            candidate.put("historyCount", 3);
            candidate.put("baseScore", BigDecimal.valueOf(80 - id % 10));
            candidate.put("representativeHistories", List.of(Map.of("movieTitle", "테스트 영화")));
            candidate.put("matchedKeywords", List.of("서울"));
            return candidate;
        }).toList();
    }

    private JsonNode chatResponse(String content) {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode message = objectMapper.createObjectNode();
        message.put("content", content);
        ObjectNode choice = objectMapper.createObjectNode();
        choice.set("message", message);
        root.putArray("choices").add(choice);
        return root;
    }

    private static class FakeOpenAiClient extends OpenAiClient {

        private final ObjectMapper objectMapper;
        private JsonNode response;
        private SlateException exception;

        FakeOpenAiClient(ObjectMapper objectMapper) {
            super(new OpenAiProperties("test-openai-key", "https://api.openai.com/v1", "test-model"), objectMapper, RestClient.builder());
            this.objectMapper = objectMapper;
        }

        @Override
        public String model() {
            return "test-model";
        }

        @Override
        public JsonNode postJson(String path, Object payload) {
            if (exception != null) {
                throw exception;
            }
            if (response != null) {
                return response;
            }
            ObjectNode root = objectMapper.createObjectNode();
            root.putArray("recommendations");
            return root;
        }
    }
}
