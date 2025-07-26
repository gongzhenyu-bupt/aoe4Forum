package com.aoe4Forum.service;

import com.aoe4Forum.entity.FollowRelation;
import com.aoe4Forum.entity.request.FollowCursorPageRequest;
import com.aoe4Forum.entity.dto.FollowQueryResult;

public interface FollowService {

    FollowQueryResult queryFollower(FollowCursorPageRequest request);

    FollowQueryResult queryFollowee(FollowCursorPageRequest request);

    int addFollower(FollowRelation relation);

    int deleteFollower(FollowRelation relation);

    int checkFollower(FollowRelation relation);

}
