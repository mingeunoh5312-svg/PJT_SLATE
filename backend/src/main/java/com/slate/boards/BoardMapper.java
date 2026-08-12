package com.slate.boards;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BoardMapper {

    List<Map<String, Object>> selectPosts(
            @Param("category") String category,
            @Param("sort") String sort,
            @Param("keyword") String keyword,
            @Param("freeCategory") String freeCategory,
            @Param("workType") String workType,
            @Param("genreId") Long genreId,
            @Param("limit") int limit,
            @Param("userId") Long userId,
            @Param("admin") boolean admin
    );

    List<Map<String, Object>> selectMyWorks(
            @Param("userId") Long userId,
            @Param("limit") int limit
    );

    Map<String, Object> selectPostById(
            @Param("postId") Long postId,
            @Param("userId") Long userId,
            @Param("admin") boolean admin
    );

    Map<String, Object> selectPostForModeration(@Param("postId") Long postId);

    List<Map<String, Object>> selectAdminPosts(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("status") String status,
            @Param("visibility") String visibility,
            @Param("authorUserId") Long authorUserId,
            @Param("limit") int limit
    );

    Map<String, Object> selectAdminPostById(@Param("postId") Long postId);

    int insertPost(Map<String, Object> post);

    int updatePost(Map<String, Object> post);

    int updatePostAsAdmin(Map<String, Object> post);

    int updatePostStatusAsAdmin(@Param("postId") Long postId, @Param("status") String status);

    int softDeletePost(@Param("postId") Long postId, @Param("status") String status);

    int restorePostAsAdmin(@Param("postId") Long postId);

    int blindPost(@Param("postId") Long postId);

    Map<String, Object> selectWorkByPostId(@Param("postId") Long postId);

    int insertWork(Map<String, Object> work);

    int updateWork(Map<String, Object> work);

    int updateWorkTypeByPostId(@Param("postId") Long postId, @Param("workType") String workType);

    int countActiveGenres(@Param("genreIds") List<Long> genreIds);

    int insertWorkGenre(@Param("workId") Long workId, @Param("genreId") Long genreId, @Param("sortOrder") int sortOrder);

    int deleteWorkGenres(@Param("workId") Long workId);

    List<Map<String, Object>> selectWorkGenresByWorkId(@Param("workId") Long workId);

    int insertTeamWorkApprovalGenre(@Param("requestId") Long requestId, @Param("genreId") Long genreId, @Param("sortOrder") int sortOrder);

    List<Map<String, Object>> selectTeamWorkApprovalGenres(@Param("requestId") Long requestId);

    int softDeleteWorkByPostId(@Param("postId") Long postId);

    int restoreWorkByPostId(@Param("postId") Long postId);

    List<Map<String, Object>> selectTeamWorkRequestsByRequester(@Param("userId") Long userId);

    List<Map<String, Object>> selectTeamWorkRequestsByTeam(@Param("teamId") Long teamId);

    Map<String, Object> selectTeamWorkRequestById(@Param("requestId") Long requestId);

    int countTeamWorkRequestByFile(@Param("fileId") Long fileId);

    int insertTeamWorkRequest(Map<String, Object> request);

    int decideTeamWorkRequest(Map<String, Object> request);

    List<Map<String, Object>> selectReviewsByPostId(@Param("postId") Long postId);

    Map<String, Object> selectReviewById(@Param("reviewId") Long reviewId);

    Map<String, Object> selectReviewForModeration(@Param("reviewId") Long reviewId);

    int insertReview(Map<String, Object> review);

    int updateReview(@Param("reviewId") Long reviewId, @Param("content") String content);

    int softDeleteReview(
            @Param("reviewId") Long reviewId,
            @Param("status") String status,
            @Param("deleteDisplayText") String deleteDisplayText
    );

    int blindReview(@Param("reviewId") Long reviewId, @Param("displayText") String displayText);

    int countReviewReplies(@Param("reviewId") Long reviewId);

    String selectLikeActiveYn(@Param("postId") Long postId, @Param("userId") Long userId);

    int insertLike(@Param("postId") Long postId, @Param("userId") Long userId);

    int updateLike(@Param("postId") Long postId, @Param("userId") Long userId, @Param("activeYn") String activeYn);

    Number selectLikeCount(@Param("postId") Long postId);

    int recountLikeCount(@Param("postId") Long postId);

    int recountReviewCount(@Param("postId") Long postId);

    int countRecentView(@Param("postId") Long postId, @Param("userId") Long userId, @Param("ipHash") String ipHash);

    int insertViewLog(
            @Param("postId") Long postId,
            @Param("userId") Long userId,
            @Param("ipHash") String ipHash,
            @Param("viewWindowStart") LocalDateTime viewWindowStart
    );

    int incrementViewCount(@Param("postId") Long postId);

    List<Map<String, Object>> selectWorkRanking(
            @Param("period") String period,
            @Param("workType") String workType,
            @Param("genreId") Long genreId,
            @Param("currentUserId") Long currentUserId,
            @Param("limit") int limit
    );

    List<Map<String, Object>> selectPopularProfileRanking(
            @Param("currentUserId") Long currentUserId,
            @Param("limit") int limit
    );

    List<Map<String, Object>> selectPopularTeamRanking(@Param("limit") int limit);
}
