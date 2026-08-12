package com.slate.locations;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LocationMapper {

    List<Map<String, Object>> selectCandidateLocations(
            @Param("sido") String sido,
            @Param("sigungu") String sigungu,
            @Param("keywords") List<String> keywords,
            @Param("limit") int limit
    );

    List<Map<String, Object>> selectCandidateLocationsByRegions(
            @Param("sidos") List<String> sidos,
            @Param("regions") List<Map<String, String>> regions,
            @Param("keywords") List<String> keywords,
            @Param("limit") int limit
    );

    List<String> selectCandidateSidos(@Param("keywords") List<String> keywords);

    Map<String, Object> selectLocationById(@Param("locationId") Long locationId);

    List<Map<String, Object>> selectRepresentativeHistories(@Param("locationId") Long locationId, @Param("limit") int limit);

    int insertSearchSession(Map<String, Object> session);

    int updateSearchSessionStatus(
            @Param("sessionId") Long sessionId,
            @Param("status") String status,
            @Param("candidateCount") int candidateCount,
            @Param("recommendationCount") int recommendationCount,
            @Param("failureReason") String failureReason
    );

    int insertRecommendationResult(Map<String, Object> recommendation);

    Map<String, Object> selectSessionById(@Param("sessionId") Long sessionId);

    List<Map<String, Object>> selectSessionRecommendations(@Param("sessionId") Long sessionId);

    Map<String, Object> selectRecommendationById(@Param("recommendationId") Long recommendationId);

    Map<String, Object> selectActiveSavedCandidate(
            @Param("userId") Long userId,
            @Param("teamId") Long teamId,
            @Param("locationId") Long locationId
    );

    int insertSavedCandidate(Map<String, Object> candidate);

    Map<String, Object> selectSavedCandidateById(@Param("candidateId") Long candidateId);

    List<Map<String, Object>> selectPersonalCandidates(@Param("userId") Long userId);

    List<Map<String, Object>> selectTeamCandidates(@Param("teamId") Long teamId);
}
