package com.aoe4Forum.entity.dto;

import lombok.Data;

@Data
public class LikeNoticeDto {
    private Long businessId;
    private Long senderId;
    private String senderName;
    private String businessType;
}
