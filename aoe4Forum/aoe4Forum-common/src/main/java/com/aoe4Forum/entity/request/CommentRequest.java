package com.aoe4Forum.entity.request;

import com.aoe4Forum.entity.CanSetRequestParams;
import lombok.Data;

@Data
public class CommentRequest implements CanSetRequestParams {
    private Long CommentId;
    private String content;
    private Long userId;
    private String username;
    private String repliedUsername;
    private Long repliedUserId;
    private Long postId;
    private Long parentId;
    private int limit;
    private int offset;
    private String token;
}
