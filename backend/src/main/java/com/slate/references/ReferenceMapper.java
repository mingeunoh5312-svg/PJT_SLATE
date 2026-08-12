package com.slate.references;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReferenceMapper {

    List<Map<String, Object>> selectCodes(@Param("groups") List<String> groups);

    List<Map<String, Object>> selectRegions(@Param("keyword") String keyword, @Param("limit") int limit);

    List<Map<String, Object>> selectRoles();

    List<Map<String, Object>> selectGenres();
}
