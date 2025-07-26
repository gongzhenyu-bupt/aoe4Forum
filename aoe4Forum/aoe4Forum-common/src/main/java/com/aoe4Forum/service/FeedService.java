package com.aoe4Forum.service;

import com.aoe4Forum.entity.Feed;
import com.aoe4Forum.entity.request.FeedCursorPageRequest;
import com.aoe4Forum.entity.dto.FeedQueryResult;

import java.util.List;

public interface FeedService {

    void batchPushFeed2Follower(List<Feed> feeds);

    FeedQueryResult batchQueryFeeds(FeedCursorPageRequest feedCursorPageRequest);

}
