package com.slate.accounts;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AccountMapper {

    Map<String, Object> selectAccountByLoginId(@Param("loginId") String loginId);

    Map<String, Object> selectAccountByEmail(@Param("email") String email);

    Map<String, Object> selectAccountById(@Param("userId") Long userId);

    List<Map<String, Object>> selectAdminUsers(
            @Param("keyword") String keyword,
            @Param("accountType") String accountType,
            @Param("accountStatus") String accountStatus,
            @Param("limit") int limit
    );

    Map<String, Object> selectAdminUserById(@Param("userId") Long userId);

    int insertAccount(Map<String, Object> account);

    int updateLastLoginAt(@Param("userId") Long userId);

    int updateCurrentAccount(
            @Param("userId") Long userId,
            @Param("nickname") String nickname,
            @Param("email") String email,
            @Param("passwordHash") String passwordHash
    );

    int withdrawCurrentAccount(@Param("userId") Long userId);

    int insertCompanyApplication(Map<String, Object> application);

    List<Map<String, Object>> selectCompanyApplications();

    Map<String, Object> selectCompanyApplicationById(@Param("applicationId") Long applicationId);

    Map<String, Object> selectCompanyApplicationByUserId(@Param("userId") Long userId);

    int insertCompanyApplicationDocument(Map<String, Object> document);

    List<Map<String, Object>> selectCompanyApplicationDocuments(@Param("applicationId") Long applicationId);

    Map<String, Object> selectCompanyApplicationDocumentById(@Param("documentId") Long documentId);

    int softDeleteCompanyApplicationDocument(@Param("documentId") Long documentId);

    int countActiveCompanyApplicationDocuments(@Param("applicationId") Long applicationId);

    int updateCompanyApplicationDecision(
            @Param("applicationId") Long applicationId,
            @Param("status") String status,
            @Param("reason") String reason,
            @Param("reviewedBy") Long reviewedBy
    );

    int updateAccountStatus(@Param("userId") Long userId, @Param("accountStatus") String accountStatus);

    int updateAdminUser(Map<String, Object> user);

    int deactivateAdminUser(@Param("userId") Long userId, @Param("accountStatus") String accountStatus);

    int restoreAdminUser(@Param("userId") Long userId);
}
