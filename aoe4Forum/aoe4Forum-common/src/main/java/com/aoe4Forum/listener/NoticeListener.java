package com.aoe4Forum.listener;


import com.aoe4Forum.entity.Comment;
import com.aoe4Forum.entity.Post;
import com.aoe4Forum.entity.dto.CommentNoticeDto;
import com.aoe4Forum.entity.dto.FollowNoticeDto;
import com.aoe4Forum.entity.dto.LikeNoticeDto;
import com.aoe4Forum.entity.notice.CommentNotice;
import com.aoe4Forum.entity.notice.FollowNotice;
import com.aoe4Forum.entity.notice.LikeNotice;
import com.aoe4Forum.mapper.LikeNoticeMapper;
import com.aoe4Forum.service.CommentService;
import com.aoe4Forum.service.NoticeService;
import com.aoe4Forum.service.PostService;
import com.aoe4Forum.service.impl.CommentNoticeServiceImpl;
import com.aoe4Forum.utils.SnowflakeIdGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class NoticeListener {

    @Autowired
    private NoticeService<LikeNotice> likeNoticeService;

    @Autowired
    private CommentNoticeServiceImpl commentNoticeService;

    @Autowired
    private NoticeService<FollowNotice> followNoticeService;

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    ObjectMapper objectMapper = new ObjectMapper();

    private static final SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(1, 1);

    @Autowired
    private LikeNoticeMapper likeNoticeMapper;

    @RabbitListener(queues = "notice.like.queue")
    public void handleLikePush(String likeNoticeData) {
        if(likeNoticeData ==null){
            return;
        }
        LikeNoticeDto dto = null;
        try{
            dto = objectMapper.readValue(likeNoticeData, LikeNoticeDto.class);
        }catch (Exception e){
            return;
        }
        if(dto == null){
            return;
        }

        LikeNotice likeNotice = new LikeNotice();
        likeNotice.setBusinessId(dto.getBusinessId());
        likeNotice.setSenderId(dto.getSenderId());
        likeNotice.setBusinessType(dto.getBusinessType());
        likeNotice.setCreateTime(LocalDateTime.now());
        likeNotice.setId(idGenerator.nextId());
        likeNotice.setSenderName(dto.getSenderName());
        Long userId = 0L;
        if(dto.getBusinessType().equals("post")){
            Post post = postService.queryPostById(dto.getBusinessId());
            likeNotice.setUserId(post.getUserId());
            userId = post.getUserId();
        }else if(dto.getBusinessType().equals("comment")){
            Comment comment = commentService.getComment(dto.getBusinessId());
            likeNotice.setUserId(comment.getUserId());
            userId = comment.getUserId();
        }
        Boolean status = likeNoticeMapper.QueryIds(dto.getSenderId(),dto.getBusinessType(),userId,dto.getBusinessId());
        if(status||dto.getSenderId().equals(userId)){
            return;
        }
        likeNoticeService.insert(likeNotice);
    }

    @RabbitListener(queues = "notice.comment.queue")
    public void handleCommentPush(String commentNoticeData) {
        if(commentNoticeData==null){
            return;
        }
        CommentNoticeDto dto = null;
        try{
            dto = objectMapper.readValue(commentNoticeData, CommentNoticeDto.class);
        }catch (Exception e){
            return;
        }
        if(dto == null){
            return;
        }

        Long businessId = dto.getCommentId();
        Long userId = dto.getRepliedUserId();
        Long senderId = dto.getUserId();
        Long postId = dto.getPostId(); // 获取帖子ID

        CommentNotice notice = commentNoticeService.createNoticeWithPostId(businessId, idGenerator.nextId(), LocalDateTime.now(), postId);
        notice.setUserId(userId);
        notice.setSenderId(senderId);
        commentNoticeService.insert(notice);

    }

    @RabbitListener(queues = "notice.follow.queue")
    public void handleFollowPush(String idAndUserId) {
        if (idAndUserId == null) return;
        FollowNoticeDto dto = null;
        try{
            dto = objectMapper.readValue(idAndUserId,FollowNoticeDto.class);
        }catch (Exception e){
            return;
        }
        if(dto == null){
            return;
        }

        Long businessId = dto.getUserId();
        Long userId = dto.getRepliedUserId();

        FollowNotice notice = followNoticeService.createNotice(businessId, idGenerator.nextId(), LocalDateTime.now());
        notice.setUserId(userId);
        followNoticeService.insert(notice);
    }
}
