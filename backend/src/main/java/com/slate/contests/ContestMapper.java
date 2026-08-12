package com.slate.contests;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ContestMapper {

    List<Map<String, Object>> selectContests(
            @Param("filter") ContestSearchCriteria filter,
            @Param("userId") Long userId
    );

    List<Map<String, Object>> selectUrgentContests(
            @Param("userId") Long userId,
            @Param("limit") int limit
    );

    Map<String, Object> selectContestById(
            @Param("contestId") Long contestId,
            @Param("basisType") String basisType,
            @Param("basisId") Long basisId,
            @Param("userId") Long userId
    );

    int insertContest(Map<String, Object> contest);

    Map<String, Object> selectContestBySource(
            @Param("sourceName") String sourceName,
            @Param("sourceExternalId") String sourceExternalId
    );

    int upsertContestFromCrawler(Map<String, Object> contest);

    List<Map<String, Object>> selectManagedContests(
            @Param("status") String status,
            @Param("contestType") String contestType,
            @Param("requesterCompanyUserId") Long requesterCompanyUserId,
            @Param("basisType") String basisType,
            @Param("basisId") Long basisId,
            @Param("userId") Long userId,
            @Param("limit") int limit
    );

    int updateContest(Map<String, Object> contest);

    int updateContestStatus(
            @Param("contestId") Long contestId,
            @Param("status") String status
    );

    int clearContestOpenRequestApprovedContest(@Param("contestId") Long contestId);

    int deleteContestSaves(@Param("contestId") Long contestId);

    int deleteContestFitCaches(@Param("contestId") Long contestId);

    int deleteContestPreparations(@Param("contestId") Long contestId);

    int deleteContestById(@Param("contestId") Long contestId);

    int insertContestOpenRequest(Map<String, Object> request);

    List<Map<String, Object>> selectOpenRequestsByRequester(@Param("requesterUserId") Long requesterUserId);

    List<Map<String, Object>> selectContestOpenRequests(
            @Param("status") String status,
            @Param("limit") int limit
    );

    Map<String, Object> selectContestOpenRequestById(@Param("requestId") Long requestId);

    int updateContestOpenRequestDecision(
            @Param("requestId") Long requestId,
            @Param("status") String status,
            @Param("reviewReason") String reviewReason,
            @Param("reviewedBy") Long reviewedBy,
            @Param("approvedContestId") Long approvedContestId
    );

    int clearContestOpenRequestImagePath(@Param("requestId") Long requestId);

    Map<String, Object> selectFitCache(
            @Param("contestId") Long contestId,
            @Param("basisType") String basisType,
            @Param("basisId") Long basisId
    );

    int upsertFitCache(Map<String, Object> fit);

    int existsContestSave(@Param("contestId") Long contestId, @Param("userId") Long userId);

    int insertContestSave(@Param("contestId") Long contestId, @Param("userId") Long userId);

    int deleteContestSave(@Param("contestId") Long contestId, @Param("userId") Long userId);

    int recountSaveCount(@Param("contestId") Long contestId);

    Number selectSaveCount(@Param("contestId") Long contestId);

    Map<String, Object> selectProfileBasis(@Param("userId") Long userId);

    Map<String, Object> selectProfileBasisById(@Param("profileId") Long profileId, @Param("userId") Long userId);

    List<Map<String, Object>> selectTeamBases(@Param("userId") Long userId);

    Map<String, Object> selectTeamBasisById(@Param("teamId") Long teamId, @Param("userId") Long userId);

    Map<String, Object> selectPreparation(
            @Param("contestId") Long contestId,
            @Param("userId") Long userId,
            @Param("basisType") String basisType,
            @Param("basisId") Long basisId
    );

    int upsertPreparation(Map<String, Object> preparation);
}
