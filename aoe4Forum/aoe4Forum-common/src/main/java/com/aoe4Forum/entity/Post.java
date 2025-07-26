package com.aoe4Forum.entity;

import lombok.Data;
import org.joda.time.DateTime;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class Post {
    private Long id;
    private String uuid;
    private Long userId;
    private String userName;
    private String forum;
    private String title;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime lastCommentTime;
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer commentCount;
    private Long pageViewCount;
    private Integer status;
    private String postAbstract;
}
