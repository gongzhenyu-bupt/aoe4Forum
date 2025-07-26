package com.aoe4Forum.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Feed {
    private Long id;
    private Long userId;
    private Long postId;
    private Long senderId;
    private LocalDateTime createTime;
    private Integer status;
}
