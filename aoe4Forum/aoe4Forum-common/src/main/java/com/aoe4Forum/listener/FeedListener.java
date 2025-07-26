package com.aoe4Forum.listener;


import com.aoe4Forum.entity.Feed;
import com.aoe4Forum.entity.request.FollowCursorPageRequest;
import com.aoe4Forum.entity.dto.FollowQueryResult;
import com.aoe4Forum.entity.dto.FollowUserInfo;
import com.aoe4Forum.service.impl.FeedServiceImpl;
import com.aoe4Forum.service.impl.FollowServiceImpl;
import com.aoe4Forum.utils.SnowflakeIdGenerator;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class FeedListener {

    @Resource
    private FeedServiceImpl feedService;
    @Resource
    private FollowServiceImpl followService;

    private static final SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(1, 1);
//    在一个人发帖后，接受信息。
    @RabbitListener(queues = "feed.push.queue")
    public void handleFeedPush(String idAndUserId) {
        if(idAndUserId==null){
            return;
        }
        String[] temp  = idAndUserId.split(":");
        if(temp.length!=2){
            return;
        }
        Long id = Long.parseLong(temp[0]);
        Long userId = Long.parseLong(temp[1]);


        FollowCursorPageRequest request = new FollowCursorPageRequest();
        request.setPageSize(1000);
        request.setLastCreateTime(null);
        request.setUserId(userId);
        request.setLastId(null);
        FollowQueryResult result = followService.queryFollower(request);
        LocalDateTime time = LocalDateTime.now();
        while(result!=null && !result.getFollowers().isEmpty()){
            List<Feed> feeds = new ArrayList<>();
            for(FollowUserInfo followUserInfo:result.getFollowers()){
                Feed feed = new Feed();
                feed.setId(idGenerator.nextId());
                feed.setStatus(0);
                feed.setSenderId(userId);
                feed.setCreateTime(time);
                feed.setPostId(id);
                feed.setUserId(followUserInfo.getId());
                feeds.add(feed);
            }
            feedService.batchPushFeed2Follower(feeds);
//            更新游标
            request.setLastId(result.getLastId());
            request.setLastCreateTime(result.getLastCreateTime());
//            查询下一批
            result = followService.queryFollower(request);
        }
    }

    @RabbitListener(queues = "feed.push.dlx.queue")
    public void handleDeadLetter(Message message) {
        System.err.println("⚠️ 死信消息捕获: " + new String(message.getBody()));
    }

}
