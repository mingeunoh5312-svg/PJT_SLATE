package com.slate.profiles;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProfileMapper {

    Map<String, Object> selectProfileByUserId(@Param("userId") Long userId);

    Map<String, Object> selectAnyProfileByUserId(@Param("userId") Long userId);

    Map<String, Object> selectProfileById(@Param("profileId") Long profileId);

    Map<String, Object> selectPublicProfileById(@Param("profileId") Long profileId);

    List<Map<String, Object>> selectProfileRoles(@Param("profileId") Long profileId);

    List<Map<String, Object>> selectProfileGenres(@Param("profileId") Long profileId);

    List<Map<String, Object>> selectProfileConditions(@Param("profileId") Long profileId);

    List<Map<String, Object>> selectPortfolioItems(@Param("profileId") Long profileId);

    Map<String, Object> selectPortfolioItemById(@Param("portfolioItemId") Long portfolioItemId);

    Map<String, Object> selectOwnedPortfolioItem(@Param("userId") Long userId, @Param("portfolioItemId") Long portfolioItemId);

    Map<String, Object> selectPortfolioVerificationByItemId(@Param("portfolioItemId") Long portfolioItemId);

    int countActivePortfolioItems(@Param("profileId") Long profileId);

    List<Map<String, Object>> selectPublicDataItems(
            @Param("keyword") String keyword,
            @Param("itemType") String itemType,
            @Param("limit") int limit
    );

    Map<String, Object> selectPublicDataItemById(@Param("publicDataSyncItemId") Long publicDataSyncItemId);

    int insertProfile(Map<String, Object> profile);

    int updateProfile(Map<String, Object> profile);

    int reactivateProfile(Map<String, Object> profile);

    int softDeleteProfile(@Param("userId") Long userId, @Param("profileId") Long profileId);

    int insertPortfolioItem(Map<String, Object> item);

    int updatePortfolioItem(Map<String, Object> item);

    int deletePortfolioItem(@Param("userId") Long userId, @Param("portfolioItemId") Long portfolioItemId);

    int upsertPortfolioVerification(Map<String, Object> verification);

    int deletePortfolioVerification(@Param("portfolioItemId") Long portfolioItemId);

    int deleteProfileRoles(@Param("profileId") Long profileId);

    int insertProfileRole(@Param("profileId") Long profileId, @Param("roleId") Long roleId, @Param("sortOrder") int sortOrder);

    int deleteProfileGenres(@Param("profileId") Long profileId);

    int insertProfileGenre(@Param("profileId") Long profileId, @Param("genreId") Long genreId);

    int deleteProfileConditions(@Param("profileId") Long profileId);

    int insertProfileCondition(@Param("profileId") Long profileId, @Param("conditionCode") String conditionCode);
}
