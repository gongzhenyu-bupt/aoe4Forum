package com.aoe4Forum.mapper;

import com.aoe4Forum.entity.PostContent;

import java.util.List;

public interface PostContentMapper {
    List<PostContent> queryPostContentByPostId(Long id);
    void updateByPostId(PostContent postContent);
    int deleteByPostId(Long id);
    int insert(PostContent postContent);
}
