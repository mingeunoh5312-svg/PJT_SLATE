package com.slate.references;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminRegionMapper {

    List<Map<String, Object>> selectRegions(
            @Param("keyword") String keyword,
            @Param("sidoName") String sidoName,
            @Param("activeYn") String activeYn,
            @Param("limit") int limit
    );

    Map<String, Object> selectRegionById(@Param("regionId") Long regionId);

    Map<String, Object> selectSummary();

    int updateRegion(Map<String, Object> row);
}
