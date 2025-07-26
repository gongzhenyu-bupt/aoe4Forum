package com.aoe4Forum.entity;

import lombok.Data;

import java.time.LocalDateTime;


@Data
public class User {
    private Long id;
    private String name;
    private String password;
    private String phoneNo;
    private Integer role;
    private String personIntroduction;
    private LocalDateTime joinTime;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private Integer status;
    private Integer exp;
    private String avatar;
    private Integer followerNums; // 我的粉丝数量（多少人关注了我）
    private Integer followeeNums; // 我关注的人数量（我关注了多少人）
}
