package com.aoe4Forum.entity.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class FollowQueryResult {
    private List<FollowUserInfo>  followers;
    private LocalDateTime lastCreateTime;
    private Long lastId;
}
