package com.slate.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.slate.common.ApiResponse;
import com.slate.common.SlateException;
import com.slate.matching.MatchingController.AiRecommendationRequest;
import com.slate.matching.MatchingController.AiRecommendationResponse;
import com.slate.matching.MatchingController.AiRecommendationType;
import com.slate.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class AiMatchingRecommendationServiceTest {

    private ObjectMapper objectMapper;
    private MatchingService matchingService;
    private FakeOpenAiClient openAiClient;
    private AiMatchingRecommendationService aiMatchingRecommendationService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        matchingService = new FakeMatchingService();
        openAiClient = new FakeOpenAiClient(objectMapper);
        aiMatchingRecommendationService = new AiMatchingRecommendationService(matchingService, openAiClient, objectMapper);
    }

    @Test
    void returnsEmptyRecommendationsWhenCandidatesAreEmpty() {
        fakeMatchingService().teamToMembersResponse = envelope(List.of(), List.of());

        AiRecommendationResponse response = aiMatchingRecommendationService.recommend(
                1L,
                new AiRecommendationRequest(AiRecommendationType.TEAM_TO_MEMBER, 10L, 100L, null)
        );

        assertThat(response.type()).isEqualTo(AiRecommendationType.TEAM_TO_MEMBER);
        assertThat(response.recommendations()).isEmpty();
        assertThat(openAiClient.callCount).isZero();
    }

    @Test
    void filtersOpenAiRecommendationsToKnownCandidatesAndExpectedType() throws Exception {
        fakeMatchingService().teamToMembersResponse = envelope(
                List.of(profile(1L, "이지은", 91.0), profile(2L, "김민수", 86.0)),
                List.of(profile(3L, "박서준", 72.0), profile(4L, "정하늘", 68.0))
        );
        openAiClient.response = directRecommendations("""
                {
                  "recommendations": [
                    {"targetId": 999, "targetType": "MEMBER", "targetName": "없는 후보", "reason": "없는 후보입니다."},
                    {"targetId": 1, "targetType": "TEAM", "targetName": "잘못된 타입", "reason": "타입이 다릅니다."},
                    {"targetId": 1, "targetType": "MEMBER", "targetName": "다른 이름", "reason": "촬영 경험과 일정 조건이 잘 맞습니다."},
                    {"targetId": 2, "targetType": "MEMBER", "targetName": "김민수", "reason": ""},
                    {"targetId": 3, "targetType": "MEMBER", "targetName": "박서준", "reason": "장르 선호가 팀 조건과 맞습니다."},
                    {"targetId": 4, "targetType": "MEMBER", "targetName": "정하늘", "reason": "네 번째 후보입니다."}
                  ]
                }
                """);

        AiRecommendationResponse response = aiMatchingRecommendationService.recommend(
                1L,
                new AiRecommendationRequest(AiRecommendationType.TEAM_TO_MEMBER, 10L, 100L, null)
        );

        assertThat(response.recommendations()).hasSize(3);
        assertThat(response.recommendations()).extracting("targetId").containsExactly(1L, 2L, 3L);
        assertThat(response.recommendations()).extracting("targetType").containsOnly("MEMBER");
        assertThat(response.recommendations().get(0).targetName()).isEqualTo("이지은");
        assertThat(response.recommendations().get(1).reason()).isEqualTo("기존 매칭 조건과 비교해 추천 후보로 선정되었습니다.");
    }

    @Test
    void filtersCandidatesThatDoNotMatchRecruitmentRole() throws Exception {
        fakeMatchingService().teamToMembersResponse = envelope(
                List.of(profileWithRole(1L, "이지은", 91.0, 101L), profileWithRole(2L, "김민수", 86.0, 202L)),
                List.of()
        );
        openAiClient.response = directRecommendations("""
                {
                  "recommendations": [
                    {"targetId": 2, "targetType": "MEMBER", "targetName": "김민수", "reason": "점수는 높지만 역할군이 다릅니다."},
                    {"targetId": 1, "targetType": "MEMBER", "targetName": "이지은", "reason": "모집 역할군과 후보 역할군이 일치합니다."}
                  ]
                }
                """);

        AiRecommendationResponse response = aiMatchingRecommendationService.recommend(
                1L,
                new AiRecommendationRequest(AiRecommendationType.TEAM_TO_MEMBER, 10L, 100L, null)
        );

        assertThat(response.recommendations()).hasSize(1);
        assertThat(response.recommendations().get(0).targetId()).isEqualTo(1L);
        assertThat(fakeMatchingService().lastTeamToMembersQuery).containsEntry("slotId", 100L);
    }

    @Test
    void fallsBackToTopCandidatesWhenOpenAiContentCannotBeParsed() throws Exception {
        fakeMatchingService().memberToTeamsResponse = envelope(
                List.of(team(10L, "단편영화 A", 95.0), team(11L, "독립영화 B", 90.0), team(12L, "다큐멘터리 C", 88.0), team(13L, "뮤직비디오 D", 70.0)),
                List.of()
        );
        openAiClient.response = chatResponse("not-json");

        AiRecommendationResponse response = aiMatchingRecommendationService.recommend(
                1L,
                new AiRecommendationRequest(AiRecommendationType.MEMBER_TO_TEAM, null, null, 3L)
        );

        assertThat(response.recommendations()).hasSize(3);
        assertThat(response.recommendations()).extracting("targetId").containsExactly(10L, 11L, 12L);
        assertThat(response.recommendations()).extracting("targetType").containsOnly("TEAM");
        assertThat(response.recommendations()).extracting("reason")
                .containsOnly("기존 매칭 조건과 비교해 추천 후보로 선정되었습니다.");
    }

    @Test
    void fallsBackWhenOpenAiApiKeyIsMissing() {
        fakeMatchingService().teamToMembersResponse = envelope(List.of(profile(1L, "이지은", 91.0)), List.of());
        openAiClient.exception = new SlateException(OpenAiProperties.MISSING_API_KEY_MESSAGE);

        AiRecommendationResponse response = aiMatchingRecommendationService.recommend(
                1L,
                new AiRecommendationRequest(AiRecommendationType.TEAM_TO_MEMBER, 10L, 100L, null)
        );

        assertThat(response.recommendations()).hasSize(1);
        assertThat(response.recommendations().get(0).targetId()).isEqualTo(1L);
        assertThat(response.recommendations().get(0).reason()).isEqualTo("기존 매칭 조건과 비교해 추천 후보로 선정되었습니다.");
    }

    @Test
    void requiresSlotIdForTeamToMemberAiRecommendations() {
        assertThatThrownBy(() -> aiMatchingRecommendationService.recommend(
                1L,
                new AiRecommendationRequest(AiRecommendationType.TEAM_TO_MEMBER, 10L, null, null)
        ))
                .isInstanceOf(SlateException.class)
                .hasMessage("TEAM_TO_MEMBER 추천에는 slotId가 필요합니다.");
    }

    @Test
    void controllerRequiresLoginForAiRecommendations() {
        MatchingController controller = new MatchingController(matchingService, aiMatchingRecommendationService);

        assertThatThrownBy(() -> controller.aiRecommendations(
                null,
                new AiRecommendationRequest(AiRecommendationType.TEAM_TO_MEMBER, 10L, 100L, null)
        ))
                .isInstanceOf(SlateException.class)
                .hasMessage("로그인이 필요합니다.");
    }

    @Test
    void controllerDelegatesAiRecommendationsForLoggedInUser() {
        MatchingController controller = new MatchingController(matchingService, aiMatchingRecommendationService);
        fakeMatchingService().teamToMembersResponse = envelope(List.of(), List.of());

        ApiResponse<AiRecommendationResponse> response = controller.aiRecommendations(
                new CurrentUser(1L, "user@example.com", "사용자", "USER", List.of("ROLE_USER")),
                new AiRecommendationRequest(AiRecommendationType.TEAM_TO_MEMBER, 10L, 100L, null)
        );

        assertThat(response.success()).isTrue();
        assertThat(response.data().recommendations()).isEmpty();
        assertThat(fakeMatchingService().teamToMembersCallCount).isEqualTo(1);
        assertThat(fakeMatchingService().memberToTeamsCallCount).isZero();
    }

    private FakeMatchingService fakeMatchingService() {
        return (FakeMatchingService) matchingService;
    }

    private Map<String, Object> envelope(List<Map<String, Object>> primary, List<Map<String, Object>> supplementary) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("primary", primary);
        result.put("supplementary", supplementary);
        result.put("context", Map.of("name", "테스트 컨텍스트", "roles", List.of(Map.of("roleId", 201L, "roleName", "편집"))));
        result.put("slot", Map.of("slotId", 100L, "roleId", 101L, "roleName", "촬영"));
        return result;
    }

    private Map<String, Object> profile(Long profileId, String name, double score) {
        return profileWithRole(profileId, name, score, 101L);
    }

    private Map<String, Object> profileWithRole(Long profileId, String name, double score, Long roleId) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("profileId", profileId);
        row.put("displayName", name);
        row.put("score", score);
        row.put("reasons", List.of("역할 일치", "장르 일치"));
        row.put("roles", List.of(Map.of("roleId", roleId, "roleName", roleId.equals(101L) ? "촬영" : "편집")));
        row.put("genres", List.of(Map.of("name", "드라마")));
        row.put("publicRegionName", "서울");
        return row;
    }

    private Map<String, Object> team(Long teamId, String name, double score) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("teamId", teamId);
        row.put("teamName", name);
        row.put("score", score);
        row.put("reasons", List.of("역할 일치", "일정 적합"));
        row.put("roleId", 201L);
        row.put("roleName", "편집");
        row.put("teamGenres", List.of(Map.of("name", "다큐멘터리")));
        row.put("publicRegionName", "경기");
        return row;
    }

    private JsonNode directRecommendations(String json) throws Exception {
        return objectMapper.readTree(json);
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
        private int callCount;

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
            callCount++;
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

    private static class FakeMatchingService extends MatchingService {

        private Map<String, Object> teamToMembersResponse = Map.of("primary", List.of(), "supplementary", List.of());
        private Map<String, Object> memberToTeamsResponse = Map.of("primary", List.of(), "supplementary", List.of());
        private Map<String, Object> lastTeamToMembersQuery = Map.of();
        private int teamToMembersCallCount;
        private int memberToTeamsCallCount;

        FakeMatchingService() {
            super(null, null, null);
        }

        @Override
        public Map<String, Object> teamToMembers(Long currentUserId, Map<String, Object> query) {
            teamToMembersCallCount++;
            lastTeamToMembersQuery = new LinkedHashMap<>(query);
            return teamToMembersResponse;
        }

        @Override
        public Map<String, Object> memberToTeams(Long currentUserId, Map<String, Object> query) {
            memberToTeamsCallCount++;
            return memberToTeamsResponse;
        }
    }
}
