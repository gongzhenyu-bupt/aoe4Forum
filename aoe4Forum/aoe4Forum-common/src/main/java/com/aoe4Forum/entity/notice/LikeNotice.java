package com.aoe4Forum.entity.notice;

import lombok.Data;

@Data
public class LikeNotice extends Notice {
    Long senderId;
    String senderName;
//    点赞的是评论还是帖子
    String businessType;
}
