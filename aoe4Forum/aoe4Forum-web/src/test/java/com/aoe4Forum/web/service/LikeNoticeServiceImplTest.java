package com.aoe4Forum.web.service;

import com.aoe4Forum.entity.notice.LikeNotice;
import com.aoe4Forum.entity.notice.NoticeCursorPageRequest;
import com.aoe4Forum.entity.notice.NoticeQueryResult;
import com.aoe4Forum.mapper.LikeNoticeMapper;
import com.aoe4Forum.redis.RedisUtils;
import com.aoe4Forum.service.impl.LikeNoticeServiceImpl;
import org.apache.ibatis.annotations.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@RunWith(SpringRunner.class)
@Transactional
public class LikeNoticeServiceImplTest {

    @Autowired
    private LikeNoticeServiceImpl likeNoticeService;

    @Autowired
    private LikeNoticeMapper likeNoticeMapper;

    @Resource
    private RedisUtils redisUtils;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 测试插入通知（数据库+Redis）
    @Test
    public void testInsert() {
        // 1. 准备测试数据
        LikeNotice notice = new LikeNotice();
        notice.setId(1001L);
        notice.setUserId(20001L); // 接收通知的用户ID
        notice.setBusinessId(30001L); // 被点赞的业务ID
        notice.setSenderId(40001L); // 点赞者ID
        notice.setSenderName("测试用户"); // 点赞者名称
        notice.setCreateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)); // 截断毫秒，避免精度问题

        // 2. 执行插入
        likeNoticeService.insert(notice);
        
        // 4. 验证Redis插入
        String redisKey = "notice:LikeNotice:" + notice.getUserId(); // 对应generateRedisKey逻辑
        Set<String> redisValues = redisUtils.zRange(redisKey, 0, -1);
        Assert.assertNotNull("Redis键不存在", redisValues);
        System.out.println(redisValues);
        // 验证Redis存储的值是否正确（匹配encodeValueForRedis逻辑）
        String expectedValue = notice.getId() + ":" + notice.getBusinessId() + ":" +
                notice.getSenderId() + ":" + notice.getSenderName();
        System.out.println(expectedValue);
        Assert.assertTrue("Redis值不匹配", redisValues.contains(expectedValue));
    }

    // 测试批量查询通知（Redis优先，空则查库并同步）
    @Test
    public void testBatchQueryNotices() {
        // 1. 准备测试数据
        Long userId = 20002L;
        LikeNotice notice1 = createTestNotice(1002L, userId, 30002L, 40002L, "用户A");
        LikeNotice notice2 = createTestNotice(1003L, userId, 30003L, 40003L, "用户B");
        likeNoticeService.insert(notice1);
        likeNoticeService.insert(notice2);

        // 2. 清空Redis，确保首次查询从数据库获取
        String redisKey = "notice:LikeNotice:" + userId;
        redisTemplate.delete(redisKey);

        // 3. 首次查询
        NoticeCursorPageRequest request = new NoticeCursorPageRequest();
        request.setUserId(userId);
        request.setPageSize(10);
        request.setNoticeName("LikeNotice");

        NoticeQueryResult<LikeNotice> result = likeNoticeService.batchQueryNotices(request);
        Assert.assertNotNull("查询结果为空", result);
        Assert.assertEquals("查询数量不正确", 2, result.getNotices().size());

        // 4. 验证Redis已同步
        Set<Object> redisValues = redisTemplate.opsForZSet().range(redisKey, 0, -1);
        Assert.assertEquals("Redis同步数量不正确", 2, redisValues.size());

        // 5. 验证分页游标
        LikeNotice lastNotice = result.getNotices().get(1);
        Assert.assertEquals("最后ID不正确", lastNotice.getId(), result.getLastId());

        // 时间允许1秒内误差
        long timeDiff = ChronoUnit.SECONDS.between(lastNotice.getCreateTime(), result.getLastCreateTime());
        Assert.assertTrue("最后时间不正确", Math.abs(timeDiff) <= 1);
    }

    // 测试Redis分页查询逻辑
    @Test
    public void testQueryNoticeFromRedis() {
        // 1. 准备测试数据（插入3条不同时间的通知）
        Long userId = 20003L;
        LocalDateTime baseTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        LikeNotice notice1 = createTestNotice(1004L, userId, 30004L, 40004L, "用户C");
        notice1.setCreateTime(baseTime.minusMinutes(5));
        likeNoticeService.insert(notice1);

        LikeNotice notice2 = createTestNotice(1005L, userId, 30005L, 40005L, "用户D");
        notice2.setCreateTime(baseTime.minusMinutes(3));
        likeNoticeService.insert(notice2);

        LikeNotice notice3 = createTestNotice(1006L, userId, 30006L, 40006L, "用户E");
        notice3.setCreateTime(baseTime.minusMinutes(1));
        likeNoticeService.insert(notice3);

        // 2. 第一页查询（取2条，应返回最新的notice3和notice2）
        NoticeCursorPageRequest request = new NoticeCursorPageRequest();
        request.setUserId(userId);
        request.setPageSize(2);
        request.setNoticeName("LikeNotice");

        List<LikeNotice> firstPage = likeNoticeService.queryNoticeFromRedis(request);
        Assert.assertEquals("第一页数量不正确", 2, firstPage.size());
        Assert.assertEquals("第一页第一条应为最新通知", notice3.getId(), firstPage.get(0).getId());

        // 3. 第二页查询（基于第一页游标，应返回notice1）
        request.setLastId(firstPage.get(1).getId());
        request.setLastCreateTime(firstPage.get(1).getCreateTime());

        List<LikeNotice> secondPage = likeNoticeService.queryNoticeFromRedis(request);
        Assert.assertEquals("第二页数量不正确", 1, secondPage.size());
        Assert.assertEquals("第二页数据不正确", notice1.getId(), secondPage.get(0).getId());
    }

    // 测试批量推送Redis（pipeline）
    @Test
    public void testBatchPushNoticesByPipeline() {
        Long userId = 20004L;
        List<LikeNotice> notices = Arrays.asList(
                createTestNotice(1007L, userId, 30007L, 40007L, "用户F"),
                createTestNotice(1008L, userId, 30008L, 40008L, "用户G")
        );

        // 执行批量推送
        likeNoticeService.batchPushNoticesByPipeline(notices);

        // 验证Redis
        String redisKey = "notice:LikeNotice:" + userId;
        Long size = redisTemplate.opsForZSet().size(redisKey);
        Assert.assertEquals("批量推送数量不正确", 2L, size.longValue());
    }

    // 测试过期时间刷新
    @Test
    public void testFreshExpire() {
        Long userId = 20005L;
        String redisKey = "notice:LikeNotice:" + userId;

        // 先设置一个短期过期时间
        redisTemplate.opsForValue().set(redisKey, "test");
        redisTemplate.expire(redisKey, 1, TimeUnit.MINUTES);

        // 执行刷新过期时间
        likeNoticeService.freshExpire(userId, "LikeNotice");

        // 验证过期时间是否延长（预期大于1分钟）
        Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.MINUTES);
        Assert.assertTrue("过期时间未刷新", ttl > 1);
    }

    // 辅助方法：创建测试通知
    private LikeNotice createTestNotice(Long id, Long userId, Long businessId, Long senderId, String senderName) {
        LikeNotice notice = new LikeNotice();
        notice.setId(id);
        notice.setUserId(userId);
        notice.setBusinessId(businessId);
        notice.setSenderId(senderId);
        notice.setSenderName(senderName);
        notice.setCreateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        return notice;
    }
}
