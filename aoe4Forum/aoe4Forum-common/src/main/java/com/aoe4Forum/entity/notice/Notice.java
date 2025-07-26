package com.aoe4Forum.entity.notice;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public abstract class Notice implements Serializable {

    // 通知记录主键（唯一 ID，用于分页游标）
    private Long id;

    // 接收通知的用户 ID（谁的通知列表中出现）
    private Long userId;

    // 通知产生的业务实体 ID（如帖子 ID、评论 ID）
    private Long businessId;

    // 通知创建时间（用于游标分页排序）
    private LocalDateTime createTime;
}