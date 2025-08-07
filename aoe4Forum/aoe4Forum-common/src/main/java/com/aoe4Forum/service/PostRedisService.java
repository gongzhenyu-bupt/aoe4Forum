package com.aoe4Forum.service;

import com.aoe4Forum.entity.Post;
import com.aoe4Forum.entity.PostContent;
import com.aoe4Forum.entity.dto.PostCountDto;
import com.aoe4Forum.entity.dto.RedisPostQueryDto;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRedisService {
    Post queryPostByIdFromRedis(Long postId);
    PostCountDto queryPostCountFromRedis(Long postId);
    void changeCount(Long postId,String type,int delta);
    void setLastCommentTime(Long postId,LocalDateTime lastCommentTime);
    List<Long> queryPostIdsByForumFromRedis(String forumName);
    List<Long> queryPostIdsByHotFromRedis(int page);
    void addPostToRedis(Post post);
    RedisPostQueryDto batchQueryPostInfoWithCacheFallback(List<Long> postIds);
    void addPostCountToRedis(Post post);
    void addPostContentToRedis(PostContent postContent);
    PostContent queryPostContentFromRedis(Long postId);
    void cachePostIdsByForum(String forumName, List<Post> posts);
    void cachePostIdByForum(String forumName,Post post);
}
