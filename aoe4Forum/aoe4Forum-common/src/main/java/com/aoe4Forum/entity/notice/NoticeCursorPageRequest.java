package com.aoe4Forum.entity.notice;

import com.aoe4Forum.entity.constans.Constants;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeCursorPageRequest {
    private Long userId;
    private LocalDateTime lastCreateTime;
    private Long lastId;
    private Integer pageSize = Constants.FEED_PAGE_SIZE;
    private String NoticeName;
}
