package com.aoe4Forum.mapper;

import com.aoe4Forum.entity.notice.LikeNotice;
import com.aoe4Forum.entity.notice.NoticeCursorPageRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface LikeNoticeMapper {

    void batchInsert(@Param("list") List<LikeNotice> notices);

    List<LikeNotice> cursorQuery(@Param("request") NoticeCursorPageRequest request);

    void insert(LikeNotice notice);

    Boolean QueryIds(
            @Param("senderId") Long senderId,
            @Param("businessType") String businessType,
            @Param("userId") Long userId,
            @Param("businessId") Long businessId
    );
}