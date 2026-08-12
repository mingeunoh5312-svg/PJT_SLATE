package com.slate.follows;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FollowMapper {

    Map<String, Object> selectActiveActorByUserId(@Param("userId") Long userId);

    Map<String, Object> selectPublicTargetByProfileId(@Param("profileId") Long profileId);

    int countRelation(
            @Param("followerUserId") Long followerUserId,
            @Param("followingUserId") Long followingUserId
    );

    int insertIgnoreFollow(
            @Param("followerUserId") Long followerUserId,
            @Param("followingUserId") Long followingUserId
    );

    int deleteFollow(
            @Param("followerUserId") Long followerUserId,
            @Param("followingUserId") Long followingUserId
    );

    int countPublicFollowers(@Param("followingUserId") Long followingUserId);

    int countPublicFollowing(@Param("followerUserId") Long followerUserId);

    List<Map<String, Object>> selectPublicFollowers(
            @Param("followingUserId") Long followingUserId,
            @Param("currentUserId") Long currentUserId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    List<Map<String, Object>> selectPublicFollowing(
            @Param("followerUserId") Long followerUserId,
            @Param("currentUserId") Long currentUserId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );
}
