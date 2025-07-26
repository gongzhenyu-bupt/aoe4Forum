package com.aoe4Forum.mapper;

import com.aoe4Forum.entity.notice.NoticeCursorPageRequest;
import com.aoe4Forum.entity.notice.CommentNotice;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CommentNoticeMapper {

    void insert(CommentNotice notice);

    List<CommentNotice> cursorQuery(@Param("request") NoticeCursorPageRequest request);

    void batchInsert(@Param("list")  List<CommentNotice> notices);
}
