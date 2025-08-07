package com.aoe4Forum.entity.dto;

import com.aoe4Forum.entity.Post;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RedisPostQueryDto {
    List<Post> posts = new ArrayList<>();
    List<Long> missedPostIds = new ArrayList<>();
}
