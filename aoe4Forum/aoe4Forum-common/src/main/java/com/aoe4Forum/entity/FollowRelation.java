package com.aoe4Forum.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FollowRelation {
    private Long id;
    private Long follower;
    private Long followee;
    private LocalDateTime createTime;
    private Integer deleteMark;
}

