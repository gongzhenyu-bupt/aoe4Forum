package com.aoe4Forum.service;

import com.aoe4Forum.entity.Post;
import com.aoe4Forum.entity.PostContent;
import com.aoe4Forum.entity.request.CreatePostRequest;
import com.aoe4Forum.entity.request.PostRequest;
import com.aoe4Forum.entity.request.QueryPostRequest;

import java.util.List;

public interface PostService {
    void createPost(CreatePostRequest createPostRequest);
    void deletePost(PostRequest postRequest);
    void updatePost(PostRequest postRequest);
    List<Post> queryPostByForum(QueryPostRequest queryPostRequest);
    List<Post> queryPostByHot(QueryPostRequest queryPostRequest);
    List<Post> batchQueryPostByIds(List<Long> postIds);
    Post queryPostById(Long postId);
    PostContent queryPostContent(long postId);
    void likePost(PostRequest postRequest);
    void dislikePost(PostRequest postRequest);
}
