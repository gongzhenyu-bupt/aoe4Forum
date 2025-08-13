package com.aoe4Forum.mapper;

import com.aoe4Forum.entity.PostImage;

import java.util.List;

public interface PostImageMapper {
    void insert(PostImage postImage);
    List<PostImage> queryByPostId(Long postId);
}
