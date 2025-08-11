package com.aoe4Forum.service.impl;

import com.aoe4Forum.entity.Post;
import com.aoe4Forum.entity.PostContent;
import com.aoe4Forum.entity.constans.Constants;
import com.aoe4Forum.entity.dto.PostCountDto;
import com.aoe4Forum.entity.dto.RedisPostQueryDto;
import com.aoe4Forum.exception.BusinessException;
import com.aoe4Forum.mapper.PostMapper;
import com.aoe4Forum.redis.RedisUtils;
import com.aoe4Forum.service.PostRedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PostRedisServiceImpl implements PostRedisService {

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private PostMapper postMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisTemplate redisTemplate;

/*----------------------------------帖子详情缓存方法-------------------------------------------*/

//查单个帖子的详情
    @Override
    public Post queryPostByIdFromRedis(Long postId){
        String key = Constants.REDIS_POST_INFO + postId;
        String result = redisUtils.get(key);
        if (result == null || "null".equals(result)) {
            return null;
        }
        Post post;
        try{
            post = objectMapper.readValue(result,Post.class);
            if(post.getStatus()==-1){
                throw new BusinessException("帖子已被删除");
            }
        }catch (Exception e){
            throw new BusinessException("json反序列化失败");
        }
        return post;
    }

//批量查询缓存中的帖子详情
    @Override
    public RedisPostQueryDto batchQueryPostInfoWithCacheFallback(List<Long> postIds) {
        RedisPostQueryDto redisPostQueryDto = new RedisPostQueryDto();
        List<Post> result = new ArrayList<>();
        if (postIds == null || postIds.isEmpty()) return redisPostQueryDto;


        // 1. 构造 key 列表
        List<String> redisKeys = postIds.stream()
                .map(id -> Constants.REDIS_POST_INFO + id)
                .collect(Collectors.toList());

        // 2. 批量获取 Redis 值（JSON 字符串）
        List<String> values = redisUtils.multiGet(redisKeys);

        // 3. 解析成功的值
        List<Long> missPostIds = new ArrayList<>();
        for (int i = 0; i < postIds.size(); i++) {
            String json = values.get(i);
            if (json == null || "null".equals(json)) {
                missPostIds.add(postIds.get(i));
                continue;
            }
            try {
                Post post = objectMapper.readValue(json, Post.class);
                result.add(post);
            } catch (Exception e) {
                log.warn("Failed to deserialize post from Redis, key={}, value={}", redisKeys.get(i), json, e);
            }
        }
        redisPostQueryDto.setPosts(result);
        if (!missPostIds.isEmpty()) {
            redisPostQueryDto.setMissedPostIds(missPostIds);
        }
        return redisPostQueryDto;
    }

//查论坛名的前十个帖子id
    @Override
    public List<Long> queryPostIdsByForumFromRedis(String forumName){
        // 倒序取分数最高的前10条，保证最新评论在前
        Set<String> result =  redisUtils.zReverseRange(Constants.REDIS_POST_LIST_FORUM + forumName, 0, 9);
        if (result == null || result.isEmpty()) {
            return null;
        }
        List<Long> postIds = new ArrayList<>();
        result.forEach(postId -> postIds.add(Long.parseLong(postId)));
        return postIds;
    }

//查热帖的id，共五页
    @Override
    public List<Long> queryPostIdsByHotFromRedis(int page){
        if(page>5){
            return null;
        }
        long start = (long) (page - 1) * 10L;
        long end = start + 9;
        Set<String> result =  redisUtils.zReverseRange(Constants.REDIS_POST_LIST_HOT, start, end);
        if(result==null||result.isEmpty()){
            return null;
        }
        List<Long> postIds = new ArrayList<>();
        result.forEach(postId->{
            postIds.add(Long.parseLong(postId));
        });
        return postIds;
    }

//添加单个帖子到redis中
    @Override
    public void addPostToRedis(Post post) {
        String value;
        try{
            value = objectMapper.writeValueAsString(post);
        }catch (Exception e){
            e.printStackTrace();
            return;
        }
        int baseDays = 3;
        int randomOffset = ThreadLocalRandom.current().nextInt(-1, 2);
        int expireDays = baseDays + randomOffset;
        redisUtils.setEx(Constants.REDIS_POST_INFO+post.getId(),value, (long) Constants.TIME_MILLIS_DAY *expireDays, TimeUnit.MILLISECONDS);
    }

//更新论坛名的帖子
    @Override
    public void cachePostIdsByForum(String forumName, List<Post> posts) {
        if (posts == null || posts.isEmpty()) return;
        String key = Constants.REDIS_POST_LIST_FORUM + forumName;
        for (Post post : posts) {
            double timestamp = post.getLastCommentTime().toInstant(ZoneOffset.UTC).toEpochMilli();
            redisUtils.zAdd(key, String.valueOf(post.getId()), timestamp);
        }
        // 保留 score 最大的前10个（从高到低），删除多余的（低分项）
        long total = redisUtils.zCount(key,0, Double.MAX_VALUE);
        if (total > 10) {
            redisUtils.zRemoveRange(key, 0, total - 11); // 删除 rank 最低的（score 最小）
        }
    }
//更新单个论坛的新帖子
    @Override
    public void cachePostIdByForum(String forumName,Post post){
        if(post==null) return;
        String key = Constants.REDIS_POST_LIST_FORUM + forumName;
        double timestamp = post.getLastCommentTime().toInstant(ZoneOffset.UTC).toEpochMilli();
        redisUtils.zAdd(key, String.valueOf(post.getId()), timestamp);
        // 保留 score 最大的前10个（从高到低），删除多余的（低分项）
        long total = redisUtils.zCount(key,0, Double.MAX_VALUE);
        if (total > 10) {
            redisUtils.zRemoveRange(key, 0, total - 11); // 删除 rank 最低的（score 最小）
        }
    }

/*----------------------------------计数信息缓存方法-------------------------------------------*/

//查单个帖子的计数信息
    @Override
    public PostCountDto queryPostCountFromRedis(Long postId) {
        String key = Constants.REDIS_POST_COUNT + postId;
        Map<Object, Object> rawMap = redisUtils.hGetAll(key);
        if (rawMap == null || rawMap.isEmpty()) return null;

        Map<String, String> map = new HashMap<>();
        for (Map.Entry<Object, Object> entry : rawMap.entrySet()) {
            map.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }

        PostCountDto dto = new PostCountDto();
        try {
            dto.setLikeCount(Integer.parseInt(map.getOrDefault("like", "0")));
            dto.setDislikeCount(Integer.parseInt(map.getOrDefault("dislike", "0")));
            dto.setPageViewCount(Long.parseLong(map.getOrDefault("view", "0")));
            dto.setCommentCount(Integer.parseInt(map.getOrDefault("comment", "0")));

            String ts = map.get("lastCommentTime");
            if (ts != null) {
                long epochMillis = Long.parseLong(ts);
                dto.setLastCommentTime(LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC));
            }
        } catch (Exception e) {
            log.warn("Failed to parse PostCountDto from Redis hash, key={}, map={}", key, map, e);
            return null;
        }

        return dto;
    }

//更改帖子的计数信息
    @Override
    public void changeCount(Long postId,String type,int delta){
        ensurePostCountCached(postId);
        redisUtils.hIncrBy(Constants.REDIS_POST_COUNT+postId,type,delta);
    }
    @Override
    public void setLastCommentTime(Long postId, LocalDateTime lastCommentTime){
        ensurePostCountCached(postId);
        String timestamp = String.valueOf(lastCommentTime.toInstant(ZoneOffset.UTC).toEpochMilli());
        redisUtils.hPut(Constants.REDIS_POST_COUNT + postId, "lastCommentTime", timestamp);
    }

//添加帖子计数信息到redis中
    @Override
    public void addPostCountToRedis(Post post){

        if(post==null) return;

        String key = Constants.REDIS_POST_COUNT + post.getId();

        Map<String, String> countMap = new HashMap<>();
        countMap.put("like", String.valueOf(post.getLikeCount()));
        countMap.put("dislike", String.valueOf(post.getDislikeCount()));
        countMap.put("view", String.valueOf(post.getPageViewCount()));
        countMap.put("comment", String.valueOf(post.getCommentCount()));
        countMap.put("lastCommentTime", String.valueOf(
                post.getLastCommentTime() != null
                        ? post.getLastCommentTime().toInstant(ZoneOffset.UTC).toEpochMilli()
                        : System.currentTimeMillis()
        ));

        redisUtils.hPutAll(key, countMap);
        redisUtils.expire(key, Constants.TIME_MILLIS_DAY * 2, TimeUnit.MILLISECONDS);
    }

//检查缓存中的计数信息存在，不存在则添加
    private void ensurePostCountCached(Long postId){
        Boolean exists = redisUtils.hasKey(Constants.REDIS_POST_COUNT+postId);

        if (exists == null || !exists){
            List<Post> post = postMapper.queryPostById(postId);
            if (post == null|| post.isEmpty()) {
                log.warn("PostCount not found for postId={}", postId);
                return;
            }
            addPostCountToRedis(post.get(0));
        }
    }

/*----------------------------------帖子内容缓存方法-------------------------------------------*/

    @Override
    public void addPostContentToRedis(PostContent postContent) {
        if (postContent == null || postContent.getContent() == null) return;
        Long postId = postContent.getPostId();
        String key = Constants.REDIS_POST_CONTENT + postId;
        String value = postContent.getContent();
        redisUtils.setEx(key, value, (long) Constants.TIME_MILLIS_DAY * 2, TimeUnit.MILLISECONDS);
    }

    @Override
    public PostContent queryPostContentFromRedis(Long postId) {
        String key = Constants.REDIS_POST_CONTENT + postId;
        String content = redisUtils.get(key);
        if (content == null || "null".equals(content)) {
            return null;
        }
        PostContent postContent = new PostContent();
        postContent.setContent(content);
        postContent.setPostId(postId);
        return postContent;
    }

}
