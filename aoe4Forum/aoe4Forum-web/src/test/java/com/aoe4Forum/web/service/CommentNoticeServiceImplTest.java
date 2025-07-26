package com.aoe4Forum.web.service;

import com.aoe4Forum.entity.constans.Constants;
import com.aoe4Forum.entity.notice.CommentNotice;
import com.aoe4Forum.entity.notice.NoticeCursorPageRequest;
import com.aoe4Forum.entity.notice.NoticeQueryResult;
import com.aoe4Forum.mapper.CommentNoticeMapper;
import com.aoe4Forum.redis.RedisUtils;
import com.aoe4Forum.service.impl.CommentNoticeServiceImpl;
import org.apache.ibatis.annotations.ResultMap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
@Transactional // 测试后自动回滚数据库操作
public class CommentNoticeServiceImplTest {

    @Autowired
    private CommentNoticeServiceImpl commentNoticeService;

    @Autowired
    private CommentNoticeMapper commentNoticeMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedisUtils redisUtils;

    // 测试插入评论通知（数据库+Redis）
    @Test
    public void testInsert() {
        // 1. 准备测试数据
        CommentNotice notice = new CommentNotice();
        notice.setId(1001L);
        notice.setUserId(5001L); // 接收通知的用户ID
        notice.setBusinessId(6001L); // 评论对应的业务ID（如帖子ID）
        notice.setCreateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)); // 截断毫秒

        // 2. 执行插入操作
        commentNoticeService.insert(notice);

        // 4. 验证Redis插入
        String redisKey = "notice:CommentNotice:" + notice.getUserId(); // 对应generateRedisKey逻辑
        Set<String> redisValues = redisUtils.zRange(redisKey, 0, -1);
        System.out.println(redisValues);
        Assert.assertNotNull("Redis键不存在", redisValues);
        // 验证Redis存储的值是否正确（匹配encodeValueForRedis逻辑）
        String expectedValue = notice.getId() + ":" + notice.getBusinessId(); // 对应encode方法
        System.out.println(expectedValue);
        Assert.assertTrue("Redis未存储目标值", redisValues.contains(expectedValue));
    }

    // 测试批量查询通知（优先Redis，空则查库并同步）
    @Test
    public void testBatchQueryNotices() {
        // 1. 准备测试数据
        Long userId = 5002L;
        CommentNotice notice1 = createTestNotice(1002L, userId, 6002L);
        CommentNotice notice2 = createTestNotice(1003L, userId, 6003L);
        commentNoticeService.insert(notice1);
        commentNoticeService.insert(notice2);

        // 2. 清空Redis，确保首次查询从数据库获取
        String redisKey = "notice:CommentNotice:" + userId;
        redisTemplate.delete(redisKey);

        // 3. 执行查询
        NoticeCursorPageRequest request = new NoticeCursorPageRequest();
        request.setUserId(userId);
        request.setPageSize(10);
        request.setNoticeName("CommentNotice");

        NoticeQueryResult<CommentNotice> result = commentNoticeService.batchQueryNotices(request);
        Assert.assertNotNull("查询结果为空", result);
        Assert.assertEquals("查询数量不正确", 2, result.getNotices().size());

        // 4. 验证Redis同步
        Set<Object> redisValues = redisTemplate.opsForZSet().range(redisKey, 0, -1);
        Assert.assertEquals("Redis同步数量不匹配", 2, redisValues.size());

        // 5. 验证分页游标
        CommentNotice lastNotice = result.getNotices().get(1); // 按时间倒序，第二条是较早的
        Assert.assertEquals("最后ID不匹配", lastNotice.getId(), result.getLastId());

        // 时间允许1秒内误差
        long timeDiff = ChronoUnit.SECONDS.between(lastNotice.getCreateTime(), result.getLastCreateTime());
        Assert.assertTrue("最后时间不匹配", Math.abs(timeDiff) <= 1);
    }

    // 测试Redis分页查询逻辑
    @Test
    public void testQueryNoticeFromRedis() {
        // 1. 准备测试数据（3条不同时间的通知）
        Long userId = 5003L;
        LocalDateTime baseTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        CommentNotice notice1 = createTestNotice(1004L, userId, 6004L);
        notice1.setCreateTime(baseTime.minusMinutes(10)); // 最早
        commentNoticeService.insert(notice1);

        CommentNotice notice2 = createTestNotice(1005L, userId, 6005L);
        notice2.setCreateTime(baseTime.minusMinutes(5)); // 中间
        commentNoticeService.insert(notice2);

        CommentNotice notice3 = createTestNotice(1006L, userId, 6006L);
        notice3.setCreateTime(baseTime); // 最新
        commentNoticeService.insert(notice3);

        // 2. 第一页查询（pageSize=2，应返回最新的2条：notice3、notice2）
        NoticeCursorPageRequest request = new NoticeCursorPageRequest();
        request.setUserId(userId);
        request.setPageSize(2);
        request.setNoticeName("CommentNotice");

        List<CommentNotice> firstPage = commentNoticeService.queryNoticeFromRedis(request);
        System.out.println(firstPage);
        Assert.assertEquals("第一页数量不正确", 2, firstPage.size());
        Assert.assertEquals("第一页第一条应为最新通知", notice3.getId(), firstPage.get(0).getId());

        // 3. 第二页查询（基于第一页游标，应返回notice1）
        request.setLastId(firstPage.get(1).getId()); // 第一页最后一条是notice2
        request.setLastCreateTime(firstPage.get(1).getCreateTime());

        List<CommentNotice> secondPage = commentNoticeService.queryNoticeFromRedis(request);
        Assert.assertEquals("第二页数量不正确", 1, secondPage.size());
        Assert.assertEquals("第二页数据不匹配", notice1.getId(), secondPage.get(0).getId());
    }

    // 测试批量推送Redis（pipeline）
    @Test
    public void testBatchPushNoticesByPipeline() {
        Long userId = 5004L;
        // Java 8用Arrays.asList代替List.of
        List<CommentNotice> notices = Arrays.asList(
                createTestNotice(1007L, userId, 6007L),
                createTestNotice(1008L, userId, 6008L)
        );

        // 执行批量推送
        commentNoticeService.batchPushNoticesByPipeline(notices);
        // 验证Redis
        String redisKey = "notice:CommentNotice:" + userId;
        Long redisSize = redisTemplate.opsForZSet().size(redisKey);
        Assert.assertEquals("批量推送数量不正确", 2L, redisSize.longValue());
    }

    // 测试Redis过期时间刷新
    @Test
    public void testFreshExpire() {
        Long userId = 5005L;
        String redisKey = "notice:CommentNotice:" + userId;

        // 先设置一个短期过期时间
        redisTemplate.opsForValue().set(redisKey, "test");
        redisTemplate.expire(redisKey, 1, TimeUnit.MINUTES);

        // 执行刷新过期时间
        commentNoticeService.freshExpire(userId, "CommentNotice");

        // 验证过期时间已延长（大于1分钟）
        Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.MINUTES);
        Assert.assertTrue("过期时间未刷新", ttl > 1);
    }

    // 测试读取游标存储
    @Test
    public void testSaveNoticeReadCursor() {
        Long userId = 5006L;
        String cursorKey = Constants.USER_READ_CURSOR + "CommentNotice:" + userId;

        // 执行存储游标
        commentNoticeService.saveNoticeReadCursor(userId, "CommentNotice");

        // 验证游标值（时间戳）
        String cursorValue = (String) redisTemplate.opsForValue().get(cursorKey);
        Assert.assertNotNull("游标未存储", cursorValue);

        // 验证时间戳在合理范围内（1分钟内）
        long storedTimestamp = Long.parseLong(cursorValue);
        long currentTimestamp = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Assert.assertTrue("游标时间戳异常", currentTimestamp - storedTimestamp < 60_000); // 1分钟内
    }

    // 辅助方法：创建测试通知
    private CommentNotice createTestNotice(Long id, Long userId, Long businessId) {
        CommentNotice notice = new CommentNotice();
        notice.setId(id);
        notice.setUserId(userId);
        notice.setBusinessId(businessId);
        notice.setCreateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        return notice;
    }
}