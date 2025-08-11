package com.aoe4Forum.service.impl;

import com.aoe4Forum.entity.Comment;
import com.aoe4Forum.entity.constans.Constants;
import com.aoe4Forum.entity.dto.CommentNoticeDto;
import com.aoe4Forum.entity.dto.LikeNoticeDto;
import com.aoe4Forum.entity.request.CommentRequest;
import com.aoe4Forum.mapper.CommentMapper;
import com.aoe4Forum.mapper.PostMapper;
import com.aoe4Forum.redis.RedisUtils;
import com.aoe4Forum.service.CommentService;
import com.aoe4Forum.service.PostRedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.aoe4Forum.entity.dto.TokenUserInfoDto;
import com.aoe4Forum.component.RedisComponent;

@Service
public class CommentServiceImpl implements CommentService {

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private PostMapper postMapper;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private PostRedisService postRedisService;

    @Resource
    RedisUtils redisUtils;

    @Resource
    private RedisComponent redisComponent;

    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Comment createComment(CommentRequest commentRequest){
        Comment comment = new Comment();
        comment.setPostId(commentRequest.getPostId());
        comment.setParentId(commentRequest.getParentId());
        comment.setCommentTime(LocalDateTime.now());
        comment.setLikeCount(0);
        comment.setContent(commentRequest.getContent());
        comment.setStatus(0);
        comment.setUsername(commentRequest.getUsername());
        comment.setUserId(commentRequest.getUserId());
        comment.setRepliedUsername(commentRequest.getRepliedUsername());
        comment.setRepliedUserId(commentRequest.getRepliedUserId());
        comment.setChildCount(0);
        
        // 设置评论用户的头像
        try {
            TokenUserInfoDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(commentRequest.getToken());
            comment.setAvatar(tokenUserInfoDto.getAvatar());
        } catch (Exception e) {
            // 如果获取头像失败，使用默认头像
            comment.setAvatar("/defaultImg/ottomans.png");
        }
        commentMapper.insert(comment);
        if(comment.getParentId()!=-1){
            changeCommentChildCount(comment.getParentId(),1);
        }else{
            changePostCommentCount(comment.getPostId(),1);
        }
        if(commentRequest.getRepliedUserId().equals(comment.getUserId())){
            return comment;
        }
        CommentNoticeDto commentNoticeDto =  new CommentNoticeDto();
        commentNoticeDto.setCommentId(comment.getCommentId());
        commentNoticeDto.setRepliedUserId(commentRequest.getRepliedUserId());
        commentNoticeDto.setUserId(commentRequest.getUserId());
        commentNoticeDto.setPostId(comment.getPostId()); // 设置帖子ID
        String json = null;
        try {
            json = objectMapper.writeValueAsString(commentNoticeDto);
        } catch (Exception e) {
            return comment;
        }
        rabbitTemplate.convertAndSend("notice.exchange","notice.comment",json);
        
        return comment;
    }

    @Override
    public void deleteComment(CommentRequest commentRequest) {
//        查询当前评论是否是父评论
        Comment comment = commentMapper.queryCommentById(commentRequest.getCommentId()).get(0);
        if(tokenAuthUserNComment(comment.getUserId(),commentRequest.getUserId())){
            throw new RuntimeException("无权限删除该评论");
        }
        if(verifyComment(comment)){
            throw new RuntimeException("评论已被删除");
        }
//        如果是父评论，就删除所有该父的子评论
        if(comment.getParentId()==-1){
            commentMapper.deleteCommentWithChildren(comment.getCommentId());
            changePostCommentCount(comment.getPostId(),-1);
        }else{
            commentMapper.delete(commentRequest.getCommentId());
            changeCommentChildCount(commentRequest.getParentId(),-1);
        }
    }

    @Override
    public Comment getComment(Long commentId) {
        List<Comment> comments = commentMapper.queryCommentById(commentId);
        if(comments.isEmpty()){
            throw new RuntimeException("没有找到对应评论");
        }
        return comments.get(0);
    }

    @Override
    public List<Comment> getCommentsByPostId(Long postId,int offset,int limit) {
        List<Comment> comments = commentMapper.queryCommentByPostId(postId,offset,limit);
        return comments;
    }

    @Override
    public List<Comment> getCommentsByParentId(Long parentId,int offset,int limit){
        return commentMapper.queryCommentByParentId(parentId,offset,limit);
    }


    @Override
    public Map<Long,List<Comment>> getCommentsByParentIds(List<Long> parentIds,int limit) {
        if(parentIds.isEmpty()){
            throw new RuntimeException("查询列表为空");
        }
        List<Comment> comments = commentMapper.queryCommentByParentIds(parentIds);
        if(comments.isEmpty()){
            return null;
        }
        Map<Long, List<Comment>> grouped = comments.stream()
                .collect(Collectors.groupingBy(Comment::getParentId));
        Map<Long,List<Comment>> result = new HashMap<>();
        for (Map.Entry<Long, List<Comment>> entry : grouped.entrySet()) {
            // 由于SQL已经按时间排序，这里只需要取前limit条
            List<Comment> sortedComments = entry.getValue().stream()
                    .limit(limit)
                    .collect(Collectors.toList());
            result.put(entry.getKey(), sortedComments);
        }
        return result;
    }

    private void changeCommentChildCount(Long commentId,Integer delta){
        commentMapper.changeChildCount(commentId,delta);
    }

    private void changePostCommentCount(Long postId,Integer delta){
        postRedisService.changeCount(postId,"comment",delta);
        // 更新最新评论时间
        LocalDateTime now = LocalDateTime.now();
        postRedisService.setLastCommentTime(postId, now);

        // 同步更新论坛帖子有序集合的分数，保证按最新评论时间排序
        try {
            String forum;
            // 优先从缓存中取论坛名
            com.aoe4Forum.entity.Post cachedPost = postRedisService.queryPostByIdFromRedis(postId);
            if (cachedPost != null && cachedPost.getForum() != null) {
                forum = cachedPost.getForum();
            } else {
                // 回源数据库获取论坛名
                List<com.aoe4Forum.entity.Post> posts = postMapper.queryPostById(postId);
                if (posts == null || posts.isEmpty()) return;
                forum = posts.get(0).getForum();
            }
            com.aoe4Forum.entity.Post temp = new com.aoe4Forum.entity.Post();
            temp.setId(postId);
            temp.setLastCommentTime(now);
            // 更新对应板块与 all 的有序集合分数
            postRedisService.cachePostIdByForum(forum, temp);
            postRedisService.cachePostIdByForum("all", temp);
        } catch (Exception ignored) {
        }
    }

    boolean verifyComment(Comment comment){
        return comment.getStatus()!=-1;
    }

    //    鉴权，确定帖子的拥有者
    public boolean tokenAuthUserNComment(Long userIdInToken,Long userId){
//        TODO 可能需要添加管理员的权限
        return !Objects.equals(userIdInToken, userId);
    }

    @Override
    public void likeComment(CommentRequest commentRequest) {
        Long commentId = commentRequest.getCommentId();
        Long userId = commentRequest.getUserId();
        String likeKey = Constants.COMMENT_LIKE_LIST + commentId;
        String userValue = String.valueOf(userId);

        // 分布式锁：防止并发问题（key可设为"like:lock:"+postId+":"+userId）
        String lockKey = "commentLike:lock:" + commentId + ":" + userId;
        try {
            // 尝试获取锁（5秒超时，10秒自动释放）
            boolean locked = redisUtils.tryLock(lockKey, 5000);
            if (!locked) {
                throw new RuntimeException("操作太频繁，请稍后再试");
            }

            // 2. 处理点赞状态
            boolean isLiked = redisUtils.sIsMember(likeKey, userValue);
            if (isLiked) {
                // 已点赞：取消点赞（点赞数减1，从集合移除）
                commentMapper.likeComment(commentId,-1);
                redisUtils.sRemove(likeKey, userValue);
            } else {
                // 未点赞：新增点赞（点赞数加1，加入集合）
                commentMapper.likeComment(commentId,1);
                redisUtils.sAdd(likeKey, userValue);
                // 设置过期时间（如7天，避免Redis内存溢出）
                redisUtils.expire(likeKey, 7, TimeUnit.DAYS);
                LikeNoticeDto noticeDTO = new LikeNoticeDto();
                noticeDTO.setBusinessId(commentId);
                noticeDTO.setSenderId(userId);
                noticeDTO.setSenderName(commentRequest.getUsername());
                noticeDTO.setBusinessType("comment");
                String json = null;
                try {
                    json = objectMapper.writeValueAsString(noticeDTO);
                } catch (Exception e) {
                    return;
                }
                rabbitTemplate.convertAndSend("notice.exchange","notice.like",json);
            }
        } finally {
            // 释放锁
            redisUtils.unlock(lockKey);
        }
    }

    @Override
    public List<Comment> getCommentsByIds(List<Long> ids){
        return commentMapper.queryCommentByIds(ids);
    }
}
