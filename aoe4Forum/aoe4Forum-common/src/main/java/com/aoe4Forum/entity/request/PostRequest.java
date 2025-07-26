package com.aoe4Forum.entity.request;

import com.aoe4Forum.entity.CanSetRequestParams;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class PostRequest implements CanSetRequestParams {
    @NotNull
    private Long postId;
    private String token;
    private String content;
    //    TODO 设计userid和username字段，等到时候传进来用
    private Long userId;
    private String username;
}
