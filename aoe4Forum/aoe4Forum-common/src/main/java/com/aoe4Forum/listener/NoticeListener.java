package com.aoe4Forum.listener;


import com.aoe4Forum.entity.Post;
import com.aoe4Forum.entity.notice.CommentNotice;
import com.aoe4Forum.entity.notice.FollowNotice;
import com.aoe4Forum.entity.notice.LikeNotice;
import com.aoe4Forum.service.CommentService;
import com.aoe4Forum.service.NoticeService;
import com.aoe4Forum.service.PostService;
import com.aoe4Forum.utils.SnowflakeIdGenerator;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class NoticeListener {

    @Autowired
    private NoticeService<LikeNotice> likeNoticeService;

    @Autowired
    private NoticeService<CommentNotice> commentNoticeService;

    @Autowired
    private NoticeService<FollowNotice> followNoticeService;

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;


    private static final SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(1, 1);

    @RabbitListener(queues = "notice.like.queue")
    public void handleLikePush(String likeNoticeData) {
        if(likeNoticeData ==null){
            return;
        }
        String[] temp  = likeNoticeData.split(":");
        if(temp.length!=4&&temp.length!=5){
            return;
        }
        Long businessId = Long.parseLong(temp[0]);
        Long senderId = Long.parseLong(temp[1]);
        String senderName = temp[2];
        String businessType = temp[3];

        LikeNotice likeNotice = new LikeNotice();
        likeNotice.setBusinessId(businessId);
        likeNotice.setSenderId(senderId);
        likeNotice.setBusinessType(businessType);
        likeNotice.setCreateTime(LocalDateTime.now());
        likeNotice.setId(idGenerator.nextId());
        likeNotice.setSenderName(senderName);

        if(businessType.equals("post")){
            Post post = postService.queryPostById(businessId);
            likeNotice.setUserId(post.getUserId());
        }else if(businessType.equals("comment")){
            if (temp.length != 5) {
                return; // 格式错误，跳过
            }
            likeNotice.setUserId(Long.parseLong(temp[4]));
        }
        likeNoticeService.insert(likeNotice);
    }

    @RabbitListener(queues = "notice.comment.queue")
    public void handleCommentPush(String commentNoticeData) {
        if(commentNoticeData==null){
            return;
        }

        String[] parts = commentNoticeData.split(":");
        if (parts.length < 3) return;

        Long businessId = Long.parseLong(parts[0]);
        Long userId = Long.parseLong(parts[1]);
        Long senderId = Long.parseLong(parts[2]);

        CommentNotice notice = commentNoticeService.createNotice(businessId, idGenerator.nextId(), LocalDateTime.now());
        notice.setUserId(userId);
        notice.setSenderId(senderId);
        commentNoticeService.insert(notice); // 调用CommentNoticeServiceImpl的insert

    }

    @RabbitListener(queues = "notice.follow.queue")
    public void handleFollowPush(String idAndUserId) {
        if (idAndUserId == null) return;

        String[] parts = idAndUserId.split(":");
        if (parts.length < 2) return;

        Long businessId = Long.parseLong(parts[0]);
        Long userId = Long.parseLong(parts[1]);

        FollowNotice notice = followNoticeService.createNotice(businessId, idGenerator.nextId(), LocalDateTime.now());
        notice.setUserId(userId);
        followNoticeService.insert(notice);
    }
}
