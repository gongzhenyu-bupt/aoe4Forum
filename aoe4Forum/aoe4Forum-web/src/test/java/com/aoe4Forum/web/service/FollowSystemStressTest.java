package com.aoe4Forum.web.service;

import com.aoe4Forum.entity.FollowRelation;
import com.aoe4Forum.entity.request.FollowCursorPageRequest;
import com.aoe4Forum.entity.dto.FollowQueryResult;
import com.aoe4Forum.service.FollowService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;

@SpringBootTest
@RunWith(SpringRunner.class)
public class FollowSystemStressTest {

    @Autowired
    private FollowService followService;

    private static final int USER_COUNT = 1000;
    private static final int FOLLOW_EACH = 100;

    /**
     * 模拟 USER_COUNT 个用户，每人关注 FOLLOW_EACH 个其他用户
     */
    @Test
    public void testMassiveFollowOperations() {
        for (long i = 1; i <= USER_COUNT; i++) {
            for (long j = i + 1; j <= i + FOLLOW_EACH && j <= USER_COUNT; j++) {
                System.out.println(i);
                System.out.println(j);
                FollowRelation relation = new FollowRelation();
                relation.setFollower(i);
                relation.setFollowee(j);
                relation.setCreateTime(LocalDateTime.now());
                followService.addFollower(relation);
            }
        }
    }

    /**
     * 随机分页查询某用户关注列表
     */
    @Test
    public void testQueryFollowee() {
        for (long uid = 1; uid <= 100; uid++) {
            FollowCursorPageRequest request = new FollowCursorPageRequest();
            request.setUserId(uid);
            FollowQueryResult result = followService.queryFollowee(request);
            System.out.printf("User %d followees: %s\n", uid, result.getFollowers());
        }
    }

    /**
     * 校验某些关注关系是否存在
     */
    @Test
    public void testCheckRelation() {
        for (long i = 1; i <= 100; i++) {
            for (long j = i + 1; j <= i + 10 && j <= USER_COUNT; j++) {
                FollowRelation relation = new FollowRelation();
                relation.setFollower(i);
                relation.setFollowee(j);
                if (followService.checkFollower(relation)==0){
                    System.out.printf(" %d follow %d\n",i,j);
                }else{
                    System.out.printf(" %d not follow %d\n",i,j);
                }
            }
        }
    }
}
