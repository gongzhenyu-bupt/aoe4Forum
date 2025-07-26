package com.aoe4Forum.entity.request;

import com.aoe4Forum.entity.constans.Constants;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FeedCursorPageRequest {
    private Long userId;
    private LocalDateTime lastCreateTime;
    private Long lastId;
    private int pageSize = Constants.FEED_PAGE_SIZE;
}
