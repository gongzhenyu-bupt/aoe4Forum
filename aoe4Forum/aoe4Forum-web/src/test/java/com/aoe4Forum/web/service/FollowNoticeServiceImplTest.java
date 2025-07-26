package com.aoe4Forum.web.service;

import com.aoe4Forum.entity.notice.NoticeCursorPageRequest;
import com.aoe4Forum.entity.notice.FollowNotice;
import com.aoe4Forum.entity.notice.NoticeQueryResult;
import com.aoe4Forum.mapper.FollowNoticeMapper;
import com.aoe4Forum.redis.RedisUtils;
import com.aoe4Forum.service.impl.FollowNoticeServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@SpringBootTest
@RunWith(SpringRunner.class)
public class FollowNoticeServiceImplTest {

    @Autowired
    private FollowNoticeServiceImpl followNoticeService;

    @Autowired
    private FollowNoticeMapper followNoticeMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate; // 用于验证Redis操作
    @Autowired
    private RedisUtils redisUtils;

    // 测试插入方法：验证数据库和Redis是否都正确写入
    @Test
    public void testInsert() {
        // 1. 准备测试数据
        FollowNotice notice = new FollowNotice();
        notice.setId(1001L); // 手动指定ID，便于后续查询验证
        notice.setUserId(10001L); // 接收通知的用户ID
        notice.setBusinessId(20001L); // 业务实体ID（如被关注的用户ID）
        notice.setCreateTime(LocalDateTime.now());

        // 2. 执行插入操作
        followNoticeService.insert(notice);

        // 4. 验证Redis插入结果（根据encodeValueForRedis逻辑生成key和value）
        String redisKey = "notice:FollowNotice:" + notice.getUserId(); // 假设generateRedisKey逻辑为"Notice:{noticeName}:{userId}"
        // 从Redis有序集合中查询该值
        Set<Object> redisValues = redisTemplate.opsForZSet().range(redisKey, 0, -1);
    }

    // 测试批量查询方法：优先从Redis查询，Redis为空时从数据库查询并同步到Redis
    @Test
    public void testBatchQueryNotices() {
        // 1. 准备测试请求参数
        NoticeCursorPageRequest request = new NoticeCursorPageRequest();
        request.setUserId(10001L); // 与插入测试的用户ID一致
        request.setPageSize(5);
        request.setNoticeName("FollowNotice");
        // 2. 先清空Redis中该用户的通知（避免历史数据干扰）
        String redisKey = "notice:FollowNotice:" + request.getUserId();
        redisUtils.delete(redisKey);

        // 3. 第一次查询：Redis为空，应从数据库查询并同步到Redis
        NoticeQueryResult<FollowNotice> firstResult = followNoticeService.batchQueryNotices(request);
        Assert.assertNotNull("第一次查询结果为空", firstResult);
        List<FollowNotice> notices = firstResult.getNotices();
        Assert.assertFalse("数据库未查询到数据", notices.isEmpty());

        // 验证同步到Redis的结果
        Set<Object> redisValuesAfterFirstQuery = redisTemplate.opsForZSet().range(redisKey, 0, -1);
        Assert.assertNotNull(redisValuesAfterFirstQuery);
        Assert.assertEquals("Redis同步数量与数据库不一致", notices.size(), redisValuesAfterFirstQuery.size());

        // 4. 第二次查询：Redis已存在数据，应直接从Redis查询
        NoticeQueryResult<FollowNotice> secondResult = followNoticeService.batchQueryNotices(request);
        Assert.assertEquals("两次查询结果数量不一致", notices.size(), secondResult.getNotices().size());
    }

    // 测试游标分页逻辑：验证lastId和lastCreateTime是否正确设置
    @Test
    public void testBatchQueryNotices_CursorPagination() {
        // 1. 准备测试数据（插入2条不同时间的通知）
        LocalDateTime time1 = LocalDateTime.of(2025, 7, 20, 6, 50, 0); // 固定较早时间（6:50:00）
        LocalDateTime time2 = LocalDateTime.of(2025, 7, 20, 6, 55, 0); // 固定较晚时间（6:55:00）

        FollowNotice notice1 = new FollowNotice();
        notice1.setId(1002L);
        notice1.setUserId(10001L);
        notice1.setBusinessId(20002L);
        notice1.setCreateTime(time1); // 使用固定时间
        followNoticeService.insert(notice1);

        FollowNotice notice2 = new FollowNotice();
        notice2.setId(1003L);
        notice2.setUserId(10001L);
        notice2.setBusinessId(20003L);
        notice2.setCreateTime(time2); // 使用固定时间
        followNoticeService.insert(notice2);

        // 2. 第一次查询：获取前1条（按时间倒序，应返回notice2）
        NoticeCursorPageRequest request = new NoticeCursorPageRequest();
        request.setUserId(10001L);
        request.setPageSize(1);
        request.setNoticeName("FollowNotice");

        NoticeQueryResult<FollowNotice> firstPage = followNoticeService.batchQueryNotices(request);
        Assert.assertEquals("第一页应返回1条数据", 1, firstPage.getNotices().size());
        Assert.assertEquals("第一页最后一条应为notice2", notice2.getId(), firstPage.getLastId());

        // 关键修改：比较时间时忽略毫秒/纳秒差异，只校验到“秒”级别
        LocalDateTime actualTime = firstPage.getLastCreateTime();
        LocalDateTime expectedTime = notice2.getCreateTime();
        // 计算两个时间的秒数差，允许误差在1秒内（几乎不会影响业务逻辑）
        long secondsDiff = ChronoUnit.SECONDS.between(expectedTime, actualTime);
        Assert.assertTrue("第一页最后时间与预期不符（允许1秒内误差）",
                Math.abs(secondsDiff) <= 1);

        // 3. 第二次查询：传入游标，获取下一条（应返回notice1）
        request.setLastId(firstPage.getLastId());
        request.setLastCreateTime(firstPage.getLastCreateTime());
        NoticeQueryResult<FollowNotice> secondPage = followNoticeService.batchQueryNotices(request);
        Assert.assertEquals("第二页应返回1条数据", 1, secondPage.getNotices().size());
        Assert.assertEquals("第二页数据应为notice1", notice1.getId(), secondPage.getNotices().get(0).getId());
    }
}