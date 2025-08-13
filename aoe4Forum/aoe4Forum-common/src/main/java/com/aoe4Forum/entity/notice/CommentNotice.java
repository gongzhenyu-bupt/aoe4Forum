package com.aoe4Forum.entity.notice;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CommentNotice extends Notice{
    Long senderId;
    Long postId; // 添加帖子ID字段
}
