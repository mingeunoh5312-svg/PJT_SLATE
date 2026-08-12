package com.slate.security;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DemoAccessCodeMapper {

    List<Map<String, Object>> selectAdminCodes();

    Map<String, Object> selectAdminCodeById(@Param("codeId") Long codeId);

    List<Map<String, Object>> selectVerificationCandidates(@Param("fingerprint") String fingerprint);

    List<Map<String, Object>> selectRequestCandidates(@Param("fingerprint") String fingerprint);

    int insertCode(Map<String, Object> code);

    int updateCode(Map<String, Object> code);

    int revokeCode(
            @Param("codeId") Long codeId,
            @Param("revokedBy") Long revokedBy,
            @Param("reason") String reason
    );

    int incrementUse(@Param("codeId") Long codeId);
}
