package com.aoe4Forum.service.impl;

import com.aoe4Forum.entity.Comment;
import com.aoe4Forum.entity.constans.Constants;
import com.aoe4Forum.entity.request.CommentRequest;
import com.aoe4Forum.mapper.CommentMapper;
import com.aoe4Forum.mapper.PostMapper;
import com.aoe4Forum.redis.RedisUtils;
import com.aoe4Forum.service.CommentService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService {

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private PostMapper postMapper;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    RedisUtils redisUtils;

    @Override
    public void createComment(CommentRequest commentRequest){
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
        commentMapper.insert(comment);
        if(comment.getParentId()!=-1){
            changeCommentChildCount(comment.getParentId(),1);
        }else{
            changePostCommentCount(comment.getPostId(),1);
        }

        String idAndUserid = comment.getCommentId()+":"+comment.getRepliedUserId()+":"+comment.getUserId();
        rabbitTemplate.convertAndSend("notice.exchange","notice.comment",idAndUserid);
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
            result.put(entry.getKey(), entry.getValue().stream().limit(limit).collect(Collectors.toList()));
        }
        return result;
    }

    private void changeCommentChildCount(Long commentId,Integer delta){
        commentMapper.changeChildCount(commentId,delta);
    }

    private void changePostCommentCount(Long postId,Integer delta){
        postMapper.changeCommentCount(postId,delta);
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
                String idAndUserId = commentId +":"+userId+":"+commentRequest.getUsername()+":comment:"+commentRequest.getRepliedUserId();
                rabbitTemplate.convertAndSend("notice.exchange","notice.like",idAndUserId);
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
