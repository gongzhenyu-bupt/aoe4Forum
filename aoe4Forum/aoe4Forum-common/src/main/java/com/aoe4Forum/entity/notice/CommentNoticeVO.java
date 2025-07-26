package com.aoe4Forum.entity.notice;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CommentNoticeVO extends CommentNotice {
    private String content;
    private String avatar;
    private String nickname;
}
