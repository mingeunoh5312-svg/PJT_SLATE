package com.slate.teams;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TeamMapper {

    List<Map<String, Object>> selectTeamsByUserId(@Param("userId") Long userId);

    List<Map<String, Object>> selectAdminTeams(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("regionId") Long regionId,
            @Param("leaderUserId") Long leaderUserId,
            @Param("limit") int limit
    );

    Map<String, Object> selectTeamById(@Param("teamId") Long teamId);

    Map<String, Object> selectAdminTeamById(@Param("teamId") Long teamId);

    List<Map<String, Object>> selectTeamGenres(@Param("teamId") Long teamId);

    List<Map<String, Object>> selectTeamMembers(@Param("teamId") Long teamId);

    List<Map<String, Object>> selectRecruitmentsByTeamId(@Param("teamId") Long teamId);

    Map<String, Object> selectRecruitmentById(@Param("recruitmentId") Long recruitmentId);

    List<Map<String, Object>> selectSlotsByRecruitmentId(@Param("recruitmentId") Long recruitmentId);

    Map<String, Object> selectSlotById(@Param("slotId") Long slotId);

    int countActiveTeamsByUserId(@Param("userId") Long userId);

    String selectActiveTeamRole(@Param("teamId") Long teamId, @Param("userId") Long userId);

    int insertTeam(Map<String, Object> team);

    int updateTeam(Map<String, Object> team);

    int updateAdminTeam(Map<String, Object> team);

    int updateAdminTeamStatus(@Param("teamId") Long teamId, @Param("status") String status);

    int softDeleteTeam(@Param("teamId") Long teamId);

    int softDeleteAdminTeam(@Param("teamId") Long teamId);

    int updateTeamLeader(@Param("teamId") Long teamId, @Param("leaderUserId") Long leaderUserId);

    int closeTeam(@Param("teamId") Long teamId, @Param("endType") String endType);

    int reopenTeam(@Param("teamId") Long teamId, @Param("status") String status);

    int restoreAdminTeam(@Param("teamId") Long teamId, @Param("status") String status);

    int deleteTeamGenres(@Param("teamId") Long teamId);

    int insertTeamGenre(@Param("teamId") Long teamId, @Param("genreId") Long genreId);

    int insertTeamMember(@Param("teamId") Long teamId, @Param("userId") Long userId, @Param("teamRole") String teamRole);

    int upsertActiveTeamMember(@Param("teamId") Long teamId, @Param("userId") Long userId, @Param("teamRole") String teamRole);

    Map<String, Object> selectTeamMember(@Param("teamId") Long teamId, @Param("userId") Long userId);

    int updateTeamMemberRole(@Param("teamId") Long teamId, @Param("userId") Long userId, @Param("teamRole") String teamRole);

    int updateTeamMemberStatus(@Param("teamId") Long teamId, @Param("userId") Long userId, @Param("status") String status);

    int recountTeamMemberCount(@Param("teamId") Long teamId);

    List<Map<String, Object>> selectApplicationsByTeamId(@Param("teamId") Long teamId);

    Map<String, Object> selectApplicationById(@Param("applicationId") Long applicationId);

    int updateApplicationDecision(
            @Param("applicationId") Long applicationId,
            @Param("status") String status,
            @Param("rejectReason") String rejectReason,
            @Param("decidedBy") Long decidedBy
    );

    List<Map<String, Object>> selectInvitationsByTeamId(@Param("teamId") Long teamId);

    List<Map<String, Object>> selectInvitationsByTargetUserId(@Param("userId") Long userId);

    Map<String, Object> selectInvitationById(@Param("invitationId") Long invitationId);

    int updateInvitationDecision(@Param("invitationId") Long invitationId, @Param("status") String status);

    int cancelPendingApplications(@Param("teamId") Long teamId);

    int cancelPendingInvitations(@Param("teamId") Long teamId);

    int incrementSlotAcceptedCount(@Param("slotId") Long slotId);

    int closeFilledSlot(@Param("slotId") Long slotId);

    int insertRecruitment(Map<String, Object> recruitment);

    int updateRecruitment(Map<String, Object> recruitment);

    int softDeleteRecruitment(@Param("recruitmentId") Long recruitmentId);

    int softDeleteRecruitmentsByTeamId(@Param("teamId") Long teamId);

    int closeRecruitmentsByTeamId(@Param("teamId") Long teamId);

    int closeExpiredRecruitmentsByTeamId(@Param("teamId") Long teamId);

    int restoreRecruitmentStatus(
            @Param("teamId") Long teamId,
            @Param("recruitmentId") Long recruitmentId,
            @Param("status") String status
    );

    int restoreRecruitmentStatusAsAdmin(
            @Param("teamId") Long teamId,
            @Param("recruitmentId") Long recruitmentId,
            @Param("status") String status
    );

    int insertRecruitmentSlot(Map<String, Object> slot);

    int updateRecruitmentSlot(Map<String, Object> slot);

    int softDeleteSlot(@Param("slotId") Long slotId);

    int softDeleteSlotsByRecruitment(@Param("recruitmentId") Long recruitmentId);

    int softDeleteSlotsByTeamId(@Param("teamId") Long teamId);

    int closeSlotsByTeamId(@Param("teamId") Long teamId);

    int restoreSlotStatus(
            @Param("teamId") Long teamId,
            @Param("slotId") Long slotId,
            @Param("status") String status
    );

    int restoreSlotStatusAsAdmin(
            @Param("teamId") Long teamId,
            @Param("slotId") Long slotId,
            @Param("status") String status
    );

    int cancelPendingApplicationsByRecruitment(@Param("recruitmentId") Long recruitmentId);

    int cancelPendingInvitationsByRecruitment(@Param("recruitmentId") Long recruitmentId);

    int cancelPendingApplicationsBySlot(@Param("slotId") Long slotId);

    int cancelPendingInvitationsBySlot(@Param("slotId") Long slotId);

    List<Map<String, Object>> selectPlanItemsByTeamId(@Param("teamId") Long teamId);

    Map<String, Object> selectPlanItemById(@Param("planItemId") Long planItemId);

    int insertPlanItem(Map<String, Object> plan);

    int updatePlanItem(Map<String, Object> plan);

    int updatePlanItemStatus(@Param("planItemId") Long planItemId, @Param("status") String status);

    int restorePlanItemStatus(
            @Param("teamId") Long teamId,
            @Param("planItemId") Long planItemId,
            @Param("status") String status
    );

    int insertClosureSnapshot(Map<String, Object> snapshot);

    List<Map<String, Object>> selectClosureSnapshotsByTeamId(@Param("teamId") Long teamId);

    Map<String, Object> selectClosureSnapshotById(@Param("closureSnapshotId") Long closureSnapshotId);

    Map<String, Object> selectLatestClosureSnapshotByTeamId(@Param("teamId") Long teamId);
}
