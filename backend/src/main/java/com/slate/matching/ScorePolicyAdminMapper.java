package com.slate.matching;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ScorePolicyAdminMapper {

    Map<String, Object> selectActivePolicy();

    Map<String, Object> selectPolicyById(@Param("policyId") Long policyId);

    List<Map<String, Object>> selectPolicyItems(@Param("policyId") Long policyId);

    int archiveActivePolicies(@Param("updatedBy") Long updatedBy);

    int insertPolicy(Map<String, Object> policy);

    int insertPolicyItem(Map<String, Object> item);

    int insertPolicyHistory(
            @Param("policyId") Long policyId,
            @Param("changedBy") Long changedBy,
            @Param("beforeJson") String beforeJson,
            @Param("afterJson") String afterJson,
            @Param("changeReason") String changeReason
    );

    List<Map<String, Object>> selectPolicyHistory(@Param("limit") int limit);
}
