package com.slate.boards;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WorkFileMapper {

    int insertFileMetadata(Map<String, Object> file);

    Map<String, Object> selectFileById(@Param("fileId") Long fileId);

    List<Map<String, Object>> selectStreamAccessRows(@Param("fileId") Long fileId);

    List<Map<String, Object>> selectFilesByUploader(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("limit") Integer limit
    );

    List<Map<String, Object>> selectFiles(
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("uploaderUserId") Long uploaderUserId,
            @Param("teamId") Long teamId,
            @Param("limit") Integer limit
    );

    Map<String, Object> selectStorageSummary();

    List<Map<String, Object>> selectTopUsersByActiveSize(@Param("limit") Integer limit);

    List<Map<String, Object>> selectTopTeamsByActiveSize(@Param("limit") Integer limit);

    int countFileReferences(@Param("fileId") Long fileId);

    int softDeleteFile(
            @Param("fileId") Long fileId,
            @Param("holdReason") String holdReason
    );

    int holdFile(
            @Param("fileId") Long fileId,
            @Param("holdReason") String holdReason
    );

    int restoreFile(@Param("fileId") Long fileId);

    Long sumActiveSizeByUser(@Param("userId") Long userId);

    Long sumActiveSizeByTeam(@Param("teamId") Long teamId);
}
