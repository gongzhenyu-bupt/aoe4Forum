package com.aoe4Forum.service.impl;

import com.aoe4Forum.entity.notice.LikeNotice;
import com.aoe4Forum.entity.notice.NoticeCursorPageRequest;
import com.aoe4Forum.entity.notice.NoticeQueryResult;
import com.aoe4Forum.mapper.LikeNoticeMapper;
import com.aoe4Forum.service.NoticeService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class LikeNoticeServiceImpl extends NoticeService<LikeNotice> {

    @Resource
    private LikeNoticeMapper likeNoticeMapper;

    @Override
    protected String encodeValueForRedis(LikeNotice notice) {
        try {
            return notice.getId() + ":" +
                    notice.getBusinessId() + ":" +
                    notice.getBusinessType()+":"+
                    notice.getSenderId() + ":" +
                    URLEncoder.encode(notice.getSenderName(), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Redis 序列化 senderName 失败", e);
        }
    }

    @Override
    protected LikeNotice decodeFromRedisValue(String redisValue, double score) throws UnsupportedEncodingException {
        String[] parts = redisValue.split(":");
        if (parts.length < 5) return null;

        LikeNotice notice = new LikeNotice();
        notice.setId(Long.parseLong(parts[0]));
        notice.setBusinessId(Long.parseLong(parts[1]));
        notice.setSenderId(Long.parseLong(parts[2]));
        notice.setBusinessType(parts[3]);
        notice.setSenderName(URLDecoder.decode(parts[4], String.valueOf(StandardCharsets.UTF_8)));
        notice.setCreateTime(Instant.ofEpochMilli((long) score).atZone(ZoneId.systemDefault()).toLocalDateTime());

        return notice;
    }


    @Override
    public LikeNotice createNotice(Long businessId, Long id, LocalDateTime createTime) {
        LikeNotice notice = new LikeNotice();
        notice.setId(id);
        notice.setCreateTime(createTime);
        notice.setBusinessId(businessId);
        return notice;
    }

    @Override
    public void insert(LikeNotice notice) {
        likeNoticeMapper.insert(notice);

        insertToRedis(notice);
    }

    @Override
    public NoticeQueryResult<LikeNotice> batchQueryNotices(NoticeCursorPageRequest request) {
        request.setNoticeName("LikeNotice");
        List<LikeNotice> notices = queryNoticeFromRedis(request);

        if(notices.isEmpty()){
            notices = likeNoticeMapper.cursorQuery(request);
            if(notices.isEmpty()){
                return null;
            }
            batchPushNoticesByPipeline(notices);
            LikeNotice last = notices.get(notices.size()-1);
            request.setLastCreateTime(last.getCreateTime());
            request.setLastId(last.getId());
        }

        freshExpire(request.getUserId(), "LikeNotice");
        saveNoticeReadCursor(request.getUserId(), "LikeNotice");
        NoticeQueryResult<LikeNotice> noticeQueryResult = new NoticeQueryResult<>();
        noticeQueryResult.setNotices(notices);
        noticeQueryResult.setLastCreateTime(request.getLastCreateTime());
        noticeQueryResult.setLastId(request.getLastId());

        return noticeQueryResult;
    }

}
