package com.aoe4Forum.service;

import com.aoe4Forum.entity.notice.LikeNotice;

import java.util.List;

public interface PostAndCommentAbstractService {

    List<String> parseLikeNotice(List<LikeNotice> likeNoticeList);
}
