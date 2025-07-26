package com.aoe4Forum.mapper;

import com.aoe4Forum.entity.FollowRelation;
import com.aoe4Forum.entity.request.FollowCursorPageRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FollowRelationMapper {

    List<FollowRelation> queryByFollower(@Param("request") FollowCursorPageRequest request);

    List<FollowRelation> queryByFollowee(@Param("request") FollowCursorPageRequest request);

    void insertFollowRelation(FollowRelation followRelation);

    void deleteFollowRelation(FollowRelation followRelation);

    void restoreFollowRelation(FollowRelation followRelation);

    List<FollowRelation> queryByFollowerAndFollowee(@Param("follower") Long follower,@Param("followee")Long followee);

    List<FollowRelation> scanAllFollowRelations(@Param("offset") int offset, @Param("limit") int limit);

    List<FollowRelation> queryAllByFollowee(@Param("followee")Long followee);
}
