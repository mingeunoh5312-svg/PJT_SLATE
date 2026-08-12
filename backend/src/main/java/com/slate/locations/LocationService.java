package com.slate.locations;

import java.math.BigDecimal;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slate.common.SlateException;
import com.slate.locations.AiLocationRecommendationService.RecommendationItem;
import com.slate.locations.AiLocationRecommendationService.RecommendationOutcome;
import com.slate.locations.LocationController.LocationRecommendationRequest;
import com.slate.locations.LocationController.LocationRegionFilterRequest;
import com.slate.locations.LocationController.SaveLocationCandidateRequest;
import com.slate.teams.TeamMapper;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class LocationService {

    private static final int DEFAULT_RECOMMENDATION_LIMIT = 5;
    private static final int MAX_RECOMMENDATION_LIMIT = 5;
    private static final int MAX_CANDIDATE_LIMIT = 30;
    private static final int REGIONAL_CANDIDATE_LIMIT = MAX_CANDIDATE_LIMIT;
    private static final int MAX_REGION_FANOUT = 20;
    private static final int MAX_NATIONWIDE_CANDIDATE_LIMIT = REGIONAL_CANDIDATE_LIMIT * MAX_REGION_FANOUT;
    private static final int REPRESENTATIVE_HISTORY_LIMIT = 3;
    private static final int TEAM_CONTEXT_PLAN_LIMIT = 6;
    private static final Set<String> STOP_WORDS = Set.of(
            "그리고", "하지만", "에서", "으로", "에게", "있는", "없는", "장면", "촬영", "싶어요", "하고", "합니다",
            "저예산", "단편", "영화", "정도", "인원", "후보", "로케이션", "장소", "오는", "나오는"
    );
    private static final List<SceneTag> REQUIRED_SCENE_TAGS = List.of(
            new SceneTag("ALLEY", "골목/좁은 길",
                    List.of("골목", "골목길", "좁은 길", "좁은골목", "뒷골목", "이면도로"),
                    List.of("골목", "골목길", "좁은 길", "좁은골목", "뒷골목", "이면도로", "책방골목"),
                    List.of("골목", "골목길", "좁은 길", "뒷골목", "이면도로", "책방골목")),
            new SceneTag("MARKET", "시장/상가",
                    List.of("시장", "상가", "상점가", "재래시장", "새벽시장"),
                    List.of("시장", "상가", "상점가", "재래시장", "새벽시장"),
                    List.of("시장", "상가", "상점가", "재래시장", "새벽시장")),
            new SceneTag("SCHOOL", "학교",
                    List.of("학교", "고등학교", "중학교", "초등학교", "대학교", "교실", "운동장"),
                    List.of("학교", "고등학교", "중학교", "초등학교", "대학교", "교실", "운동장"),
                    List.of("학교", "고등학교", "중학교", "초등학교", "대학교", "교실", "운동장")),
            new SceneTag("TEMPLE", "사찰/절",
                    List.of("사찰", "사원", "불당", "암자"),
                    List.of("사찰", "절", "사원", "불당", "암자"),
                    List.of("사찰", "사원", "불당", "암자")),
            new SceneTag("HOTEL", "호텔/숙박",
                    List.of("호텔", "모텔", "여관", "숙소", "객실"),
                    List.of("호텔", "모텔", "여관", "숙소", "객실"),
                    List.of("호텔", "모텔", "여관", "숙소", "객실")),
            new SceneTag("HOSPITAL", "병원",
                    List.of("병원", "의원", "응급실", "진료실"),
                    List.of("병원", "의원", "응급실", "진료실"),
                    List.of("병원", "의원", "응급실", "진료실")),
            new SceneTag("PARK", "공원/숲",
                    List.of("공원", "숲", "산책로", "수목원", "정원"),
                    List.of("공원", "숲", "산책로", "수목원", "정원"),
                    List.of("공원", "숲", "산책로", "수목원", "정원")),
            new SceneTag("WATERFRONT", "바다/강변",
                    List.of("바다", "해변", "해수욕장", "강변", "한강", "수변"),
                    List.of("바다", "해변", "해수욕장", "강변", "한강", "수변"),
                    List.of("바다", "해변", "해수욕장", "강변", "한강", "수변")),
            new SceneTag("HARBOR", "항구/부두",
                    List.of("항구", "부두", "포구", "선착장", "방파제"),
                    List.of("항구", "부두", "포구", "선착장", "방파제"),
                    List.of("항구", "부두", "포구", "선착장", "방파제")),
            new SceneTag("FACTORY", "공장/창고",
                    List.of("공장", "창고", "폐공장", "물류창고"),
                    List.of("공장", "창고", "폐공장", "물류창고"),
                    List.of("공장", "창고", "폐공장", "물류창고"))
    );
    private static final List<SceneTag> OPTIONAL_SCENE_TAGS = List.of(
            new SceneTag("RAIN", "비/우천",
                    List.of("비오는", "비 오는", "비가", "비 내리는", "비내리는", "우천", "빗속", "빗길"),
                    List.of("비오는", "비 오는", "우천", "빗속", "빗길"),
                    List.of("비오는", "우천", "빗속", "빗길")),
            new SceneTag("NIGHT", "야간/밤",
                    List.of("밤", "야간", "심야", "새벽", "새벽 3시", "3시"),
                    List.of("야간", "심야", "새벽", "밤거리"),
                    List.of("야간", "심야", "새벽", "밤거리")),
            new SceneTag("CHASE", "추격/긴장",
                    List.of("추격", "도주", "쫓기는", "긴장", "스릴러"),
                    List.of("추격", "도주", "쫓기는", "긴장", "스릴러"),
                    List.of("추격", "도주", "긴장", "스릴러"))
    );
    private static final List<RiskRule> DATA_RISK_RULES = List.of(
            new RiskRule("CLOSED", List.of("폐업", "영업 종료", "영업종료", "폐관", "폐쇄"), "폐업 또는 운영 종료 언급이 있습니다."),
            new RiskRule("DEMOLISHED", List.of("철거", "멸실", "없어짐", "현재 없음"), "철거 또는 현존 여부 확인이 필요한 표현이 있습니다."),
            new RiskRule("RELOCATED", List.of("이전", "옮김"), "이전 가능성이 언급되어 현재 위치 확인이 필요합니다."),
            new RiskRule("VACANT_LOT", List.of("공터", "나대지"), "현재 용도나 접근 가능 여부 확인이 필요합니다.")
    );

    private final LocationMapper locationMapper;
    private final TeamMapper teamMapper;
    private final AiLocationRecommendationService aiLocationRecommendationService;
    private final LocationRecommendationPersistence recommendationPersistence;
    private final ObjectMapper objectMapper;

    public LocationService(
            LocationMapper locationMapper,
            TeamMapper teamMapper,
            AiLocationRecommendationService aiLocationRecommendationService,
            LocationRecommendationPersistence recommendationPersistence,
            ObjectMapper objectMapper
    ) {
        this.locationMapper = locationMapper;
        this.teamMapper = teamMapper;
        this.aiLocationRecommendationService = aiLocationRecommendationService;
        this.recommendationPersistence = recommendationPersistence;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> recommend(Long userId, LocationRecommendationRequest request) {
        String prompt = requirePrompt(request.prompt());
        int limit = recommendationLimit(request.limit());
        Long teamId = request.teamId();
        if (teamId != null) {
            assertActiveTeamMember(userId, teamId);
        }
        TeamRecommendationContext teamContext = shouldIncludeTeamContext(teamId, request.includeTeamContext())
                ? loadTeamRecommendationContext(teamId)
                : null;
        String contextType = teamId == null ? "PERSONAL" : "TEAM";
        LocationRegionSelection regionSelection = regionSelection(request, teamContext);
        String effectivePrompt = effectivePrompt(prompt, teamContext);
        PromptAnalysis promptAnalysis = analyzePrompt(effectivePrompt);
        List<String> keywords = promptAnalysis.searchKeywords();
        List<Map<String, Object>> candidates = loadCandidates(regionSelection, promptAnalysis);

        Map<String, Object> session = new LinkedHashMap<>();
        session.put("userId", userId);
        session.put("teamId", teamId);
        session.put("prompt", prompt);
        session.put("contextType", contextType);
        Map<String, Object> parsedConditions = new LinkedHashMap<>();
        parsedConditions.put("sido", regionSelection.sido());
        parsedConditions.put("sigungu", regionSelection.sigungu());
        parsedConditions.put("sidos", regionSelection.sidos());
        parsedConditions.put("regions", regionSelection.regions());
        parsedConditions.put("limit", limit);
        parsedConditions.put("keywords", keywords);
        parsedConditions.put("requiredSceneTags", promptAnalysis.requiredLabels());
        parsedConditions.put("optionalSceneTags", promptAnalysis.optionalLabels());
        parsedConditions.put("includeTeamContext", teamContext != null);
        parsedConditions.put("teamContext", teamContext == null ? null : teamContext.summary());
        parsedConditions.put("effectivePrompt", effectivePrompt);
        parsedConditions.put("candidateLimit", MAX_CANDIDATE_LIMIT);
        session.put("parsedConditionsJson", toJson(parsedConditions));
        session.put("candidateCount", candidates.size());
        session.put("recommendationCount", 0);
        session.put("status", candidates.isEmpty() ? "NO_CANDIDATE" : "PENDING");
        Long sessionId = recommendationPersistence.createSession(session);

        if (candidates.isEmpty()) {
            return session(userId, sessionId);
        }

        try {
            RecommendationOutcome outcome = aiLocationRecommendationService.recommend(effectivePrompt, contextType, limit, candidates);
            List<Map<String, Object>> recommendations = recommendationRows(sessionId, outcome);
            String status = outcome.fallback() ? "FALLBACK" : "COMPLETED";
            recommendationPersistence.completeSession(
                    sessionId,
                    candidates.size(),
                    status,
                    outcome.failureReason(),
                    recommendations
            );
            return session(userId, sessionId);
        } catch (RuntimeException ex) {
            recommendationPersistence.failSession(sessionId, candidates.size(), failureReason(ex));
            throw ex;
        }
    }

    public Map<String, Object> session(Long userId, Long sessionId) {
        Map<String, Object> session = requireReadableSession(userId, sessionId);
        PromptAnalysis promptAnalysis = analyzePrompt(effectivePromptFromSession(session));
        List<Map<String, Object>> recommendations = locationMapper.selectSessionRecommendations(sessionId).stream()
                .map(row -> recommendationResponse(row, promptAnalysis))
                .toList();
        Map<String, Object> result = new LinkedHashMap<>(session);
        result.put("fallback", recommendations.stream().anyMatch(row -> "Y".equals(row.get("fallbackYn")) || Boolean.TRUE.equals(row.get("fallback"))));
        result.put("recommendations", recommendations);
        return result;
    }

    @Transactional
    public Map<String, Object> saveCandidate(Long userId, SaveLocationCandidateRequest request) {
        Map<String, Object> location = locationMapper.selectLocationById(request.locationId());
        if (location == null) {
            throw new SlateException("저장할 로케이션을 찾을 수 없습니다.");
        }
        Long teamId = request.teamId();
        if (teamId != null) {
            assertActiveTeamMember(userId, teamId);
        }

        Long sessionId = request.sessionId();
        Long recommendationId = request.recommendationId();
        if (recommendationId != null) {
            Map<String, Object> recommendation = locationMapper.selectRecommendationById(recommendationId);
            if (recommendation == null) {
                throw new SlateException("추천 결과를 찾을 수 없습니다.");
            }
            Long recommendationLocationId = longValue(recommendation.get("locationId"));
            if (!request.locationId().equals(recommendationLocationId)) {
                throw new SlateException("추천 결과와 저장할 로케이션이 일치하지 않습니다.");
            }
            Long recommendationSessionId = longValue(recommendation.get("sessionId"));
            if (sessionId != null && !sessionId.equals(recommendationSessionId)) {
                throw new SlateException("추천 결과와 추천 세션이 일치하지 않습니다.");
            }
            sessionId = recommendationSessionId;
        }
        Map<String, Object> session = null;
        if (sessionId != null) {
            session = requireReadableSession(userId, sessionId);
        }
        if (session != null) {
            Long sessionTeamId = longValue(session.get("teamId"));
            if (sessionTeamId != null && !sessionTeamId.equals(teamId)) {
                throw new SlateException("추천 세션의 저장 컨텍스트와 일치하지 않습니다.");
            }
        }

        Map<String, Object> existing = locationMapper.selectActiveSavedCandidate(userId, teamId, request.locationId());
        if (existing != null) {
            Map<String, Object> result = new LinkedHashMap<>(existing);
            result.put("created", false);
            result.put("alreadySaved", true);
            return result;
        }

        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("userId", userId);
        candidate.put("teamId", teamId);
        candidate.put("locationId", request.locationId());
        candidate.put("sessionId", sessionId);
        candidate.put("recommendationId", recommendationId);
        candidate.put("title", requireTitle(request.title()));
        candidate.put("memo", textOrNull(request.memo()));
        candidate.put("status", "ACTIVE");
        candidate.put("sourceType", recommendationId != null || sessionId != null ? "AI_RECOMMENDATION" : "DIRECT_LOCATION");
        try {
            locationMapper.insertSavedCandidate(candidate);
        } catch (DuplicateKeyException ex) {
            Map<String, Object> raced = locationMapper.selectActiveSavedCandidate(userId, teamId, request.locationId());
            if (raced == null) {
                throw ex;
            }
            Map<String, Object> result = new LinkedHashMap<>(raced);
            result.put("created", false);
            result.put("alreadySaved", true);
            return result;
        }

        Long candidateId = longValue(candidate.get("candidateId"));
        Map<String, Object> saved = candidateId == null ? null : locationMapper.selectSavedCandidateById(candidateId);
        if (saved == null) {
            saved = locationMapper.selectActiveSavedCandidate(userId, teamId, request.locationId());
        }
        Map<String, Object> result = new LinkedHashMap<>(saved == null ? candidate : saved);
        result.put("created", true);
        result.put("alreadySaved", false);
        return result;
    }

    private List<Map<String, Object>> recommendationRows(Long sessionId, RecommendationOutcome outcome) {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        int rankNo = 1;
        for (RecommendationItem item : outcome.recommendations()) {
            Map<String, Object> recommendation = new LinkedHashMap<>();
            recommendation.put("sessionId", sessionId);
            recommendation.put("locationId", item.locationId());
            recommendation.put("rankNo", rankNo++);
            recommendation.put("score", item.score());
            recommendation.put("aiSummary", item.aiSummary());
            recommendation.put("matchReason", item.matchReason());
            recommendation.put("usageIdea", item.usageIdea());
            recommendation.put("recommendationBasis", item.recommendationBasis());
            recommendation.put("checkPointsJson", toJson(item.checkPoints()));
            recommendation.put("modelName", item.modelName());
            recommendation.put("fallbackYn", item.fallback() ? "Y" : "N");
            recommendations.add(recommendation);
        }
        return recommendations;
    }

    private String failureReason(RuntimeException ex) {
        String message = Objects.toString(ex.getMessage(), ex.getClass().getSimpleName()).trim();
        if (!StringUtils.hasText(message)) {
            message = ex.getClass().getSimpleName();
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    public List<Map<String, Object>> personalCandidates(Long userId) {
        return locationMapper.selectPersonalCandidates(userId).stream()
                .map(this::candidateResponse)
                .toList();
    }

    public List<Map<String, Object>> teamCandidates(Long userId, Long teamId) {
        assertActiveTeamMember(userId, teamId);
        return locationMapper.selectTeamCandidates(teamId).stream()
                .map(this::candidateResponse)
                .toList();
    }

    private List<Map<String, Object>> loadCandidates(LocationRegionSelection regionSelection, PromptAnalysis promptAnalysis) {
        Map<Long, Map<String, Object>> merged = new LinkedHashMap<>();
        List<String> primaryKeywords = promptAnalysis.requiredSearchTerms().isEmpty()
                ? promptAnalysis.searchKeywords()
                : promptAnalysis.requiredSearchTerms();
        int targetCandidateLimit = candidateQueryLimit(regionSelection);
        mergeCandidates(merged, selectCandidateLocations(regionSelection, primaryKeywords));
        mergeRegionalCandidates(merged, regionSelection, primaryKeywords);
        if (!primaryKeywords.equals(promptAnalysis.searchKeywords())
                && (!regionSelection.hasFilter() || merged.size() < targetCandidateLimit)) {
            mergeCandidates(merged, selectCandidateLocations(regionSelection, promptAnalysis.searchKeywords()));
            mergeRegionalCandidates(merged, regionSelection, promptAnalysis.searchKeywords());
        }
        if (merged.size() < 10 && !promptAnalysis.searchKeywords().isEmpty()) {
            mergeCandidates(merged, selectCandidateLocations(regionSelection, List.of()));
        }
        List<Map<String, Object>> enriched = merged.values().stream()
                .map(row -> enrichCandidate(row, promptAnalysis, regionSelection.sido(), regionSelection.sigungu()))
                .toList();
        if (promptAnalysis.hasRequiredTags()) {
            List<Map<String, Object>> strictlyMatched = enriched.stream()
                    .filter(row -> stringList(row.get("missingRequiredSceneTags")).isEmpty())
                    .toList();
            if (!strictlyMatched.isEmpty()) {
                enriched = strictlyMatched;
            }
        }
        return enriched.stream()
                .sorted(Comparator
                        .<Map<String, Object>, Double>comparing(row -> doubleValue(row.get("baseScore"))).reversed()
                        .thenComparing(row -> intValue(row.get("historyCount")), Comparator.reverseOrder())
                        .thenComparing(row -> longValue(row.get("locationId")), Comparator.nullsLast(Long::compareTo)))
                .limit(candidateAnalysisLimit(regionSelection))
                .toList();
    }

    private List<Map<String, Object>> selectCandidateLocations(LocationRegionSelection regionSelection, List<String> keywords) {
        int limit = candidateQueryLimit(regionSelection);
        if (regionSelection.multiFilter()) {
            return locationMapper.selectCandidateLocationsByRegions(
                    regionSelection.sidos(),
                    regionSelection.regions(),
                    keywords,
                    limit
            );
        }
        return locationMapper.selectCandidateLocations(regionSelection.sido(), regionSelection.sigungu(), keywords, limit);
    }

    private int candidateQueryLimit(LocationRegionSelection regionSelection) {
        return regionSelection.hasFilter() ? candidateAnalysisLimit(regionSelection) : MAX_CANDIDATE_LIMIT;
    }

    private int candidateAnalysisLimit(LocationRegionSelection regionSelection) {
        if (!regionSelection.hasFilter()) {
            return MAX_NATIONWIDE_CANDIDATE_LIMIT;
        }
        return Math.min(MAX_NATIONWIDE_CANDIDATE_LIMIT, MAX_CANDIDATE_LIMIT * Math.max(1, regionSelection.filterCount()));
    }

    private void mergeCandidates(Map<Long, Map<String, Object>> merged, List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            Long locationId = longValue(row.get("locationId"));
            if (locationId != null) {
                merged.putIfAbsent(locationId, row);
            }
        }
    }

    private Map<String, Object> enrichCandidate(Map<String, Object> row, PromptAnalysis promptAnalysis, String sido, String sigungu) {
        Map<String, Object> candidate = new LinkedHashMap<>(row);
        Long locationId = longValue(candidate.get("locationId"));
        List<Map<String, Object>> histories = locationId == null ? List.of() : enrichedHistories(locationMapper.selectRepresentativeHistories(locationId, REPRESENTATIVE_HISTORY_LIMIT));
        String searchableText = searchableText(candidate, histories);
        List<String> matchedKeywords = matchedKeywords(searchableText, promptAnalysis.searchKeywords());
        List<String> matchedRequiredTags = matchedSceneTags(searchableText, promptAnalysis.requiredTags());
        List<String> matchedOptionalTags = matchedSceneTags(searchableText, promptAnalysis.optionalTags());
        List<String> missingRequiredTags = promptAnalysis.requiredLabels().stream()
                .filter(label -> !matchedRequiredTags.contains(label))
                .toList();
        List<String> dataWarnings = dataWarnings(candidate, histories);
        candidate.put("representativeHistories", histories);
        candidate.put("matchedKeywords", matchedKeywords);
        candidate.put("matchedRequiredSceneTags", matchedRequiredTags);
        candidate.put("matchedOptionalSceneTags", matchedOptionalTags);
        candidate.put("missingRequiredSceneTags", missingRequiredTags);
        candidate.put("dataWarnings", dataWarnings);
        candidate.put("baseScore", baseScore(candidate, histories, matchedKeywords, matchedRequiredTags, matchedOptionalTags, missingRequiredTags, dataWarnings));
        return candidate;
    }

    private BigDecimal baseScore(
            Map<String, Object> candidate,
            List<Map<String, Object>> histories,
            List<String> matchedKeywords,
            List<String> matchedRequiredTags,
            List<String> matchedOptionalTags,
            List<String> missingRequiredTags,
            List<String> dataWarnings
    ) {
        double score = 45.0;
        int historyCount = intValue(candidate.get("historyCount"));
        score += Math.min(14.0, historyCount * 1.4);
        score += matchedKeywords.size() * 3.0;
        score += matchedRequiredTags.size() * 24.0;
        score += matchedOptionalTags.size() * 7.0;
        score -= missingRequiredTags.size() * 38.0;
        score -= dataWarnings.size() * 6.0;
        if (StringUtils.hasText(Objects.toString(candidate.get("roadAddress"), ""))) {
            score += 3.0;
        }
        if (StringUtils.hasText(Objects.toString(candidate.get("lotAddress"), ""))) {
            score += 2.0;
        }
        if (histories.stream().anyMatch(history -> StringUtils.hasText(Objects.toString(history.get("sceneDescription"), "")))) {
            score += 4.0;
        }
        if (!missingRequiredTags.isEmpty()) {
            score = Math.min(score, 42.0);
        }
        return BigDecimal.valueOf(Math.round(Math.max(0.0, Math.min(100.0, score)) * 100.0) / 100.0);
    }

    private List<String> matchedKeywords(String searchableText, List<String> keywords) {
        if (keywords.isEmpty()) {
            return List.of();
        }
        String haystack = searchableText.toLowerCase(Locale.ROOT);
        return keywords.stream()
                .filter(keyword -> haystack.contains(keyword.toLowerCase(Locale.ROOT)))
                .toList();
    }

    private Map<String, Object> recommendationResponse(Map<String, Object> row, PromptAnalysis promptAnalysis) {
        Map<String, Object> result = new LinkedHashMap<>(row);
        result.put("fallback", "Y".equals(result.get("fallbackYn")));
        result.put("checkPoints", parseStringList(result.get("checkPointsJson")));
        result.remove("checkPointsJson");
        Long locationId = longValue(result.get("locationId"));
        List<Map<String, Object>> histories = locationId == null ? List.of() : enrichedHistories(locationMapper.selectRepresentativeHistories(locationId, REPRESENTATIVE_HISTORY_LIMIT));
        String searchableText = searchableText(result, histories);
        result.put("histories", histories);
        result.put("matchedRequiredSceneTags", matchedSceneTags(searchableText, promptAnalysis.requiredTags()));
        result.put("matchedOptionalSceneTags", matchedSceneTags(searchableText, promptAnalysis.optionalTags()));
        result.put("dataWarnings", dataWarnings(result, histories));
        return result;
    }

    private Map<String, Object> candidateResponse(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>(row);
        result.put("dataWarnings", dataWarnings(result, List.of()));
        return result;
    }

    private Map<String, Object> requireReadableSession(Long userId, Long sessionId) {
        Map<String, Object> session = locationMapper.selectSessionById(sessionId);
        if (session == null) {
            throw new SlateException("추천 세션을 찾을 수 없습니다.");
        }
        Long ownerUserId = longValue(session.get("userId"));
        if (userId.equals(ownerUserId)) {
            return session;
        }
        Long teamId = longValue(session.get("teamId"));
        if (teamId != null && isActiveTeamMember(userId, teamId)) {
            return session;
        }
        throw new SlateException(HttpStatus.FORBIDDEN, "추천 세션을 조회할 권한이 없습니다.");
    }

    private void assertActiveTeamMember(Long userId, Long teamId) {
        if (teamId == null || !isActiveTeamMember(userId, teamId)) {
            throw new SlateException(HttpStatus.FORBIDDEN, "팀의 활성 멤버만 사용할 수 있습니다.");
        }
    }

    private boolean isActiveTeamMember(Long userId, Long teamId) {
        return StringUtils.hasText(teamMapper.selectActiveTeamRole(teamId, userId));
    }

    private String requirePrompt(String value) {
        String prompt = Objects.toString(value, "").trim();
        if (prompt.length() < 5) {
            throw new SlateException("프롬프트는 5자 이상 입력해야 합니다.");
        }
        return prompt;
    }

    private String requireTitle(String value) {
        String title = Objects.toString(value, "").trim();
        if (!StringUtils.hasText(title)) {
            throw new SlateException("후보지 제목이 필요합니다.");
        }
        return title;
    }

    private int recommendationLimit(Integer requestedLimit) {
        if (requestedLimit == null) {
            return DEFAULT_RECOMMENDATION_LIMIT;
        }
        return Math.max(1, Math.min(MAX_RECOMMENDATION_LIMIT, requestedLimit));
    }

    private boolean shouldIncludeTeamContext(Long teamId, Boolean requested) {
        if (teamId == null) {
            return false;
        }
        return requested == null || Boolean.TRUE.equals(requested);
    }

    private TeamRecommendationContext loadTeamRecommendationContext(Long teamId) {
        if (teamId == null) {
            return null;
        }
        Map<String, Object> team = teamMapper.selectTeamById(teamId);
        if (team == null) {
            return null;
        }
        List<Map<String, Object>> genres = safeRows(teamMapper.selectTeamGenres(teamId));
        List<Map<String, Object>> plans = safeRows(teamMapper.selectPlanItemsByTeamId(teamId)).stream()
                .filter(plan -> !Set.of("DONE", "CANCELED").contains(Objects.toString(plan.get("status"), "")))
                .limit(TEAM_CONTEXT_PLAN_LIMIT)
                .toList();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("teamId", teamId);
        summary.put("name", team.get("name"));
        summary.put("description", team.get("description"));
        summary.put("region", team.get("publicRegionName"));
        summary.put("regionAnyYn", team.get("regionAnyYn"));
        summary.put("genres", genres.stream().map(row -> row.get("name")).filter(Objects::nonNull).toList());
        summary.put("plans", plans.stream().map(this::planSummary).toList());
        return new TeamRecommendationContext(team, genres, plans, summary, teamContextPrompt(team, genres, plans));
    }

    private Map<String, Object> planSummary(Map<String, Object> plan) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("title", plan.get("title"));
        summary.put("description", plan.get("description"));
        summary.put("roleName", plan.get("roleName"));
        summary.put("dueAt", plan.get("dueAt"));
        summary.put("status", plan.get("status"));
        return summary;
    }

    private String teamContextPrompt(Map<String, Object> team, List<Map<String, Object>> genres, List<Map<String, Object>> plans) {
        List<String> parts = new ArrayList<>();
        appendContext(parts, "팀명", team.get("name"));
        appendContext(parts, "팀 설명", team.get("description"));
        if (!"Y".equals(team.get("regionAnyYn"))) {
            appendContext(parts, "팀 활동 지역", team.get("publicRegionName"));
        }
        List<String> genreNames = genres.stream()
                .map(row -> Objects.toString(row.get("name"), "").trim())
                .filter(StringUtils::hasText)
                .toList();
        if (!genreNames.isEmpty()) {
            parts.add("팀 장르: " + String.join(", ", genreNames));
        }
        if (!plans.isEmpty()) {
            parts.add("진행 중 작업 계획:");
            for (Map<String, Object> plan : plans) {
                List<String> planParts = new ArrayList<>();
                appendText(planParts, plan.get("title"));
                appendText(planParts, plan.get("description"));
                appendText(planParts, plan.get("roleName"));
                appendText(planParts, plan.get("dueAt"));
                if (!planParts.isEmpty()) {
                    parts.add("- " + String.join(" / ", planParts));
                }
            }
        }
        return String.join("\n", parts);
    }

    private String effectivePrompt(String prompt, TeamRecommendationContext teamContext) {
        if (teamContext == null || !StringUtils.hasText(teamContext.promptText())) {
            return prompt;
        }
        return prompt + "\n\n팀 컨텍스트(선택 반영):\n" + teamContext.promptText();
    }

    private String defaultSido(String requestedSido, TeamRecommendationContext teamContext) {
        if (StringUtils.hasText(requestedSido) || teamContext == null || "Y".equals(teamContext.team().get("regionAnyYn"))) {
            return requestedSido;
        }
        List<String> tokens = regionTokens(teamContext.team().get("publicRegionName"));
        return tokens.isEmpty() ? requestedSido : tokens.get(0);
    }

    private String defaultSigungu(String requestedSigungu, String requestedSido, TeamRecommendationContext teamContext) {
        if (StringUtils.hasText(requestedSigungu)
                || StringUtils.hasText(requestedSido)
                || teamContext == null
                || "Y".equals(teamContext.team().get("regionAnyYn"))) {
            return requestedSigungu;
        }
        List<String> tokens = regionTokens(teamContext.team().get("publicRegionName"));
        return tokens.size() < 2 ? requestedSigungu : tokens.get(1);
    }

    private LocationRegionSelection regionSelection(LocationRecommendationRequest request, TeamRecommendationContext teamContext) {
        LinkedHashSet<String> sidos = new LinkedHashSet<>();
        List<Map<String, String>> regions = new ArrayList<>();
        Set<String> regionKeys = new LinkedHashSet<>();
        if (request.sidos() != null) {
            request.sidos().stream()
                    .map(this::textOrNull)
                    .filter(StringUtils::hasText)
                    .forEach(sidos::add);
        }
        if (request.regions() != null) {
            for (LocationRegionFilterRequest region : request.regions()) {
                addRegionFilter(regions, regionKeys, region.sido(), region.sigungu());
            }
        }

        String requestedSido = textOrNull(request.sido());
        String requestedSigungu = textOrNull(request.sigungu());
        if (!sidos.isEmpty() || !regions.isEmpty()) {
            if (StringUtils.hasText(requestedSigungu)) {
                addRegionFilter(regions, regionKeys, requestedSido, requestedSigungu);
            } else if (StringUtils.hasText(requestedSido)) {
                sidos.add(requestedSido);
            }
            return new LocationRegionSelection(null, null, List.copyOf(sidos), List.copyOf(regions));
        }

        String sido = defaultSido(requestedSido, teamContext);
        String sigungu = defaultSigungu(requestedSigungu, request.sido(), teamContext);
        return new LocationRegionSelection(sido, sigungu, List.of(), List.of());
    }

    private void addRegionFilter(List<Map<String, String>> regions, Set<String> regionKeys, String rawSido, String rawSigungu) {
        String sido = textOrNull(rawSido);
        String sigungu = textOrNull(rawSigungu);
        if (!StringUtils.hasText(sido) && !StringUtils.hasText(sigungu)) {
            return;
        }
        String key = Objects.toString(sido, "") + "\u0000" + Objects.toString(sigungu, "");
        if (!regionKeys.add(key)) {
            return;
        }
        Map<String, String> filter = new LinkedHashMap<>();
        filter.put("sido", sido);
        filter.put("sigungu", sigungu);
        regions.add(filter);
    }

    private List<String> regionTokens(Object value) {
        return Arrays.stream(Objects.toString(value, "").trim().split("\\s+"))
                .filter(StringUtils::hasText)
                .limit(2)
                .toList();
    }

    private String effectivePromptFromSession(Map<String, Object> session) {
        String prompt = Objects.toString(session.get("prompt"), "");
        String parsed = Objects.toString(session.get("parsedConditionsJson"), "");
        if (StringUtils.hasText(parsed)) {
            try {
                String effective = objectMapper.readTree(parsed).path("effectivePrompt").asText("");
                if (StringUtils.hasText(effective)) {
                    return effective;
                }
            } catch (Exception ignored) {
                return prompt;
            }
        }
        return prompt;
    }

    private List<Map<String, Object>> safeRows(List<Map<String, Object>> rows) {
        return rows == null ? List.of() : rows;
    }

    private void appendContext(List<String> parts, String label, Object value) {
        String text = Objects.toString(value, "").trim();
        if (StringUtils.hasText(text)) {
            parts.add(label + ": " + text);
        }
    }

    private void appendText(List<String> parts, Object value) {
        String text = Objects.toString(value, "").trim();
        if (StringUtils.hasText(text)) {
            parts.add(text);
        }
    }

    private List<String> extractKeywords(String prompt) {
        String normalized = prompt.replaceAll("[^가-힣a-zA-Z0-9]+", " ");
        LinkedHashSet<String> result = new LinkedHashSet<>();
        Arrays.stream(normalized.split("\\s+"))
                .map(this::normalizeKeyword)
                .filter(keyword -> keyword.length() >= 2)
                .filter(keyword -> !STOP_WORDS.contains(keyword))
                .limit(12)
                .forEach(result::add);
        return new ArrayList<>(result);
    }

    private PromptAnalysis analyzePrompt(String prompt) {
        List<SceneTag> requiredTags = matchedTags(prompt, REQUIRED_SCENE_TAGS);
        List<SceneTag> optionalTags = matchedTags(prompt, OPTIONAL_SCENE_TAGS);
        LinkedHashSet<String> requiredSearchTerms = new LinkedHashSet<>();
        requiredTags.forEach(tag -> tag.searchTerms().stream()
                .map(this::normalizeKeyword)
                .filter(keyword -> keyword.length() >= 2)
                .forEach(requiredSearchTerms::add));

        LinkedHashSet<String> searchKeywords = new LinkedHashSet<>(extractKeywords(prompt));
        requiredSearchTerms.forEach(searchKeywords::add);
        optionalTags.forEach(tag -> tag.searchTerms().stream()
                .map(this::normalizeKeyword)
                .filter(keyword -> keyword.length() >= 2)
                .forEach(searchKeywords::add));

        return new PromptAnalysis(
                new ArrayList<>(searchKeywords).stream().limit(24).toList(),
                new ArrayList<>(requiredSearchTerms).stream().limit(16).toList(),
                requiredTags,
                optionalTags
        );
    }

    private List<SceneTag> matchedTags(String prompt, List<SceneTag> tags) {
        String normalized = normalizeForMatch(prompt);
        return tags.stream()
                .filter(tag -> tag.triggerTerms().stream()
                        .anyMatch(term -> normalized.contains(normalizeForMatch(term))))
                .toList();
    }

    private List<String> matchedSceneTags(String searchableText, List<SceneTag> tags) {
        if (tags.isEmpty()) {
            return List.of();
        }
        String normalized = normalizeForMatch(searchableText);
        return tags.stream()
                .filter(tag -> tag.matchTerms().stream()
                        .map(this::normalizeForMatch)
                        .filter(term -> term.length() >= 2)
                        .anyMatch(normalized::contains))
                .map(SceneTag::label)
                .toList();
    }

    private List<Map<String, Object>> enrichedHistories(List<Map<String, Object>> histories) {
        return histories.stream()
                .map(history -> {
                    Map<String, Object> enriched = new LinkedHashMap<>(history);
                    enriched.put("dataWarnings", dataWarnings(enriched, List.of()));
                    return enriched;
                })
                .toList();
    }

    private List<String> dataWarnings(Map<String, Object> candidate, List<Map<String, Object>> histories) {
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        String text = searchableText(candidate, histories);
        String normalized = normalizeForMatch(text);
        for (RiskRule rule : DATA_RISK_RULES) {
            if (rule.terms().stream().map(this::normalizeForMatch).anyMatch(normalized::contains)) {
                warnings.add(rule.message());
            }
        }
        String qualityFlags = Objects.toString(candidate.get("qualityFlagsJson"), "");
        if (qualityFlags.contains("missingRoadAddress")) {
            warnings.add("도로명 주소가 없어 주소 확인이 필요합니다.");
        }
        if (qualityFlags.contains("missingLotAddress")) {
            warnings.add("지번 주소가 없어 위치 확인이 필요합니다.");
        }
        return new ArrayList<>(warnings);
    }

    private String searchableText(Map<String, Object> candidate, List<Map<String, Object>> histories) {
        StringBuilder target = new StringBuilder();
        append(target, candidate.get("placeName"));
        append(target, candidate.get("sido"));
        append(target, candidate.get("sigungu"));
        append(target, candidate.get("lotAddress"));
        append(target, candidate.get("roadAddress"));
        append(target, candidate.get("searchText"));
        append(target, candidate.get("qualityFlagsJson"));
        for (Map<String, Object> history : histories) {
            append(target, history.get("movieTitle"));
            append(target, history.get("sceneDescription"));
            append(target, history.get("characters"));
        }
        return target.toString();
    }

    private String normalizeForMatch(String value) {
        return Objects.toString(value, "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "");
    }

    private String normalizeKeyword(String value) {
        String keyword = Objects.toString(value, "").trim().toLowerCase(Locale.ROOT);
        for (String suffix : List.of("에서", "으로", "에게", "보다", "처럼", "까지", "부터", "은", "는", "이", "가", "을", "를", "의", "도", "에", "와", "과")) {
            if (keyword.length() > suffix.length() + 1 && keyword.endsWith(suffix)) {
                return keyword.substring(0, keyword.length() - suffix.length());
            }
        }
        return keyword;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(jsonSafe(value));
        } catch (Exception ex) {
            return null;
        }
    }

    private List<String> parseStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        try {
            if (value instanceof String text && StringUtils.hasText(text)) {
                return objectMapper.readValue(text, new TypeReference<List<String>>() {
                });
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return List.of();
    }

    private List<String> stringList(Object value) {
        if (value == null) {
            return List.of();
        }
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
        return parseStringList(value);
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
        if (value instanceof Iterable<?> iterable) {
            List<Object> safe = new ArrayList<>();
            for (Object item : iterable) {
                safe.add(jsonSafe(item));
            }
            return safe;
        }
        return Objects.toString(value, "");
    }

    private String textOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void append(StringBuilder builder, Object value) {
        if (value != null) {
            builder.append(' ').append(value);
        }
    }

    private void mergeRegionalCandidates(Map<Long, Map<String, Object>> merged, LocationRegionSelection regionSelection, List<String> keywords) {
        if (regionSelection.hasFilter()) {
            return;
        }
        List<String> sidos = locationMapper.selectCandidateSidos(keywords);
        if (sidos == null || sidos.isEmpty()) {
            return;
        }
        sidos.stream()
                .filter(StringUtils::hasText)
                .limit(MAX_REGION_FANOUT)
                .forEach(regionSido -> mergeCandidates(
                        merged,
                        locationMapper.selectCandidateLocations(regionSido, null, keywords, REGIONAL_CANDIDATE_LIMIT)
                ));
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

    private int intValue(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(Objects.toString(value, "0"));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private double doubleValue(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.doubleValue();
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(Objects.toString(value, "0"));
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private record SceneTag(String code, String label, List<String> triggerTerms, List<String> matchTerms, List<String> searchTerms) {
    }

    private record RiskRule(String code, List<String> terms, String message) {
    }

    private record PromptAnalysis(
            List<String> searchKeywords,
            List<String> requiredSearchTerms,
            List<SceneTag> requiredTags,
            List<SceneTag> optionalTags
    ) {
        boolean hasRequiredTags() {
            return !requiredTags.isEmpty();
        }

        List<String> requiredLabels() {
            return requiredTags.stream().map(SceneTag::label).toList();
        }

        List<String> optionalLabels() {
            return optionalTags.stream().map(SceneTag::label).toList();
        }
    }

    private record LocationRegionSelection(
            String sido,
            String sigungu,
            List<String> sidos,
            List<Map<String, String>> regions
    ) {
        boolean multiFilter() {
            return !sidos.isEmpty() || !regions.isEmpty();
        }

        boolean hasFilter() {
            return multiFilter() || StringUtils.hasText(sido) || StringUtils.hasText(sigungu);
        }

        int filterCount() {
            if (multiFilter()) {
                return sidos.size() + regions.size();
            }
            return hasFilter() ? 1 : 0;
        }
    }

    private record TeamRecommendationContext(
            Map<String, Object> team,
            List<Map<String, Object>> genres,
            List<Map<String, Object>> plans,
            Map<String, Object> summary,
            String promptText
    ) {
    }
}
