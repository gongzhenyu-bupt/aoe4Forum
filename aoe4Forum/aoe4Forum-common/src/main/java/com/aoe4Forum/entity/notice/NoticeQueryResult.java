package com.aoe4Forum.entity.notice;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class NoticeQueryResult<T extends Notice> {
    List<T> notices;
    private LocalDateTime lastCreateTime;
    private Long lastId;
}
