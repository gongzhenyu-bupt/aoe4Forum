package com.aoe4Forum.service;

import com.aoe4Forum.entity.constans.Constants;
import com.aoe4Forum.entity.notice.Notice;
import com.aoe4Forum.entity.notice.NoticeCursorPageRequest;
import com.aoe4Forum.entity.notice.NoticeQueryResult;
import com.aoe4Forum.redis.RedisUtils;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ZSetOperations;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public abstract class NoticeService<T extends Notice> {

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private RedisTemplate redisTemplate;

    protected abstract String encodeValueForRedis(T notice);

    protected abstract T decodeFromRedisValue(String redisValue, double score) throws UnsupportedEncodingException;

    public abstract T createNotice(Long businessId, Long id, LocalDateTime createTime);

    public abstract void insert(T notice);

    public abstract NoticeQueryResult<T> batchQueryNotices(@Param("request") NoticeCursorPageRequest request);

    public void insertToRedis(T notice) {
        if(notice == null) return;
        if(notice.getId() == null||notice.getBusinessId()==null) return;
        String key = generateRedisKey(notice);
        String value = encodeValueForRedis(notice);
        double score = generateRedisScore(notice);

        redisUtils.zAdd(key,value,score);
    }

    public void batchPushNoticesByPipeline(List<T> notices) {
        redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) {
                for (T notice : notices) {
                    if (notice == null || notice.getId() == null || notice.getBusinessId() == null) continue;

                    String key = generateRedisKey(notice);
                    String value = encodeValueForRedis(notice);
                    double score = generateRedisScore(notice);

                    operations.opsForZSet().add(key, value, score);

                    // 限制最多 N 条通知（防止过多占用空间）
                    operations.opsForZSet().removeRange(key, Constants.NOTICE_MAX_NUM, -1);
                }
                return null;
            }
        });
    }

    public List<T> queryNoticeFromRedis(NoticeCursorPageRequest noticeCursorPageRequest){
        String key = Constants.NOTICE + noticeCursorPageRequest.getNoticeName() + ":" +noticeCursorPageRequest.getUserId();
        int pageSize = noticeCursorPageRequest.getPageSize();
        double maxScore;
        Long lastId = null;
        boolean isFirstPage = noticeCursorPageRequest.getLastCreateTime()==null;

        if(isFirstPage){
            maxScore = Double.MAX_VALUE;
        }else{
            lastId = noticeCursorPageRequest.getLastId();
            maxScore = noticeCursorPageRequest.getLastCreateTime()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        }

        Set<ZSetOperations.TypedTuple<String>> resultSet = redisUtils.zReverseRangeByScoreWithScores(
                key, 0, maxScore, 0, pageSize
        );
        if(resultSet==null||resultSet.isEmpty()){
            return Collections.emptyList();
        }

        List<T> notices = new ArrayList<>();

        for (ZSetOperations.TypedTuple<String> tuple : resultSet) {
            String value = tuple.getValue();
            Double score = tuple.getScore();

            if (value == null || score == null) continue;

            value = value.replaceAll("^\"|\"$", "");
            try {
                T notice = decodeFromRedisValue(value, score);
                if (notice.getId() == null || notice.getCreateTime() == null) continue;
                long timeScore = score.longValue();
                Long id = notice.getId();
                if (timeScore < maxScore || (timeScore == maxScore && lastId != null && id < lastId)) {
                    notices.add(notice);
                }

                if (notices.size() >= pageSize) break;
            } catch (Exception e) {
            }

        }
        if(notices.isEmpty()) return Collections.emptyList();

        Notice lastNotice = notices.get(notices.size() - 1);
        noticeCursorPageRequest.setLastCreateTime(lastNotice.getCreateTime());
        noticeCursorPageRequest.setLastId(lastNotice.getId());
        return notices;
    }

    public void freshExpire(Long userId,String name){
        String key = Constants.NOTICE+name + ":" + userId;
        redisUtils.expire(key, Constants.REDIS_KEY_EXPIRES_ONE_WEEK, TimeUnit.MILLISECONDS);
    }

    public void saveNoticeReadCursor(Long userId,String name){
        String key = Constants.USER_READ_CURSOR + name + ":" + userId;
        long timestamp = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        redisUtils.setEx(key, String.valueOf(timestamp),Constants.REDIS_KEY_EXPIRES_ONE_WEEK,TimeUnit.MILLISECONDS);
    }

    private String generateRedisKey(T notice) {
        return Constants.NOTICE + notice.getClass().getSimpleName() + ":" + notice.getUserId();
    }

    private double generateRedisScore(T notice) {
        return notice.getCreateTime()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }
}
