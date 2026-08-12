package com.slate.admin;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminPermissionMapper {

    List<String> selectActivePermissionCodes(@Param("userId") Long userId);

    int countActivePermission(@Param("userId") Long userId, @Param("permissionCode") String permissionCode);

    List<Map<String, Object>> selectAdminUsers();

    Map<String, Object> selectAdminUserById(@Param("userId") Long userId);

    int deactivatePermissions(@Param("userId") Long userId);

    int upsertPermission(
            @Param("userId") Long userId,
            @Param("permissionCode") String permissionCode,
            @Param("activeYn") String activeYn,
            @Param("grantedBy") Long grantedBy
    );
}
