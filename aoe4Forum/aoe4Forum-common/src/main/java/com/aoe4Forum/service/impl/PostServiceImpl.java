package com.aoe4Forum.service.impl;

import com.aoe4Forum.component.RedisComponent;
import com.aoe4Forum.entity.Post;
import com.aoe4Forum.entity.PostContent;
import com.aoe4Forum.entity.constans.Constants;
import com.aoe4Forum.entity.request.CreatePostRequest;
import com.aoe4Forum.entity.request.PostRequest;
import com.aoe4Forum.entity.request.QueryPostRequest;
import com.aoe4Forum.entity.dto.TokenUserInfoDto;
import com.aoe4Forum.exception.BusinessException;
import com.aoe4Forum.mapper.PostContentMapper;
import com.aoe4Forum.mapper.PostMapper;
import com.aoe4Forum.redis.RedisUtils;
import com.aoe4Forum.service.PostService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class PostServiceImpl implements PostService {

    @Resource
    private PostMapper postMapper;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private PostContentMapper postContentMapper;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisUtils redisUtils;


    @Override
    public void createPost(CreatePostRequest createPostRequest) {
//        打包post对象
        Post post = new Post();
        post.setTitle(createPostRequest.getTitle());
        post.setUuid(UUID.randomUUID().toString());
        post.setCreateTime(LocalDateTime.now());
        post.setForum(createPostRequest.getForum());
        post.setCommentCount(0);
        post.setDislikeCount(0);
        post.setLikeCount(0);
        post.setLastCommentTime(LocalDateTime.now());
        post.setStatus(0);
        post.setPageViewCount(0L);
        post.setUpdateTime(LocalDateTime.now());
        post.setUserName(createPostRequest.getUsername());
        post.setUserId(createPostRequest.getUserId());
        String temp = getPostAbstract(createPostRequest.getContent());
        if(temp==null){
            post.setPostAbstract(createPostRequest.getTitle());
        }else{
            post.setPostAbstract(temp);
        }
        try{
            TokenUserInfoDto tokenUserInfoDto =  redisComponent.getTokenUserInfoDto(createPostRequest.getToken());
            post.setUserId(tokenUserInfoDto.getId());
            post.setUserName(tokenUserInfoDto.getName());
        }catch (Exception e){
            throw new  BusinessException("token无效");
        }
//        插入post对象
        postMapper.insert(post);
        long postId = post.getId();
        if(postId==0){
            throw new  BusinessException("插入失败，获取postId失败");
        }
//        打包postContent对象
        PostContent postContent = new PostContent();
        postContent.setPostId(postId);
        postContent.setContent(createPostRequest.getContent());

//        插入postContent对象
        postContentMapper.insert(postContent);
        String idAndUserId = post.getId()+":"+post.getUserId();
        rabbitTemplate.convertAndSend("feed.exchange", "feed.push",idAndUserId);
    }
    @Override
    public void deletePost(PostRequest postRequest) {
//        鉴权，是否有权限删
        if(tokenAuthUserNPost(postRequest.getUserId(), postRequest.getPostId(),-1L)){
            throw new  BusinessException("无权限");
        }
//        逻辑删除帖子
        postMapper.deleteById(postRequest.getPostId());
    }
    @Override
    public void updatePost(PostRequest postRequest) {
        //        鉴权，是否有权限改
        Post post = postMapper.queryPostById(postRequest.getPostId()).get(0);
        if(tokenAuthUserNPost(postRequest.getUserId(),null, post.getUserId())){
            throw new  BusinessException("无权限");
        }
        if(post.getStatus()==-1){
            throw new  BusinessException("帖子已经被删除");
        }
        post.setUpdateTime(LocalDateTime.now());
        postMapper.updateById(post);
        PostContent postContent = new PostContent();
        postContent.setContent(postRequest.getContent());
        postContent.setPostId(postRequest.getPostId());
        postContentMapper.updateByPostId(postContent);
    }
//根据板块查询
    @Override
    public List<Post> queryPostByForum(QueryPostRequest queryPostRequest) {
       return postMapper.queryPostByForum(queryPostRequest.getForum(), queryPostRequest.getOffset(),queryPostRequest.getLimit());
    }

//根据帖子热度查询
//    TODO 需要实现对应的Mapper，还未实现
    @Override
    public List<Post> queryPostByHot(QueryPostRequest queryPostRequest) {
        return null;
    }

    @Override
    public List<Post> batchQueryPostByIds(List<Long> postIds){
        return postMapper.queryPostByIds(postIds);
    }

    @Override
    public Post queryPostById(Long postId) {
        List<Post> posts = postMapper.queryPostById(postId);
        if(posts==null || posts.size()==0){
            throw new  BusinessException("没有查询到该帖子");
        }
        Post post = posts.get(0);
        if(post.getStatus()==-1){
            throw new  BusinessException("帖子已被删除");
        }

        return post;
    }

    @Override
    public void likePost(PostRequest postRequest) {
        Long postId = postRequest.getPostId();
        Long userId = postRequest.getUserId();
        String likeKey = Constants.POST_LIKE_LIST + postId;
        String dislikeKey = Constants.POST_DISLIKE_LIST + postId;
        String userValue = String.valueOf(userId);

        // 分布式锁：防止并发问题（key可设为"like:lock:"+postId+":"+userId）
        String lockKey = "postLike:lock:" + postId + ":" + userId;
        try {
            // 尝试获取锁（5秒超时，10秒自动释放）
            boolean locked = redisUtils.tryLock(lockKey, 5000);
            if (!locked) {
                throw new RuntimeException("操作太频繁，请稍后再试");
            }

            // 1. 检查是否已点踩，若有则先取消点踩
            boolean isDisliked = redisUtils.sIsMember(dislikeKey, userValue);
            if (isDisliked) {
                // 点踩数减1
                postMapper.postDislike(postId, -1);
                // 从点踩集合移除
                redisUtils.sRemove(dislikeKey, userValue);
            }

            // 2. 处理点赞状态
            boolean isLiked = redisUtils.sIsMember(likeKey, userValue);
            if (isLiked) {
                // 已点赞：取消点赞（点赞数减1，从集合移除）
                postMapper.postLike(postId, -1);
                redisUtils.sRemove(likeKey, userValue);
            } else {
                // 未点赞：新增点赞（点赞数加1，加入集合）
                postMapper.postLike(postId, 1);
                redisUtils.sAdd(likeKey, userValue);
                // 设置过期时间（如7天，避免Redis内存溢出）
                redisUtils.expire(likeKey, 7, TimeUnit.DAYS);
                String idAndUserId = postId+":"+userId+":"+postRequest.getUsername()+":post";
                rabbitTemplate.convertAndSend("notice.exchange","notice.like",idAndUserId);
            }
        } finally {
            // 释放锁
            redisUtils.unlock(lockKey);
        }
    }

    @Override
    public void dislikePost(PostRequest postRequest) {
        Long postId = postRequest.getPostId();
        Long userId = postRequest.getUserId();
        String likeKey = Constants.POST_LIKE_LIST + postId;
        String dislikeKey = Constants.POST_DISLIKE_LIST + postId;
        String userValue = String.valueOf(userId);

        // 分布式锁：防止并发问题
        String lockKey = "dislike:lock:" + postId + ":" + userId;
        try {
            boolean locked = redisUtils.tryLock(lockKey, 5000);
            if (!locked) {
                throw new RuntimeException("操作太频繁，请稍后再试");
            }

            // 1. 检查是否已点赞，若有则先取消点赞
            boolean isLiked = redisUtils.sIsMember(likeKey, userValue);
            if (isLiked) {
                // 点赞数减1
                postMapper.postLike(postId, -1);
                // 从点赞集合移除
                redisUtils.sRemove(likeKey, userValue);
            }

            // 2. 处理点踩状态
            boolean isDisliked = redisUtils.sIsMember(dislikeKey, userValue);
            if (isDisliked) {
                // 已点踩：取消点踩（点踩数减1，从集合移除）
                postMapper.postDislike(postId, -1);
                redisUtils.sRemove(dislikeKey, userValue);
            } else {
                // 未点踩：新增点踩（点踩数加1，加入集合）
                postMapper.postDislike(postId, 1);
                redisUtils.sAdd(dislikeKey, userValue);
                // 设置过期时间
                redisUtils.expire(dislikeKey, 7, TimeUnit.DAYS);
            }
        } finally {
            // 释放锁
            redisUtils.unlock(lockKey);
        }
    }


    //查询帖子实际内容
    public PostContent queryPostContent(long postId){
        Post post = postMapper.queryPostById(postId).get(0);
        if(post.getStatus()==-1){
            throw new  BusinessException("帖子已经被删除");
        }
        List<PostContent> postContent = postContentMapper.queryPostContentByPostId(postId);
        if(postContent.isEmpty()){
            throw new  BusinessException("没有找到帖子");
        }
        return postContent.get(0);
    }


//    鉴权，确定帖子的拥有者
    public boolean tokenAuthUserNPost(Long userIdInToken,Long postId,Long userId){
        if(userId==-1){
            List<Post> post = postMapper.queryPostById(postId);
            userId = post.get(0).getUserId();
        }
//        TODO 可能需要添加管理员的权限
        return !Objects.equals(userIdInToken, userId);
    }

    private String getPostAbstract(String content){
        if (content == null) return " ";
        // 去除HTML标签
        String plainText = content.replaceAll("<[^>]*>", "");
        // 去除多余空白
        plainText = plainText.replaceAll("\\s+", "");
        // 截取前50个字符
        return plainText.length() > 50 ? plainText.substring(0, 50) : plainText;
    }
}
