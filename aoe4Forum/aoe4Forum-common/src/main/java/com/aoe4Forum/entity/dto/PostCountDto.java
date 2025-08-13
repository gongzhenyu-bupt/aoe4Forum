package com.aoe4Forum.entity.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostCountDto {
    private Long postId;
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer commentCount;
    private Long pageViewCount;
    private LocalDateTime lastCommentTime;
}
