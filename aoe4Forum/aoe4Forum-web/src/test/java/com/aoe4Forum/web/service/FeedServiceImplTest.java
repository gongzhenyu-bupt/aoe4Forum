package com.aoe4Forum.web.service;

import com.aoe4Forum.entity.Feed;
import com.aoe4Forum.entity.request.FeedCursorPageRequest;
import com.aoe4Forum.entity.dto.FeedQueryResult;
import com.aoe4Forum.service.impl.FeedServiceImpl;
import com.aoe4Forum.utils.SnowflakeIdGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
public class FeedServiceImplTest {

    @Autowired
    private FeedServiceImpl feedService;

    private static final SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(1, 1);


    @Test
    public void testBatchPushAndQueryFeeds() throws InterruptedException {
        Long testUserId = 10086L;
        Long senderId = 1L;

        // 1️⃣ 构造 Feed 列表
        List<Feed> feeds = new ArrayList<>();
        for (long i = 1; i <= 5; i++) {
            Feed feed = new Feed();
            feed.setId(idGenerator.nextId());
            feed.setUserId(testUserId);           // 被推送用户
            feed.setPostId(1000L + i);            // 模拟帖子的 id
            feed.setSenderId(senderId);           // 发送者
            feed.setCreateTime(LocalDateTime.now().minusSeconds(i)); // 模拟不同时间
            feed.setStatus(0);
            feeds.add(feed);
        }

        // 2️⃣ 批量推送 Feed
        feedService.batchPushFeed2Follower(feeds);
        System.out.println("✅ 已推送 Feed 到 Redis 和数据库");

        // 等待 0.5s 确保 Redis 有数据写入
        Thread.sleep(500);

        // 3️⃣ 查询 Feed（第一页）
        FeedCursorPageRequest request = new FeedCursorPageRequest();
        request.setUserId(testUserId);
        request.setPageSize(3); // 分页大小

        FeedQueryResult result1 = feedService.batchQueryFeeds(request);
        System.out.println("✅ 第1页结果：" + result1.getFeeds().size() + " 条");
        result1.getFeeds().forEach(feed -> {
            System.out.println(feed.getPostId() + " | " + feed.getCreateTime());
        });

        // 4️⃣ 查询 Feed（第二页）
        request.setLastId(result1.getLastId());
        request.setLastCreateTime(result1.getLastCreateTime());

        FeedQueryResult result2 = feedService.batchQueryFeeds(request);
        System.out.println("✅ 第2页结果：" + result2.getFeeds().size() + " 条");
        result2.getFeeds().forEach(feed -> {
            System.out.println(feed.getPostId() + " | " + feed.getCreateTime());
        });
    }
}
