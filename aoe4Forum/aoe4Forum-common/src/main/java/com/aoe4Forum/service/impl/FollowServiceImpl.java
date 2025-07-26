package com.aoe4Forum.service.impl;

import com.aoe4Forum.component.RedisComponent;
import com.aoe4Forum.entity.FollowRelation;
import com.aoe4Forum.entity.User;
import com.aoe4Forum.entity.request.FollowCursorPageRequest;
import com.aoe4Forum.entity.dto.FollowQueryResult;
import com.aoe4Forum.entity.dto.FollowUserInfo;
import com.aoe4Forum.entity.dto.UserInfoDto;
import com.aoe4Forum.mapper.FollowRelationMapper;
import com.aoe4Forum.mapper.UserMapper;
import com.aoe4Forum.redis.RedisUtils;
import com.aoe4Forum.service.FollowBloomFilterService;
import com.aoe4Forum.service.FollowService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class FollowServiceImpl implements FollowService {

    @Resource
    private FollowBloomFilterService followBloomFilterService;

    @Resource
    private FollowRelationMapper followRelationMapper;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private UserMapper userMapper;

    //    查询userid关注的人
    @Override
    public FollowQueryResult queryFollowee(FollowCursorPageRequest request) {
            List<Long> followeeIds = new ArrayList<>();
            LocalDateTime lastCreateTime = null;
            Long lastId = null;

    //        未命中缓存查sql
            List<FollowRelation> followRelations = followRelationMapper.queryByFollower(request);
            if (followRelations == null || followRelations.isEmpty()) {
                return null;
            }
            List<Long> notInRedis = new ArrayList<>();
            List<FollowUserInfo> followUserInfos = new ArrayList<>();
            for(FollowRelation followRelation : followRelations){
                UserInfoDto user = redisComponent.getUserInfo(followRelation.getFollowee());
                if(user == null){
                    notInRedis.add(followRelation.getFollowee());
                    continue;
                }
                FollowUserInfo followUserInfo = new FollowUserInfo();
                followUserInfo.setId(user.getId());
                followUserInfo.setAvatar(user.getAvatar());
                followUserInfo.setUsername(user.getUsername());
                followUserInfos.add(followUserInfo);
            }

            if(!notInRedis.isEmpty()){
                List<User> users = userMapper.batchQueryById(notInRedis);
                for(User user : users){
                    FollowUserInfo followUserInfo = new FollowUserInfo();
                    followUserInfo.setId(user.getId());
                    followUserInfo.setAvatar(user.getAvatar());
                    followUserInfo.setUsername(user.getName());
                    followUserInfos.add(followUserInfo);
                    redisComponent.saveUserInfo(user);
                }
        }
    //        返回
            FollowRelation last = followRelations.get(followRelations.size() - 1);
            lastCreateTime = last.getCreateTime();
            lastId = last.getId();
            //  返回
            return buildQueryResult(followUserInfos,lastCreateTime,lastId);
    }

// 查询userid的粉丝
    @Override
    public FollowQueryResult queryFollower(FollowCursorPageRequest request) {
        List<Long> followerIds = new ArrayList<>();
        LocalDateTime lastCreateTime;
        Long lastId;
            //        未命中缓存查sql
        List<FollowRelation> followRelations = followRelationMapper.queryByFollowee(request);
        if (followRelations == null || followRelations.isEmpty()) {
            return null;
        }
        List<Long> notInRedis = new ArrayList<>();
        List<FollowUserInfo> followUserInfos = new ArrayList<>();
        for(FollowRelation followRelation : followRelations){
            UserInfoDto user = redisComponent.getUserInfo(followRelation.getFollower());
            if(user == null){
                notInRedis.add(followRelation.getFollower());
                continue;
            }
            FollowUserInfo followUserInfo = new FollowUserInfo();
            followUserInfo.setId(user.getId());
            followUserInfo.setAvatar(user.getAvatar());
            followUserInfo.setUsername(user.getUsername());
            followUserInfos.add(followUserInfo);
        }
        if(!notInRedis.isEmpty()){
            List<User> users = userMapper.batchQueryById(notInRedis);
            for(User user : users){
                FollowUserInfo followUserInfo = new FollowUserInfo();
                followUserInfo.setId(user.getId());
                followUserInfo.setAvatar(user.getAvatar());
                followUserInfo.setUsername(user.getName());
                followUserInfos.add(followUserInfo);
                redisComponent.saveUserInfo(user);
            }
        }

        FollowRelation last = followRelations.get(followRelations.size() - 1);
        lastCreateTime = last.getCreateTime();
        lastId = last.getId();
        //  返回
        return buildQueryResult(followUserInfos,lastCreateTime,lastId);
    }

    @Override
    public int addFollower(FollowRelation relation) {
        //       关注检查
        int followStatus = checkFollowRelation(relation.getFollower(),relation.getFollowee());
        if(followStatus == 0){
            return 0;
        }
        else if(followStatus == -1){
            followRelationMapper.insertFollowRelation(relation);
        }else{
            followRelationMapper.restoreFollowRelation(relation);
        }

        followBloomFilterService.addFollow(relation.getFollower(), relation.getFollowee());

        userMapper.updateFolloweeNumsDelta(relation.getFollower(), 1);  // 主动关注者：关注数 +1
        userMapper.updateFollowerNumsDelta(relation.getFollowee(), 1);  // 被关注者：粉丝数 +1

        redisComponent.cleanUserInfo(relation.getFollower());
        redisComponent.cleanUserInfo(relation.getFollowee());
//      TODO发送被关注通知消息
        String idAndUserId = relation.getFollower()+":"+relation.getFollowee();
        rabbitTemplate.convertAndSend("notice.exchange", "notice.follow",idAndUserId);
        return 0;
    }

    @Override
    public int deleteFollower(FollowRelation relation) {
        //       关注检查
        int followStatus = checkFollowRelation(relation.getFollower(),relation.getFollowee());
        if(followStatus == -1||followStatus == 1){
            return 0;
        }
        followRelationMapper.deleteFollowRelation(relation);

        userMapper.updateFolloweeNumsDelta(relation.getFollower(), -1);  // 主动关注者：关注数 -1
        userMapper.updateFollowerNumsDelta(relation.getFollowee(), -1);  // 被关注者：粉丝数 -1

        redisComponent.cleanUserInfo(relation.getFollower());
        redisComponent.cleanUserInfo(relation.getFollowee());

        return 0;
    }


    @Override
    public int checkFollower(FollowRelation relation) {
        return checkFollowRelation(relation.getFollower(), relation.getFollowee());
    }

    public int checkFollowRelation(Long followerId, Long followeeId) {
        // 1. 布隆过滤器快速判断
        if (!followBloomFilterService.mightBeFollowing(followerId, followeeId)) {
            return -1;
        }
        // 2.查询数据库
        List<FollowRelation> followRelations = followRelationMapper.queryByFollowerAndFollowee(followerId, followeeId);
        if(followRelations.isEmpty()){
            return -1;
        }
        return followRelations.get(0).getDeleteMark();
    }


    private FollowQueryResult buildQueryResult(List<FollowUserInfo> followUserInfos, LocalDateTime time, Long id) {
        FollowQueryResult result = new FollowQueryResult();
        result.setFollowers(followUserInfos);
        result.setLastCreateTime(time);
        result.setLastId(id);
        return result;
    }
}
