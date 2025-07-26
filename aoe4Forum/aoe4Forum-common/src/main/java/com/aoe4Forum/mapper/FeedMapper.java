package com.aoe4Forum.mapper;

import com.aoe4Forum.entity.Feed;
import com.aoe4Forum.entity.request.FeedCursorPageRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FeedMapper {
    List<Feed> queryFeedByUserId(Long userId);
    void addFeed(Feed feed);
    void batchAddFeeds(@Param("feeds") List<Feed> feeds);
    List<Feed> batchQueryFeedsByUserId(FeedCursorPageRequest feedCursorPageRequest);
    void setFeedRead(Feed feed);
}
