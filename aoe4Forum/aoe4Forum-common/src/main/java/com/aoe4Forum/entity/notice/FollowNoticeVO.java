package com.aoe4Forum.entity.notice;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class FollowNoticeVO extends FollowNotice{
    private String avatar;
    private String nickname;
}
