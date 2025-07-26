package com.aoe4Forum.entity.dto;

import com.aoe4Forum.entity.Feed;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class FeedQueryResult {
    List<Feed> feeds;
    private LocalDateTime lastCreateTime;
    private Long lastId;
}
