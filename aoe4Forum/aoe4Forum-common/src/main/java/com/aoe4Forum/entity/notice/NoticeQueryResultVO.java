package com.aoe4Forum.entity.notice;

import com.aoe4Forum.entity.Post;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class  NoticeQueryResultVO <T extends Notice>{
    private List<T> noticeList;
    private LocalDateTime lastCreateTime;
    private Long lastId;
}
