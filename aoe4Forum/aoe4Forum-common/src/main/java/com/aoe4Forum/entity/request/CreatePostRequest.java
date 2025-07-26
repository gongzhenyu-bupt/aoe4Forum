package com.aoe4Forum.entity.request;

import com.aoe4Forum.entity.CanSetRequestParams;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Data
public class CreatePostRequest implements CanSetRequestParams {
    @NotEmpty
    @Size(max=128)
    private String title;

    @NotEmpty
    private String content;

    private String token;

    @NotEmpty
    private String forum;

//    TODO 设计userid和username字段，等到时候传进来用
    private Long userId;
    private String username;

}
