package com.slate.moderation;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ModerationMapper {

    int countPendingReport(
            @Param("reporterUserId") Long reporterUserId,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId
    );

    int insertReport(Map<String, Object> report);

    List<Map<String, Object>> selectReports(
            @Param("status") String status,
            @Param("targetType") String targetType,
            @Param("limit") int limit
    );

    Map<String, Object> selectReportById(@Param("reportId") Long reportId);

    int updateReportDecision(
            @Param("reportId") Long reportId,
            @Param("status") String status,
            @Param("moderationAction") String moderationAction,
            @Param("resolutionNote") String resolutionNote,
            @Param("reviewedBy") Long reviewedBy
    );

    List<Map<String, Object>> selectUsers(
            @Param("keyword") String keyword,
            @Param("accountStatus") String accountStatus,
            @Param("limit") int limit
    );

    List<Map<String, Object>> selectSanctions(
            @Param("status") String status,
            @Param("limit") int limit
    );

    Map<String, Object> selectSanctionById(@Param("sanctionId") Long sanctionId);

    Map<String, Object> selectActiveSanctionByUserId(@Param("userId") Long userId);

    List<Map<String, Object>> selectRecentSanctionsByUserId(
            @Param("userId") Long userId,
            @Param("limit") int limit
    );

    int insertSanction(Map<String, Object> sanction);

    int revokeSanction(
            @Param("sanctionId") Long sanctionId,
            @Param("revokedBy") Long revokedBy,
            @Param("revokeReason") String revokeReason
    );

    int expireSanctionsByUserId(@Param("userId") Long userId);

    int countActiveSanctionsByUserId(@Param("userId") Long userId);
}
