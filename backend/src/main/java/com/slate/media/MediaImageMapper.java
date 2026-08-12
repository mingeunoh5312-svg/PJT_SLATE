package com.slate.media;

import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MediaImageMapper {
    Map<String, Object> selectTarget(@Param("entityType") String entityType, @Param("entityId") Long entityId);
    int updatePath(@Param("entityType") String entityType, @Param("entityId") Long entityId, @Param("storedPath") String storedPath);
}
