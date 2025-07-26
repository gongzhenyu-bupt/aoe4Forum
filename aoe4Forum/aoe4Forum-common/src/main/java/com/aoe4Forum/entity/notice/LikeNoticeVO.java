package com.aoe4Forum.entity.notice;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class LikeNoticeVO extends LikeNotice{
    private String content;
    private String avatar;
    private String nickname;
}
