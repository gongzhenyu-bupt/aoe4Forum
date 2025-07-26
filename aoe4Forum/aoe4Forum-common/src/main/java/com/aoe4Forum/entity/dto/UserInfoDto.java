package com.aoe4Forum.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInfoDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String username;
    private Integer followeeNums;
    private Integer followerNums;
    private String avatar;
    private Long expireTime;

    public Integer getFollowerNums() {
        return followerNums;
    }

    public void setFollowerNums(Integer followerNums) {
        this.followerNums = followerNums;
    }

    public Integer getFolloweeNums() {
        return followeeNums;
    }

    public void setFolloweeNums(Integer followeeNums) {
        this.followeeNums = followeeNums;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Long expireTime) {
        this.expireTime = expireTime;
    }

}
