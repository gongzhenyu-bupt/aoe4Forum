package com.aoe4Forum.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class Comment {
    private Long commentId;
    private Long postId;
    private Long parentId;
    private LocalDateTime commentTime;
    private Integer likeCount;
    private String content;
    private Integer status;
    private String username;
    private Long userId;
    private String avatar; // 用户头像
    private String repliedUsername;
    private Long repliedUserId;
    private Integer childCount;
}
