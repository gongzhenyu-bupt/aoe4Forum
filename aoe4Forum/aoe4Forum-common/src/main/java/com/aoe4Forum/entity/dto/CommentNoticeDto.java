package com.aoe4Forum.entity.dto;

import lombok.Data;

@Data
public class CommentNoticeDto {
    Long commentId;
    Long repliedUserId;
    Long userId;
    Long postId; // 添加帖子ID字段
}
