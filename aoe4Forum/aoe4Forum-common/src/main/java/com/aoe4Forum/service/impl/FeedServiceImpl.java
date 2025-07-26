package com.aoe4Forum.service.impl;

import com.aoe4Forum.entity.Feed;
import com.aoe4Forum.entity.constans.Constants;
import com.aoe4Forum.entity.request.FeedCursorPageRequest;
import com.aoe4Forum.entity.dto.FeedQueryResult;
import com.aoe4Forum.mapper.FeedMapper;
import com.aoe4Forum.redis.RedisUtils;
import com.aoe4Forum.service.FeedService;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class FeedServiceImpl implements FeedService {

    @Resource
    private FeedMapper feedMapper;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private RedisTemplate redisTemplate;


    @Override
    public void batchPushFeed2Follower(List<Feed> feeds){

//    保存到数据库
        feedMapper.batchAddFeeds(feeds);

//     批量保存到redis
        batchPushFeedsByPipeline(feeds);

    }


    @Override
    public FeedQueryResult batchQueryFeeds(FeedCursorPageRequest feedCursorPageRequest){
//        先查redis
        List<Feed> feeds =queryFeeds(feedCursorPageRequest);
//        缓存未命中查数据库
        if(feeds.isEmpty()){
            feeds = feedMapper.batchQueryFeedsByUserId(feedCursorPageRequest);
            if(feeds.isEmpty()){
                return null;
            }
            batchPushFeedsByPipeline(feeds);
            Feed last = feeds.get(feeds.size() - 1);
            feedCursorPageRequest.setLastId(last.getId());
            feedCursorPageRequest.setLastCreateTime(last.getCreateTime());
        }

        freshFeedExpire(feedCursorPageRequest.getUserId());
        saveFeedReadCursor(feedCursorPageRequest.getUserId());
        FeedQueryResult feedQueryResult = new FeedQueryResult();
        feedQueryResult.setFeeds(feeds);
        feedQueryResult.setLastId(feedCursorPageRequest.getLastId());
        feedQueryResult.setLastCreateTime(feedCursorPageRequest.getLastCreateTime());
        return feedQueryResult;
    }


    //Feed推送相关
    public void batchPushFeedsByPipeline(List<Feed> feeds) {
        redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) {
                for (Feed feed : feeds) {
                    String key = Constants.USER_FEED + feed.getUserId();
                    String value = feed.getPostId() + ":" + feed.getId();
                    double score = feed.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    operations.opsForZSet().add(key, value, score);
                    operations.opsForZSet().removeRange(key, Constants.NOTICE_MAX_NUM, -1);
                }
                return null;
            }
        });
    }


    public List<Feed> queryFeeds(FeedCursorPageRequest feedCursorPageRequest){
        String key = Constants.USER_FEED + feedCursorPageRequest.getUserId();
        int pageSize = feedCursorPageRequest.getPageSize();

        double maxScore;
        Long lastId = null;
        boolean isFirstPage = feedCursorPageRequest.getLastCreateTime() == null;

        if (isFirstPage) {
            maxScore = Double.MAX_VALUE;
        } else {
            maxScore = feedCursorPageRequest.getLastCreateTime()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            lastId = feedCursorPageRequest.getLastId();
        }

        Set<ZSetOperations.TypedTuple<String>> resultSet = redisUtils.zReverseRangeByScoreWithScores(
                key, 0, maxScore, 0, pageSize * 2
        );

        if (resultSet == null || resultSet.isEmpty()) {
            return Collections.emptyList();
        }

        List<Feed> feeds = new ArrayList<>();

        for (ZSetOperations.TypedTuple<String> tuple : resultSet) {
            String value = tuple.getValue();
            Double score = tuple.getScore();

            if (value == null || score == null) continue;

            value = value.replaceAll("^\"|\"$", "");

            long timeScore = score.longValue();
            String[] parts = value.split(":");
            if (parts.length != 2) continue;

            Long postId = Long.parseLong(parts[0]);
            Long id = Long.parseLong(parts[1]);

            // 双重排序判断（确保不会重复）
            if (timeScore < maxScore || (timeScore == maxScore && lastId != null && id < lastId)) {
                Feed feed = new Feed();
                feed.setPostId(postId);
                feed.setId(id);
                feed.setCreateTime(Instant.ofEpochMilli(timeScore).atZone(ZoneId.systemDefault()).toLocalDateTime());
                feeds.add(feed);
            }
            // 满足 pageSize 即停止
            if (feeds.size() >= pageSize) break;
        }

        if (feeds.isEmpty()) {
            return Collections.emptyList();
        }

        Feed last = feeds.get(feeds.size() - 1);
        feedCursorPageRequest.setLastCreateTime(last.getCreateTime());
        feedCursorPageRequest.setLastId(last.getId());

        return feeds;
    }


    public void freshFeedExpire(Long userId){
        String key = Constants.USER_FEED + userId;
        redisUtils.expire(key, Constants.REDIS_KEY_EXPIRES_ONE_WEEK, TimeUnit.MILLISECONDS);
    }


    //在用户读时调用，记录当前时间
    public void saveFeedReadCursor(Long userId){
        String key = Constants.USER_READ_CURSOR + userId;
        long timestamp = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        redisUtils.setEx(key, String.valueOf(timestamp),Constants.REDIS_KEY_EXPIRES_ONE_WEEK,TimeUnit.MILLISECONDS);
    }

}
