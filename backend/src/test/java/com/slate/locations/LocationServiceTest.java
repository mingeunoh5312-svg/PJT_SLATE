package com.slate.locations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slate.common.SlateException;
import com.slate.locations.AiLocationRecommendationService.RecommendationOutcome;
import com.slate.locations.LocationController.LocationRecommendationRequest;
import com.slate.locations.LocationController.SaveLocationCandidateRequest;
import com.slate.teams.TeamMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

class LocationServiceTest {

    private LocationMapper locationMapper;
    private TeamMapper teamMapper;
    private AiLocationRecommendationService aiService;
    private LocationRecommendationPersistence persistence;
    private LocationService service;

    @BeforeEach
    void setUp() {
        locationMapper = mock(LocationMapper.class);
        teamMapper = mock(TeamMapper.class);
        aiService = mock(AiLocationRecommendationService.class);
        persistence = mock(LocationRecommendationPersistence.class);
        service = new LocationService(locationMapper, teamMapper, aiService, persistence, new ObjectMapper());
    }

    @Test
    void teamRecommendationRequiresActiveTeamMember() {
        when(teamMapper.selectActiveTeamRole(10L, 7L)).thenReturn(null);

        assertThatThrownBy(() -> service.recommend(
                7L,
                new LocationRecommendationRequest("서울 골목 추격 장면", 10L, "서울특별시", null, null, 5)
        ))
                .isInstanceOf(SlateException.class)
                .hasMessage("팀의 활성 멤버만 사용할 수 있습니다.");

        verify(locationMapper, never()).insertSearchSession(any());
    }

    @Test
    void explicitSidoWithNoCandidatesDoesNotRetryNationwideOrCallAi() {
        when(locationMapper.selectCandidateLocations(
                org.mockito.ArgumentMatchers.eq("없는지역"),
                org.mockito.ArgumentMatchers.isNull(),
                any(),
                org.mockito.ArgumentMatchers.eq(30)
        )).thenReturn(List.of());
        when(persistence.createSession(any())).thenReturn(101L);
        when(locationMapper.selectSessionById(101L)).thenReturn(session(101L, 7L, null, "NO_CANDIDATE"));

        Map<String, Object> result = service.recommend(
                7L,
                new LocationRecommendationRequest("비 오는 밤 골목 추격", null, "없는지역", null, null, 5)
        );

        verify(locationMapper, times(3)).selectCandidateLocations(
                org.mockito.ArgumentMatchers.eq("없는지역"),
                org.mockito.ArgumentMatchers.isNull(),
                any(),
                org.mockito.ArgumentMatchers.eq(30)
        );
        verify(locationMapper, never()).selectCandidateLocations(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                any(),
                org.mockito.ArgumentMatchers.anyInt()
        );
        verify(aiService, never()).recommend(any(), any(), org.mockito.ArgumentMatchers.anyInt(), any());
        assertThat(result).containsEntry("status", "NO_CANDIDATE");
    }

    @Test
    void explicitSigunguWithNoCandidatesDoesNotRetryNationwide() {
        when(locationMapper.selectCandidateLocations(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("없는시군구"),
                any(),
                org.mockito.ArgumentMatchers.eq(30)
        )).thenReturn(List.of());
        when(persistence.createSession(any())).thenReturn(102L);
        when(locationMapper.selectSessionById(102L)).thenReturn(session(102L, 7L, null, "NO_CANDIDATE"));

        service.recommend(
                7L,
                new LocationRecommendationRequest("새벽 항구 추격 장면", null, null, "없는시군구", null, 3)
        );

        verify(locationMapper, never()).selectCandidateLocations(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                any(),
                org.mockito.ArgumentMatchers.anyInt()
        );
    }

    @Test
    void requestWithoutRegionCanUseNationwideCandidates() {
        when(locationMapper.selectCandidateLocations(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                any(),
                org.mockito.ArgumentMatchers.eq(30)
        )).thenReturn(List.of(candidateLocation(88L)));
        when(locationMapper.selectRepresentativeHistories(88L, 3)).thenReturn(List.of());
        when(persistence.createSession(any())).thenReturn(103L);
        when(aiService.recommend(any(), any(), org.mockito.ArgumentMatchers.anyInt(), any()))
                .thenReturn(new RecommendationOutcome(true, "fallback", List.of()));
        when(locationMapper.selectSessionById(103L)).thenReturn(session(103L, 7L, null, "FALLBACK"));

        service.recommend(
                7L,
                new LocationRecommendationRequest("비 오는 밤 추격 장면", null, null, null, null, 5)
        );

        verify(aiService).recommend(any(), any(), org.mockito.ArgumentMatchers.eq(5), any());
        verify(persistence).completeSession(
                org.mockito.ArgumentMatchers.eq(103L),
                org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.eq("FALLBACK"),
                org.mockito.ArgumentMatchers.eq("fallback"),
                any()
        );
    }

    @Test
    void requiredSceneTagKeepsOnlyCandidatesMatchingSceneType() {
        Map<String, Object> alley = candidateLocation(331L, "보수동책방골목", "보수동책방골목 부산 골목 촬영지");
        Map<String, Object> temple = candidateLocation(318L, "범어사", "범어사 부산 사찰 나오는 장면");
        when(locationMapper.selectCandidateLocations(
                org.mockito.ArgumentMatchers.eq("부산광역시"),
                org.mockito.ArgumentMatchers.isNull(),
                any(),
                org.mockito.ArgumentMatchers.eq(30)
        )).thenAnswer(invocation -> {
            Collection<?> keywords = invocation.getArgument(2);
            if (keywords.stream().map(String::valueOf).anyMatch("새벽"::equals)) {
                return List.of(temple, alley);
            }
            return List.of(alley);
        });
        when(locationMapper.selectRepresentativeHistories(331L, 3)).thenReturn(List.of(Map.of(
                "movieTitle", "테스트 영화",
                "sceneDescription", "좁은 골목길 추격 장면"
        )));
        when(locationMapper.selectRepresentativeHistories(318L, 3)).thenReturn(List.of(Map.of(
                "movieTitle", "테스트 영화",
                "sceneDescription", "사찰 회상 장면"
        )));
        when(persistence.createSession(any())).thenReturn(105L);
        when(aiService.recommend(any(), any(), org.mockito.ArgumentMatchers.anyInt(), any()))
                .thenReturn(new RecommendationOutcome(true, "fallback", List.of()));
        when(locationMapper.selectSessionById(105L)).thenReturn(session(105L, 7L, null, "FALLBACK"));

        service.recommend(
                7L,
                new LocationRecommendationRequest("비 오는 밤, 새벽 3시, 골목길", null, "부산광역시", null, null, 5)
        );

        ArgumentCaptor<List<Map<String, Object>>> captor = ArgumentCaptor.forClass(List.class);
        verify(aiService).recommend(any(), any(), org.mockito.ArgumentMatchers.eq(5), captor.capture());
        assertThat(captor.getValue()).extracting(row -> row.get("locationId")).containsExactly(331L);
        assertThat(captor.getValue().get(0)).containsKey("matchedRequiredSceneTags");
    }

    @Test
    void requiredSceneTagFallsBackToScoredCandidatesWhenStrictMatchIsEmpty() {
        Map<String, Object> street = candidateLocation(431L, "을지로 거리", "서울 도심 우천 추격 장면");
        when(locationMapper.selectCandidateLocations(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                any(),
                org.mockito.ArgumentMatchers.eq(30)
        )).thenAnswer(invocation -> {
            Collection<?> keywords = invocation.getArgument(2);
            return keywords.isEmpty() ? List.of(street) : List.of();
        });
        when(locationMapper.selectRepresentativeHistories(431L, 3)).thenReturn(List.of(Map.of(
                "movieTitle", "테스트 영화",
                "sceneDescription", "비 오는 날 도심 추격 장면"
        )));
        when(persistence.createSession(any())).thenReturn(109L);
        when(aiService.recommend(any(), any(), org.mockito.ArgumentMatchers.anyInt(), any()))
                .thenReturn(new RecommendationOutcome(true, "fallback", List.of()));
        when(locationMapper.selectSessionById(109L)).thenReturn(session(109L, 7L, null, "FALLBACK"));

        service.recommend(
                7L,
                new LocationRecommendationRequest("비 오는 날, 골목길, 추격씬", null, null, null, null, 5)
        );

        ArgumentCaptor<List<Map<String, Object>>> captor = ArgumentCaptor.forClass(List.class);
        verify(aiService).recommend(any(), any(), org.mockito.ArgumentMatchers.eq(5), captor.capture());
        assertThat(captor.getValue()).extracting(row -> row.get("locationId")).containsExactly(431L);
        assertThat(captor.getValue().get(0).get("missingRequiredSceneTags")).asList().contains("골목/좁은 길");
    }

    @Test
    void nationwideRecommendationMergesRegionalCandidateScansBeforeScoring() {
        Map<String, Object> globalCandidate = candidateLocation(11L, "서울 골목", "서울 골목 야간 촬영지");
        Map<String, Object> gyeonggiCandidate = candidateLocation(22L, "경기 골목", "경기도 골목 야간 촬영지");
        gyeonggiCandidate.put("historyCount", 12);
        when(locationMapper.selectCandidateLocations(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                any(),
                org.mockito.ArgumentMatchers.eq(30)
        )).thenReturn(List.of(globalCandidate));
        when(locationMapper.selectCandidateSidos(any())).thenReturn(List.of("경기도"));
        when(locationMapper.selectCandidateLocations(
                org.mockito.ArgumentMatchers.eq("경기도"),
                org.mockito.ArgumentMatchers.isNull(),
                any(),
                org.mockito.ArgumentMatchers.eq(30)
        )).thenReturn(List.of(gyeonggiCandidate));
        when(locationMapper.selectRepresentativeHistories(11L, 3)).thenReturn(List.of(Map.of(
                "movieTitle", "서울 테스트",
                "sceneDescription", "골목 야간 장면"
        )));
        when(locationMapper.selectRepresentativeHistories(22L, 3)).thenReturn(List.of(Map.of(
                "movieTitle", "경기 테스트",
                "sceneDescription", "골목 야간 장면"
        )));
        when(persistence.createSession(any())).thenReturn(106L);
        when(aiService.recommend(any(), any(), org.mockito.ArgumentMatchers.anyInt(), any()))
                .thenReturn(new RecommendationOutcome(true, "fallback", List.of()));
        when(locationMapper.selectSessionById(106L)).thenReturn(session(106L, 7L, null, "FALLBACK"));

        service.recommend(
                7L,
                new LocationRecommendationRequest("비 오는 밤 골목길", null, null, null, null, 5)
        );

        ArgumentCaptor<List<Map<String, Object>>> captor = ArgumentCaptor.forClass(List.class);
        verify(aiService).recommend(any(), any(), org.mockito.ArgumentMatchers.eq(5), captor.capture());
        assertThat(captor.getValue()).extracting(row -> row.get("locationId")).contains(11L, 22L);
        assertThat(captor.getValue().get(0)).containsEntry("locationId", 22L);
    }

    @Test
    void nationwideRecommendationKeepsUpToThirtyCandidatesPerRegionForAiAnalysis() {
        List<Map<String, Object>> seoulCandidates = new ArrayList<>();
        List<Map<String, Object>> gyeonggiCandidates = new ArrayList<>();
        for (int index = 0; index < 30; index++) {
            Map<String, Object> seoul = candidateLocation(1000L + index, "서울 골목 " + index, "서울 골목 야간 촬영지");
            seoul.put("sido", "서울특별시");
            seoulCandidates.add(seoul);

            Map<String, Object> gyeonggi = candidateLocation(2000L + index, "경기 골목 " + index, "경기도 골목 야간 촬영지");
            gyeonggi.put("sido", "경기도");
            gyeonggi.put("historyCount", 2);
            gyeonggiCandidates.add(gyeonggi);
        }
        when(locationMapper.selectCandidateLocations(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                any(),
                org.mockito.ArgumentMatchers.eq(30)
        )).thenReturn(seoulCandidates);
        when(locationMapper.selectCandidateSidos(any())).thenReturn(List.of("서울특별시", "경기도"));
        when(locationMapper.selectCandidateLocations(
                org.mockito.ArgumentMatchers.eq("서울특별시"),
                org.mockito.ArgumentMatchers.isNull(),
                any(),
                org.mockito.ArgumentMatchers.eq(30)
        )).thenReturn(seoulCandidates);
        when(locationMapper.selectCandidateLocations(
                org.mockito.ArgumentMatchers.eq("경기도"),
                org.mockito.ArgumentMatchers.isNull(),
                any(),
                org.mockito.ArgumentMatchers.eq(30)
        )).thenReturn(gyeonggiCandidates);
        when(locationMapper.selectRepresentativeHistories(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.eq(3)))
                .thenReturn(List.of(Map.of(
                        "movieTitle", "전국 테스트",
                        "sceneDescription", "골목 야간 장면"
                )));
        when(persistence.createSession(any())).thenReturn(108L);
        when(aiService.recommend(any(), any(), org.mockito.ArgumentMatchers.anyInt(), any()))
                .thenReturn(new RecommendationOutcome(true, "fallback", List.of()));
        when(locationMapper.selectSessionById(108L)).thenReturn(session(108L, 7L, null, "FALLBACK"));

        service.recommend(
                7L,
                new LocationRecommendationRequest("비 오는 밤 골목길", null, null, null, null, 5)
        );

        ArgumentCaptor<List<Map<String, Object>>> captor = ArgumentCaptor.forClass(List.class);
        verify(aiService).recommend(any(), any(), org.mockito.ArgumentMatchers.eq(5), captor.capture());
        assertThat(captor.getValue()).hasSize(60);
        assertThat(captor.getValue()).extracting(row -> row.get("locationId")).contains(1000L, 1029L, 2000L, 2029L);
    }

    @Test
    void candidateScoreDoesNotChangeOnlyBecauseRegionFilterWasSelected() {
        Map<String, Object> candidate = candidateLocation(44L, "수원 골목", "경기도 수원시 골목 야간 촬영지");
        candidate.put("sido", "경기도");
        candidate.put("sigungu", "수원시");
        candidate.put("historyCount", 8);
        when(locationMapper.selectCandidateLocations(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                any(),
                org.mockito.ArgumentMatchers.eq(30)
        )).thenReturn(List.of(candidate));
        when(locationMapper.selectCandidateSidos(any())).thenReturn(List.of("경기도"));
        when(locationMapper.selectCandidateLocations(
                org.mockito.ArgumentMatchers.eq("경기도"),
                org.mockito.ArgumentMatchers.isNull(),
                any(),
                org.mockito.ArgumentMatchers.eq(30)
        )).thenReturn(List.of(candidate));
        when(locationMapper.selectRepresentativeHistories(44L, 3)).thenReturn(List.of(Map.of(
                "movieTitle", "수원 테스트",
                "sceneDescription", "골목 야간 장면"
        )));
        when(persistence.createSession(any())).thenReturn(201L, 202L);
        when(aiService.recommend(any(), any(), org.mockito.ArgumentMatchers.anyInt(), any()))
                .thenReturn(new RecommendationOutcome(true, "fallback", List.of()));
        when(locationMapper.selectSessionById(201L)).thenReturn(session(201L, 7L, null, "FALLBACK"));
        when(locationMapper.selectSessionById(202L)).thenReturn(session(202L, 7L, null, "FALLBACK"));

        service.recommend(
                7L,
                new LocationRecommendationRequest("비 오는 밤 골목길", null, null, null, null, 5)
        );
        service.recommend(
                7L,
                new LocationRecommendationRequest("비 오는 밤 골목길", null, "경기도", null, null, 5)
        );

        ArgumentCaptor<List<Map<String, Object>>> captor = ArgumentCaptor.forClass(List.class);
        verify(aiService, times(2)).recommend(any(), any(), org.mockito.ArgumentMatchers.eq(5), captor.capture());
        Object nationwideScore = captor.getAllValues().get(0).get(0).get("baseScore");
        Object regionalScore = captor.getAllValues().get(1).get(0).get("baseScore");
        assertThat(nationwideScore).isEqualTo(regionalScore);
    }

    @Test
    void teamRecommendationUsesTeamContextByDefaultWhenNoRegionIsSelected() {
        when(teamMapper.selectActiveTeamRole(10L, 7L)).thenReturn("MEMBER");
        when(teamMapper.selectTeamById(10L)).thenReturn(team(10L));
        when(teamMapper.selectTeamGenres(10L)).thenReturn(List.of(Map.of("name", "스릴러")));
        when(teamMapper.selectPlanItemsByTeamId(10L)).thenReturn(List.of(
                Map.of(
                        "title", "야간 로케이션 리허설",
                        "description", "비 예보에 맞춘 골목 동선 확인",
                        "status", "TODO",
                        "roleName", "촬영감독"
                )
        ));
        Map<String, Object> candidate = candidateLocation(88L, "수원 골목", "경기도 수원시 골목 야간 촬영지");
        when(locationMapper.selectCandidateLocations(
                org.mockito.ArgumentMatchers.eq("경기도"),
                org.mockito.ArgumentMatchers.eq("수원시"),
                any(),
                org.mockito.ArgumentMatchers.eq(30)
        )).thenReturn(List.of(candidate));
        when(locationMapper.selectRepresentativeHistories(88L, 3)).thenReturn(List.of(Map.of(
                "movieTitle", "팀 테스트",
                "sceneDescription", "수원 골목 야간 장면"
        )));
        when(persistence.createSession(any())).thenReturn(107L);
        when(aiService.recommend(any(), any(), org.mockito.ArgumentMatchers.anyInt(), any()))
                .thenReturn(new RecommendationOutcome(true, "fallback", List.of()));
        when(locationMapper.selectSessionById(107L)).thenReturn(session(107L, 7L, 10L, "FALLBACK"));

        service.recommend(
                7L,
                new LocationRecommendationRequest("골목길 추격 장면", 10L, null, null, null, 5)
        );

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiService).recommend(promptCaptor.capture(), org.mockito.ArgumentMatchers.eq("TEAM"), org.mockito.ArgumentMatchers.eq(5), any());
        assertThat(promptCaptor.getValue()).contains("남산 새벽팀", "야간 로케이션 리허설", "비 예보");
        verify(locationMapper, times(3)).selectCandidateLocations(
                org.mockito.ArgumentMatchers.eq("경기도"),
                org.mockito.ArgumentMatchers.eq("수원시"),
                any(),
                org.mockito.ArgumentMatchers.eq(30)
        );
    }

    @Test
    void externalAiCallIsNotWrappedByTransactionalServiceMethod() throws Exception {
        Method method = LocationService.class.getMethod(
                "recommend",
                Long.class,
                LocationRecommendationRequest.class
        );

        assertThat(method.getAnnotation(Transactional.class)).isNull();
        assertThat(LocationRecommendationPersistence.class
                .getMethod("completeSession", Long.class, int.class, String.class, String.class, List.class)
                .getAnnotation(Transactional.class)).isNotNull();
    }

    @Test
    void resultPersistenceFailureMarksSessionFailed() {
        when(locationMapper.selectCandidateLocations(any(), any(), any(), org.mockito.ArgumentMatchers.eq(30)))
                .thenReturn(List.of(candidateLocation(88L)));
        when(locationMapper.selectRepresentativeHistories(88L, 3)).thenReturn(List.of());
        when(persistence.createSession(any())).thenReturn(104L);
        when(aiService.recommend(any(), any(), org.mockito.ArgumentMatchers.anyInt(), any()))
                .thenReturn(new RecommendationOutcome(true, "fallback", List.of()));
        org.mockito.Mockito.doThrow(new IllegalStateException("result write failed"))
                .when(persistence)
                .completeSession(any(), org.mockito.ArgumentMatchers.anyInt(), any(), any(), any());

        assertThatThrownBy(() -> service.recommend(
                7L,
                new LocationRecommendationRequest("비 오는 밤 추격 장면", null, null, null, null, 5)
        )).isInstanceOf(IllegalStateException.class);

        verify(persistence).failSession(104L, 1, "result write failed");
    }

    @Test
    void personalCandidateIsSavedWithNullTeamId() {
        when(locationMapper.selectLocationById(88L)).thenReturn(location(88L));
        when(locationMapper.selectActiveSavedCandidate(7L, null, 88L)).thenReturn(null);
        when(locationMapper.insertSavedCandidate(any())).thenAnswer(invocation -> {
            invocation.<Map<String, Object>>getArgument(0).put("candidateId", 301L);
            return 1;
        });
        when(locationMapper.selectSavedCandidateById(301L)).thenReturn(savedCandidate(301L, null, 88L));

        Map<String, Object> result = service.saveCandidate(
                7L,
                new SaveLocationCandidateRequest(88L, null, null, null, "명동 야간 후보", "답사 필요")
        );

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(locationMapper).insertSavedCandidate(captor.capture());
        assertThat(captor.getValue()).containsEntry("teamId", null).containsEntry("locationId", 88L);
        assertThat(result).containsEntry("candidateId", 301L).containsEntry("created", true).containsEntry("alreadySaved", false);
    }

    @Test
    void teamCandidateIsSavedOnlyForActiveTeamMember() {
        when(locationMapper.selectLocationById(88L)).thenReturn(location(88L));
        when(teamMapper.selectActiveTeamRole(10L, 7L)).thenReturn("MEMBER");
        when(locationMapper.selectActiveSavedCandidate(7L, 10L, 88L)).thenReturn(null);
        when(locationMapper.insertSavedCandidate(any())).thenAnswer(invocation -> {
            invocation.<Map<String, Object>>getArgument(0).put("candidateId", 302L);
            return 1;
        });
        when(locationMapper.selectSavedCandidateById(302L)).thenReturn(savedCandidate(302L, 10L, 88L));

        Map<String, Object> result = service.saveCandidate(
                7L,
                new SaveLocationCandidateRequest(88L, null, null, 10L, "팀 후보", null)
        );

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(locationMapper).insertSavedCandidate(captor.capture());
        assertThat(captor.getValue()).containsEntry("teamId", 10L).containsEntry("sourceType", "DIRECT_LOCATION");
        assertThat(result).containsEntry("teamId", 10L).containsEntry("created", true);
    }

    @Test
    void duplicateActiveCandidateReturnsExistingCandidateWithoutInsert() {
        Map<String, Object> existing = savedCandidate(301L, null, 88L);
        when(locationMapper.selectLocationById(88L)).thenReturn(location(88L));
        when(locationMapper.selectActiveSavedCandidate(7L, null, 88L)).thenReturn(existing);

        Map<String, Object> result = service.saveCandidate(
                7L,
                new SaveLocationCandidateRequest(88L, null, null, null, "이미 저장", null)
        );

        verify(locationMapper, never()).insertSavedCandidate(any());
        assertThat(result).containsEntry("candidateId", 301L).containsEntry("created", false).containsEntry("alreadySaved", true);
    }

    @Test
    void recommendationLocationMustMatchSavedLocation() {
        when(locationMapper.selectLocationById(88L)).thenReturn(location(88L));
        when(locationMapper.selectRecommendationById(301L)).thenReturn(Map.of(
                "recommendationId", 301L,
                "sessionId", 101L,
                "locationId", 99L
        ));

        assertThatThrownBy(() -> service.saveCandidate(
                7L,
                new SaveLocationCandidateRequest(88L, 101L, 301L, null, "불일치", null)
        ))
                .isInstanceOf(SlateException.class)
                .hasMessage("추천 결과와 저장할 로케이션이 일치하지 않습니다.");

        verify(locationMapper, never()).insertSavedCandidate(any());
    }

    @Test
    void teamSessionCanBeSavedToSameTeam() {
        prepareRecommendationSession(101L, 301L, 10L);
        when(teamMapper.selectActiveTeamRole(10L, 7L)).thenReturn("MEMBER");
        prepareSavedInsert(401L, 10L);

        Map<String, Object> result = service.saveCandidate(
                7L,
                new SaveLocationCandidateRequest(88L, 101L, 301L, 10L, "팀 A 후보", null)
        );

        assertThat(result).containsEntry("candidateId", 401L).containsEntry("created", true);
    }

    @Test
    void teamSessionCannotBeSavedToAnotherTeam() {
        prepareRecommendationSession(101L, 301L, 10L);
        when(teamMapper.selectActiveTeamRole(20L, 7L)).thenReturn("MEMBER");

        assertThatThrownBy(() -> service.saveCandidate(
                7L,
                new SaveLocationCandidateRequest(88L, 101L, 301L, 20L, "팀 B 후보", null)
        ))
                .isInstanceOf(SlateException.class)
                .hasMessage("추천 세션의 저장 컨텍스트와 일치하지 않습니다.");

        verify(locationMapper, never()).insertSavedCandidate(any());
    }

    @Test
    void teamSessionCannotBeSavedAsPersonalCandidate() {
        prepareRecommendationSession(101L, 301L, 10L);

        assertThatThrownBy(() -> service.saveCandidate(
                7L,
                new SaveLocationCandidateRequest(88L, 101L, 301L, null, "개인 후보", null)
        ))
                .isInstanceOf(SlateException.class)
                .hasMessage("추천 세션의 저장 컨텍스트와 일치하지 않습니다.");

        verify(locationMapper, never()).insertSavedCandidate(any());
    }

    @Test
    void personalSessionCanBeSavedToActiveMemberTeam() {
        prepareRecommendationSession(101L, 301L, null);
        when(teamMapper.selectActiveTeamRole(20L, 7L)).thenReturn("MEMBER");
        prepareSavedInsert(402L, 20L);

        Map<String, Object> result = service.saveCandidate(
                7L,
                new SaveLocationCandidateRequest(88L, 101L, 301L, 20L, "팀 후보", null)
        );

        assertThat(result).containsEntry("teamId", 20L).containsEntry("created", true);
    }

    @Test
    void duplicateRaceReturnsExistingCandidateAsAlreadySaved() {
        Map<String, Object> existing = savedCandidate(501L, null, 88L);
        when(locationMapper.selectLocationById(88L)).thenReturn(location(88L));
        when(locationMapper.selectActiveSavedCandidate(7L, null, 88L))
                .thenReturn(null, existing);
        when(locationMapper.insertSavedCandidate(any()))
                .thenThrow(new DuplicateKeyException("uq_saved_location_candidate_active"));

        Map<String, Object> result = service.saveCandidate(
                7L,
                new SaveLocationCandidateRequest(88L, null, null, null, "중복 경쟁", null)
        );

        assertThat(result)
                .containsEntry("candidateId", 501L)
                .containsEntry("created", false)
                .containsEntry("alreadySaved", true);
    }

    private void prepareRecommendationSession(Long sessionId, Long recommendationId, Long sessionTeamId) {
        when(locationMapper.selectLocationById(88L)).thenReturn(location(88L));
        Map<String, Object> recommendation = new LinkedHashMap<>();
        recommendation.put("recommendationId", recommendationId);
        recommendation.put("sessionId", sessionId);
        recommendation.put("locationId", 88L);
        recommendation.put("teamId", sessionTeamId);
        when(locationMapper.selectRecommendationById(recommendationId)).thenReturn(recommendation);
        when(locationMapper.selectSessionById(sessionId)).thenReturn(session(sessionId, 7L, sessionTeamId, "COMPLETED"));
    }

    private void prepareSavedInsert(Long candidateId, Long teamId) {
        when(locationMapper.selectActiveSavedCandidate(7L, teamId, 88L)).thenReturn(null);
        when(locationMapper.insertSavedCandidate(any())).thenAnswer(invocation -> {
            invocation.<Map<String, Object>>getArgument(0).put("candidateId", candidateId);
            return 1;
        });
        when(locationMapper.selectSavedCandidateById(candidateId)).thenReturn(savedCandidate(candidateId, teamId, 88L));
    }

    private Map<String, Object> session(Long sessionId, Long userId, Long teamId, String status) {
        Map<String, Object> session = new LinkedHashMap<>();
        session.put("sessionId", sessionId);
        session.put("userId", userId);
        session.put("teamId", teamId);
        session.put("status", status);
        session.put("candidateCount", 0);
        session.put("recommendationCount", 0);
        return session;
    }

    private Map<String, Object> team(Long teamId) {
        Map<String, Object> team = new LinkedHashMap<>();
        team.put("teamId", teamId);
        team.put("name", "남산 새벽팀");
        team.put("description", "비 오는 새벽 골목 추격 장면을 준비하는 팀입니다.");
        team.put("publicRegionName", "경기도 수원시");
        team.put("regionAnyYn", "N");
        return team;
    }

    private Map<String, Object> candidateLocation(Long locationId) {
        return candidateLocation(locationId, "테스트 장소", "테스트 장소");
    }

    private Map<String, Object> candidateLocation(Long locationId, String placeName, String searchText) {
        Map<String, Object> candidate = location(locationId);
        candidate.put("sido", "서울특별시");
        candidate.put("sigungu", "중구");
        candidate.put("placeName", placeName);
        candidate.put("searchText", searchText);
        candidate.put("historyCount", 1);
        return candidate;
    }

    private Map<String, Object> location(Long locationId) {
        Map<String, Object> location = new LinkedHashMap<>();
        location.put("locationId", locationId);
        location.put("placeName", "테스트 장소");
        return location;
    }

    private Map<String, Object> savedCandidate(Long candidateId, Long teamId, Long locationId) {
        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("candidateId", candidateId);
        candidate.put("userId", 7L);
        candidate.put("teamId", teamId);
        candidate.put("locationId", locationId);
        candidate.put("title", "저장 후보");
        candidate.put("placeName", "테스트 장소");
        candidate.put("histories", List.of());
        return candidate;
    }
}
