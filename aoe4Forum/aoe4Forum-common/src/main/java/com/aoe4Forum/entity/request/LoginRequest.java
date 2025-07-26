package com.aoe4Forum.entity.request;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Data
public class LoginRequest {
    @NotEmpty
    @Size(min=11, max=11)
    private String phoneNo;

    @NotEmpty
    private String password;

    // getter/setter省略
}

