package com.aoe4Forum.service.impl;

import com.aoe4Forum.entity.notice.NoticeCursorPageRequest;
import com.aoe4Forum.entity.notice.CommentNotice;
import com.aoe4Forum.entity.notice.NoticeQueryResult;
import com.aoe4Forum.mapper.CommentNoticeMapper;
import com.aoe4Forum.service.NoticeService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class CommentNoticeServiceImpl extends NoticeService<CommentNotice> {

    @Resource
    private CommentNoticeMapper commentNoticeMapper;


    @Override
    protected String encodeValueForRedis(CommentNotice notice) {
        return notice.getId() + ":" + notice.getBusinessId();
    }

    @Override
    protected CommentNotice decodeFromRedisValue(String redisValue, double score) throws UnsupportedEncodingException {
        String[] parts = redisValue.split(":");
        if (parts.length < 3) return null;

        CommentNotice notice = new CommentNotice();
        notice.setId(Long.parseLong(parts[0]));
        notice.setBusinessId(Long.parseLong(parts[1]));
        notice.setSenderId(Long.parseLong(parts[2]));
        notice.setCreateTime(Instant.ofEpochMilli((long) score).atZone(ZoneId.systemDefault()).toLocalDateTime());

        return notice;
    }

    @Override
    public CommentNotice createNotice(Long businessId, Long id, LocalDateTime createTime) {
        CommentNotice notice = new CommentNotice();
        notice.setId(id);
        notice.setCreateTime(createTime);
        notice.setBusinessId(businessId);
        return notice;
    }

    @Override
    public void insert(CommentNotice notice) {
        commentNoticeMapper.insert(notice);

        insertToRedis(notice);
    }

    @Override
    public NoticeQueryResult<CommentNotice> batchQueryNotices(NoticeCursorPageRequest request) {
        request.setNoticeName("CommentNotice");
        List<CommentNotice> notices = queryNoticeFromRedis(request);

        if(notices.isEmpty()){
            notices = commentNoticeMapper.cursorQuery(request);
            if(notices.isEmpty()){
                return null;
            }
            batchPushNoticesByPipeline(notices);
            CommentNotice last = notices.get(notices.size()-1);
            request.setLastCreateTime(last.getCreateTime());
            request.setLastId(last.getId());
        }

        freshExpire(request.getUserId(), "CommentNotice");
        saveNoticeReadCursor(request.getUserId(), "CommentNotice");
        NoticeQueryResult<CommentNotice> noticeQueryResult = new NoticeQueryResult<>();
        noticeQueryResult.setNotices(notices);
        noticeQueryResult.setLastCreateTime(request.getLastCreateTime());
        noticeQueryResult.setLastId(request.getLastId());

        return noticeQueryResult;
    }

}
