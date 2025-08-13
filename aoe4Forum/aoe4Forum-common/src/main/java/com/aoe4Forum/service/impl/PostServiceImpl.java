package com.aoe4Forum.service.impl;

import com.aoe4Forum.component.RedisComponent;
import com.aoe4Forum.entity.Post;
import com.aoe4Forum.entity.PostContent;
import com.aoe4Forum.entity.PostDocument;
import com.aoe4Forum.entity.PostImage;
import com.aoe4Forum.entity.constans.Constants;
import com.aoe4Forum.entity.dto.LikeNoticeDto;
import com.aoe4Forum.entity.dto.PostCountDto;
import com.aoe4Forum.entity.dto.RedisPostQueryDto;
import com.aoe4Forum.entity.request.CreatePostRequest;
import com.aoe4Forum.entity.request.PostRequest;
import com.aoe4Forum.entity.request.QueryPostRequest;
import com.aoe4Forum.entity.dto.TokenUserInfoDto;
import com.aoe4Forum.exception.BusinessException;
import com.aoe4Forum.mapper.PostContentMapper;
import com.aoe4Forum.mapper.PostImageMapper;
import com.aoe4Forum.mapper.PostMapper;
import com.aoe4Forum.redis.RedisUtils;
import com.aoe4Forum.service.ElasticSearchService;
import com.aoe4Forum.service.PostRedisService;
import com.aoe4Forum.service.PostService;
import com.aoe4Forum.service.StatusService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private PostRedisService postRedisService;

    @Resource
    private PostImageMapper postImageMapper;

    @Resource
    private StatusService statusService;

    ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ElasticSearchService elasticSearchService;

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
            post.setAvatar(tokenUserInfoDto.getAvatar());
        }catch (Exception e){
            throw new  BusinessException("token无效");
        }
//        插入post对象
        postMapper.insert(post);
        long postId = post.getId();
        if(postId==0){
            throw new  BusinessException("插入失败，获取postId失败");
        }
        List<String> imageUrls = extractImageUrls(createPostRequest.getContent());
        for (String imageUrl : imageUrls) {
            PostImage postImage = new PostImage();
            postImage.setPostId(post.getId());
            postImage.setImageUrl(imageUrl);
            String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            postImage.setFileName(fileName);
            postImage.setUploadTime(LocalDateTime.now());
            // ... 设置其他字段
            postImageMapper.insert(postImage);
        }
//        打包postContent对象
        PostContent postContent = new PostContent();
        postContent.setPostId(postId);
        postContent.setContent(createPostRequest.getContent());

        //        插入postContent对象
        postContentMapper.insert(postContent);

        // 缓存新创建的帖子
        postRedisService.addPostToRedis(post);
        postRedisService.addPostCountToRedis(post);
        postRedisService.addPostContentToRedis(postContent);
        postRedisService.cachePostIdByForum(post.getForum(), post);
        postRedisService.cachePostIdByForum("all", post);

//        增加计数
        statusService.changeCount("post");

//      插入es
        PostDocument postDocument = new PostDocument(postId,createPostRequest.getTitle(),createPostRequest.getContent());
        elasticSearchService.save(postDocument);
//        发mq，推送到feed
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
        redisUtils.delete(Constants.REDIS_POST_INFO+postRequest.getPostId());
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
        redisUtils.delete(Constants.REDIS_POST_INFO+postRequest.getPostId());
    }
    //根据板块查询
    @Override
    public List<Post> queryPostByForum(QueryPostRequest queryPostRequest) {
        List<Post> result;

        if(queryPostRequest.getOffset()==0){
            List<Long> postIds = postRedisService.queryPostIdsByForumFromRedis(queryPostRequest.getForum());
//            缓存未命中时写回缓存
            if (postIds == null || postIds.isEmpty()) {
                List<Post> posts = postMapper.queryPostByForum(queryPostRequest.getForum(), queryPostRequest.getOffset(), queryPostRequest.getLimit());
                postRedisService.cachePostIdsByForum(queryPostRequest.getForum(),posts);
                return posts; // 直接返回数据库查询结果
            }
            result = batchQueryPostByIds(postIds);
        }else{
            result = postMapper.queryPostByForum(queryPostRequest.getForum(), queryPostRequest.getOffset(),queryPostRequest.getLimit());
        }
        return result;
    }

    //根据帖子热度查询
    @Override
    public List<Post> queryPostByHot(int page) {
        if(page>5){
            return null;
        }
        List<Long> hotPostIds = postRedisService.queryPostIdsByHotFromRedis(page);
        if (hotPostIds == null || hotPostIds.isEmpty()) return null;
        return batchQueryPostByIds(hotPostIds);
    }

    @Override
    public List<Post> batchQueryPostByIds(List<Long> postIds) {
        RedisPostQueryDto redisPostQueryDto = postRedisService.batchQueryPostInfoWithCacheFallback(postIds);
        List<Post> result = new ArrayList<>(redisPostQueryDto.getPosts());

        // 更新缓存命中的帖子的动态数据
        for (Post post : result) {
            PostCountDto countDto = postRedisService.queryPostCountFromRedis(post.getId());
            if (countDto != null) {
                post.setLikeCount(countDto.getLikeCount());
                post.setDislikeCount(countDto.getDislikeCount());
                post.setCommentCount(countDto.getCommentCount());
                post.setPageViewCount(countDto.getPageViewCount());
                post.setLastCommentTime(countDto.getLastCommentTime());
            }
        }

        // 处理缓存未命中的帖子
        if (redisPostQueryDto.getMissedPostIds() != null && !redisPostQueryDto.getMissedPostIds().isEmpty()) {
            List<Post> missedPosts = postMapper.queryPostByIds(redisPostQueryDto.getMissedPostIds());
            if (missedPosts != null && !missedPosts.isEmpty()) {
                for (Post post : missedPosts) {
                    postRedisService.addPostToRedis(post);
                    postRedisService.addPostCountToRedis(post);
                }
                result.addAll(missedPosts);
            }
        }

        // 构建 Map 方便按传入顺序排序
        Map<Long, Post> postMap = result.stream()
                .collect(Collectors.toMap(Post::getId, p -> p));

        // 按传入 postIds 顺序返回
        List<Post> sortedResult = postIds.stream()
                .map(postMap::get) // 如果某个 id 没查到，这里会是 null
                .filter(Objects::nonNull) // 过滤掉不存在的
                .collect(Collectors.toList());

        return sortedResult;
    }

    @Override
    public Post queryPostById(Long postId) {
        Post post;
        post = postRedisService.queryPostByIdFromRedis(postId);

        if(post != null){
            // 缓存命中，但需要更新动态数据
            PostCountDto countDto = postRedisService.queryPostCountFromRedis(postId);
            if(countDto != null){
                // 更新动态数据
                post.setLikeCount(countDto.getLikeCount());
                post.setDislikeCount(countDto.getDislikeCount());
                post.setCommentCount(countDto.getCommentCount());
                post.setPageViewCount(countDto.getPageViewCount());
                post.setLastCommentTime(countDto.getLastCommentTime());
            }
            return post;
        }
        // 缓存未命中，从数据库查询
        List<Post> posts = postMapper.queryPostById(postId);
        if(posts==null || posts.size()==0){
            throw new  BusinessException("没有查询到该帖子");
        }
        post = posts.get(0);
        if(post.getStatus()==-1){
            throw new  BusinessException("帖子已被删除");
        }

        // 将查询结果缓存
        postRedisService.addPostToRedis(post);
        postRedisService.addPostCountToRedis(post);
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
                throw new BusinessException("操作太频繁");
            }

            // 1. 检查是否已点踩，若有则先取消点踩
            boolean isDisliked = redisUtils.sIsMember(dislikeKey, userValue);
            if (isDisliked) {
                // 点踩数减1
//                postMapper.postDislike(postId, -1);
                postRedisService.changeCount(postId,"dislike",-1);
                // 从点踩集合移除
                redisUtils.sRemove(dislikeKey, userValue);
            }

            // 2. 处理点赞状态
            boolean isLiked = redisUtils.sIsMember(likeKey, userValue);
            if (isLiked) {
                // 已点赞：取消点赞（点赞数减1，从集合移除）
                redisUtils.sRemove(likeKey, userValue);
                postRedisService.changeCount(postId,"like",-1);
            } else {
                // 未点赞：新增点赞（点赞数加1，加入集合）
                redisUtils.sAdd(likeKey, userValue);
                // 设置过期时间（如7天，避免Redis内存溢出）
                redisUtils.expire(likeKey, 7, TimeUnit.DAYS);
                postRedisService.changeCount(postId,"like",1);

                LikeNoticeDto noticeDTO = new LikeNoticeDto();
                noticeDTO.setBusinessId(postId);
                noticeDTO.setSenderId(userId);
                noticeDTO.setSenderName(postRequest.getUsername());
                noticeDTO.setBusinessType("post");
                String json = null;
                try{
                    json = objectMapper.writeValueAsString(noticeDTO);
                }catch (Exception e){
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
//                postMapper.postLike(postId, -1);
                postRedisService.changeCount(postId,"like",-1);
                // 从点赞集合移除
                redisUtils.sRemove(likeKey, userValue);
            }

            // 2. 处理点踩状态
            boolean isDisliked = redisUtils.sIsMember(dislikeKey, userValue);
            if (isDisliked) {
//                postMapper.postDislike(postId, -1);
                postRedisService.changeCount(postId,"dislike",-1);
                redisUtils.sRemove(dislikeKey, userValue);
            } else {
//                postMapper.postDislike(postId, 1);
                postRedisService.changeCount(postId,"dislike",1);
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
        PostContent postContent;
        postContent = postRedisService.queryPostContentFromRedis(postId);
        if(postContent==null){
            List<PostContent> postContents = postContentMapper.queryPostContentByPostId(postId);
            if(postContents.isEmpty()){
                throw new  BusinessException("没有找到帖子");
            }
            postContent = postContents.get(0);
            postRedisService.addPostContentToRedis(postContent);
        }
        postRedisService.changeCount(postId,"view",1);
//        增加计数统计
        statusService.changeCount("view");
        return postContent;
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
        if(content==null||content.isEmpty()){
            return null;
        }
        if(content.length()<=100){
            return content;
        }
        return content.substring(0,100)+"...";
    }
    
    @Override
    public List<Post> queryPostsByUserId(Long userId, int offset, int limit) {
        return postMapper.queryPostsByUserId(userId, offset, limit);
    }
    
    @Override
    public int countPostsByUserId(Long userId) {
        return postMapper.countPostsByUserId(userId);
    }


    private List<String> extractImageUrls(String content) {
        List<String> urls = new ArrayList<>();
        // 使用正则表达式提取img标签的src属性
        Pattern pattern = Pattern.compile("<img[^>]+src=\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            urls.add(matcher.group(1));
        }
        return urls;
    }
}
