package com.aoe4Forum.service.impl;

import com.aoe4Forum.entity.ForumStatus;
import com.aoe4Forum.entity.constans.Constants;
import com.aoe4Forum.mapper.ForumStatusMapper;
import com.aoe4Forum.redis.RedisUtils;
import com.aoe4Forum.service.StatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class StatusServiceImpl implements StatusService {

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private ForumStatusMapper forumStatusMapper;

    @Override
    public ForumStatus getStatus() {
        String key = Constants.REDIS_FORUM_STATUS;
        Map<Object, Object> rawMap = redisUtils.hGetAll(key);
        if (rawMap == null || rawMap.isEmpty()){
            init();
            rawMap = redisUtils.hGetAll(key);
            if(rawMap == null || rawMap.isEmpty()){
                throw new RuntimeException("统计信息缓存读取错误");
            }
        }
        ForumStatus forumStatus = new ForumStatus();
        Map<String, String> map = new HashMap<>();

        for (Map.Entry<Object, Object> entry : rawMap.entrySet()) {
            map.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }

        try {
            forumStatus.setCommentCount(Long.parseLong(map.getOrDefault("comment","0")));
            forumStatus.setPostCount(Long.parseLong(map.getOrDefault("post","0")));
            forumStatus.setViewCount(Long.parseLong(map.getOrDefault("view","0")));

        } catch (Exception e) {
            log.warn("Failed to parse PostCountDto from Redis hash, key={}, map={}", key, map, e);
            return null;
        }

        return forumStatus;
    }


    @Override
    public void changeCount(String type){
        redisUtils.hIncrBy(Constants.REDIS_FORUM_STATUS,type,1);
        System.out.println(type+"change ok");
    }

    @Scheduled(fixedRate = 6000*60)
    private void freshStatus(){
        ForumStatus forumStatus = getStatus();
        if(forumStatus == null) {
            log.error("论坛统计信息更新失败");
            return;
        }
        forumStatusMapper.insertStatus(forumStatus);
    }

    private void init(){
        ForumStatus forumStatus = forumStatusMapper.getInitStatus();
        if(forumStatus == null){
            log.error("初始化失败");
            return;
        }
        String key = Constants.REDIS_FORUM_STATUS;
        Map<String, String> countMap = new HashMap<>();
        countMap.put("view", String.valueOf(forumStatus.getViewCount()));
        countMap.put("comment", String.valueOf(forumStatus.getCommentCount()));
        countMap.put("post", String.valueOf(forumStatus.getPostCount()));
        redisUtils.hPutAll(key, countMap);
    }

}
