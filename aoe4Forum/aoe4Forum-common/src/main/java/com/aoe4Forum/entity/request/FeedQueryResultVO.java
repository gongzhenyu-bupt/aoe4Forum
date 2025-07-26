package com.aoe4Forum.entity.request;

import com.aoe4Forum.entity.Post;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;


@Data
public class FeedQueryResultVO {
    List<Post> posts;
    private LocalDateTime lastCreateTime;
    private Long lastId;
}
