package com.aoe4Forum.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostImage {
    private Long id;
    private Long postId;
    private String imageUrl;
    private String fileName;
    private LocalDateTime uploadTime;
}
