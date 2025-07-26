package com.aoe4Forum.service;

import com.aoe4Forum.entity.FollowRelation;
import com.aoe4Forum.mapper.FollowRelationMapper;
import com.google.common.hash.BloomFilter;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;

@Service
public class FollowBloomFilterService {

    @Resource
    private BloomFilter<String> followBloomFilter;

    @Resource
    private FollowRelationMapper followRelationMapper;

    private static final int BATCH_SIZE = 1000;

    /**
     * 添加关注关系到布隆过滤器
     */
    public void addFollow(Long followerId, Long followeeId) {
        String key = buildFollowKey(followerId, followeeId);
        followBloomFilter.put(key);
    }

    /**
     * 检查是否可能存在关注关系
     */
    public boolean mightBeFollowing(Long followerId, Long followeeId) {
        String key = buildFollowKey(followerId, followeeId);
        return followBloomFilter.mightContain(key);
    }

    @PostConstruct
    public void loadFromDatabase() {
        int offset = 0;
        while (true) {
            List<FollowRelation> batch = followRelationMapper.scanAllFollowRelations(offset, BATCH_SIZE);
            if (batch == null || batch.isEmpty()) break;

            for (FollowRelation relation : batch) {
                followBloomFilter.put(buildFollowKey(relation.getFollower(), relation.getFollowee()));
            }

            offset += BATCH_SIZE;
        }
        System.out.println("✅ 布隆过滤器初始化加载完成");
    }

    private String buildFollowKey(Long followerId, Long followeeId) {
        return "follow:" + followerId + "_" + followeeId;
    }


}