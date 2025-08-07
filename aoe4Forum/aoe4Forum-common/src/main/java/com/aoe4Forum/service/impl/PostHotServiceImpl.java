package com.aoe4Forum.service.impl;

import com.aoe4Forum.entity.constans.Constants;
import com.aoe4Forum.entity.dto.PostCountDto;
import com.aoe4Forum.mapper.PostMapper;
import com.aoe4Forum.redis.RedisUtils;
import com.aoe4Forum.service.PostHotService;
import com.aoe4Forum.service.PostRedisService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PostHotServiceImpl implements PostHotService {

    @Resource
    private RedisTemplate<String,String> redisTemplate;

    @Resource
    private PostRedisService postRedisService;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    PostMapper postMapper;

    /*----------------------------------热帖缓存方法-------------------------------------------*/

    @PostConstruct
    public void initHotPostOnStartup() {
        log.info("🚀 Spring Boot 启动完成，正在初始化热帖榜...");
        freshHotPost();
    }

    @Scheduled(fixedRate = 60 * 1000 * 10) // 每 5 分钟刷新一次
    public void scheduleHotPostRefresh() {
        freshHotPost();
    }

    @Scheduled(fixedRate = 60 * 1000 * 5)
    private void savePostCountToDB(){
        savePostCount();
    }

    private double hotScoreCal(PostCountDto dto) {
        int like = dto.getLikeCount() == null ? 0 : dto.getLikeCount();
        int dislike = dto.getDislikeCount() == null ? 0 : dto.getDislikeCount();
        int comment = dto.getCommentCount() == null ? 0 : dto.getCommentCount();
        long view = dto.getPageViewCount() == null ? 0L : dto.getPageViewCount();
        LocalDateTime lastCommentTime = dto.getLastCommentTime();

        // --- 1. 活跃度部分（加权评分） ---
        double score = 1.0 * like
                - 0.5 * dislike
                + 1.5 * comment
                + 0.001 * view;

        // --- 2. 时间衰减因子（越新越热）---
        long hoursSinceLastComment = ChronoUnit.HOURS.between(lastCommentTime, LocalDateTime.now());
        double timeDecay = 1.0 / (1 + hoursSinceLastComment); // 越近越接近1，越远越趋近0

        return score * timeDecay;
    }

    private Set<Long> scanPostIdsWithCountCache() {
        Set<Long> postIds = new HashSet<>();
        String pattern = Constants.REDIS_POST_COUNT + "*";

        // 注意不要用 KEYS，要用 SCAN 防止阻塞
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(1000).build();
        Cursor<byte[]> cursor = redisTemplate.getConnectionFactory().getConnection().scan(options);

        while (cursor.hasNext()) {
            String fullKey = new String(cursor.next()); // 例如 post:count:123
            String postIdStr = fullKey.substring(Constants.REDIS_POST_COUNT.length());
            try {
                postIds.add(Long.parseLong(postIdStr));
            } catch (NumberFormatException ignored) {
            }
        }

        return postIds;
    }

    @Override
    public void freshHotPost() {
        String key = Constants.REDIS_POST_LIST_HOT;
        Set<Long> postIds = scanPostIdsWithCountCache();
        if (postIds == null || postIds.isEmpty()) return;

        List<Pair<String, Double>> postScoreList = new ArrayList<>();

        for (Long postId : postIds) {
            PostCountDto dto = postRedisService.queryPostCountFromRedis(postId);
            if (dto == null) continue;

            double score = hotScoreCal(dto);
            postScoreList.add(Pair.of(String.valueOf(postId), score));
        }

        postScoreList.sort((a, b) -> Double.compare(b.getRight(), a.getRight()));
        List<Pair<String, Double>> top50 = postScoreList.stream()
                .limit(50)
                .collect(Collectors.toList());

        // 设置 ZSet 过期时间（可选）
        redisUtils.delete(key);

        for (Pair<String, Double> pair : top50) {
            redisUtils.zAdd(key, pair.getLeft(), pair.getRight());
        }
        redisUtils.expire(key, 60, TimeUnit.MINUTES);
        log.info("🔥 热度榜更新完成，Top {} 条写入成功", top50.size());
    }

    @Override
    public void savePostCount(){
        Set<Long> postIds = scanPostIdsWithCountCache();
        List<PostCountDto> list = new ArrayList<>();
        for (Long postId : postIds) {
            PostCountDto dto = postRedisService.queryPostCountFromRedis(postId);
            if (dto != null) {
                PostCountDto updateDto = new PostCountDto();
                updateDto.setPostId(postId);
                updateDto.setLikeCount(dto.getLikeCount());
                updateDto.setDislikeCount(dto.getDislikeCount());
                updateDto.setCommentCount(dto.getCommentCount());
                updateDto.setPageViewCount(dto.getPageViewCount());
                updateDto.setLastCommentTime(dto.getLastCommentTime());
                list.add(updateDto);
            }
        }
        postMapper.batchUpdatePostCounts(list);
        log.info("帖子计数持久化完毕，{} 条写入成功", list.size());
    }

}
