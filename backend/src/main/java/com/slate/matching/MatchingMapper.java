package com.slate.matching;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MatchingMapper {

    Map<String, Object> selectProfileByUserId(@Param("userId") Long userId);

    Map<String, Object> selectProfileById(@Param("profileId") Long profileId);

    List<Map<String, Object>> selectProfileRoles(@Param("profileId") Long profileId);

    List<Map<String, Object>> selectProfileGenres(@Param("profileId") Long profileId);

    List<Map<String, Object>> selectProfileConditions(@Param("profileId") Long profileId);

    Map<String, Object> selectTeamById(@Param("teamId") Long teamId);

    List<Map<String, Object>> selectTeamGenres(@Param("teamId") Long teamId);

    Map<String, Object> selectSlotById(@Param("slotId") Long slotId);

    List<Map<String, Object>> selectCandidateProfiles(
            @Param("teamId") Long teamId,
            @Param("slotId") Long slotId,
            @Param("currentUserId") Long currentUserId
    );

    List<Map<String, Object>> selectProfilePortfolioItems(@Param("profileId") Long profileId);

    List<Map<String, Object>> selectOpenRecruitmentSlots(@Param("userId") Long userId);

    boolean existsActiveTeamMember(@Param("teamId") Long teamId, @Param("userId") Long userId);

    String selectActiveTeamRole(@Param("teamId") Long teamId, @Param("userId") Long userId);

    Map<String, Object> selectActiveScorePolicy();

    List<Map<String, Object>> selectScorePolicyItems(@Param("policyId") Long policyId);

    int insertBookmark(@Param("userId") Long userId, @Param("targetType") String targetType, @Param("targetId") Long targetId);

    List<Map<String, Object>> selectTeamBookmarks(@Param("userId") Long userId);

    List<Map<String, Object>> selectOpenRecruitmentSlotsByTeamId(
            @Param("teamId") Long teamId,
            @Param("userId") Long userId
    );

    int deleteBookmark(@Param("userId") Long userId, @Param("targetType") String targetType, @Param("targetId") Long targetId);

    int countPendingInvitation(@Param("teamId") Long teamId, @Param("slotId") Long slotId, @Param("targetUserId") Long targetUserId);

    int insertInvitation(
            @Param("teamId") Long teamId,
            @Param("recruitmentId") Long recruitmentId,
            @Param("slotId") Long slotId,
            @Param("targetUserId") Long targetUserId,
            @Param("inviterUserId") Long inviterUserId,
            @Param("message") String message
    );

    Map<String, Object> selectPendingInvitation(
            @Param("teamId") Long teamId,
            @Param("slotId") Long slotId,
            @Param("targetUserId") Long targetUserId
    );

    List<Map<String, Object>> selectSentInvitations(@Param("userId") Long userId);

    Map<String, Object> selectInvitationById(@Param("invitationId") Long invitationId);

    int cancelPendingInvitation(@Param("invitationId") Long invitationId);

    int countPendingApplication(@Param("teamId") Long teamId, @Param("slotId") Long slotId, @Param("applicantUserId") Long applicantUserId);

    int insertApplication(
            @Param("teamId") Long teamId,
            @Param("recruitmentId") Long recruitmentId,
            @Param("slotId") Long slotId,
            @Param("applicantUserId") Long applicantUserId,
            @Param("message") String message
    );

    Map<String, Object> selectPendingApplication(
            @Param("teamId") Long teamId,
            @Param("slotId") Long slotId,
            @Param("applicantUserId") Long applicantUserId
    );

    List<Map<String, Object>> selectSentApplications(@Param("userId") Long userId);

    Map<String, Object> selectApplicationById(@Param("applicationId") Long applicationId);

    int cancelPendingApplication(@Param("applicationId") Long applicationId);

    int insertActionLog(
            @Param("actorUserId") Long actorUserId,
            @Param("actionType") String actionType,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("teamId") Long teamId,
            @Param("roleId") Long roleId
    );
}
