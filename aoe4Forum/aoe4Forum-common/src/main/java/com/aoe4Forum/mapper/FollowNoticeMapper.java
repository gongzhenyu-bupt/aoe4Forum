package com.aoe4Forum.mapper;

import com.aoe4Forum.entity.notice.NoticeCursorPageRequest;
import com.aoe4Forum.entity.notice.FollowNotice;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FollowNoticeMapper {

    void insert(FollowNotice notice);

    List<FollowNotice> cursorQuery(@Param("request") NoticeCursorPageRequest request);

    void batchInsert(@Param("list")  List<FollowNotice> notices);
}
