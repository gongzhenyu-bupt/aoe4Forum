package com.aoe4Forum.service.impl;

import com.aoe4Forum.entity.notice.NoticeCursorPageRequest;
import com.aoe4Forum.entity.notice.FollowNotice;
import com.aoe4Forum.entity.notice.NoticeQueryResult;
import com.aoe4Forum.mapper.FollowNoticeMapper;
import com.aoe4Forum.service.NoticeService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FollowNoticeServiceImpl extends NoticeService<FollowNotice> {

    @Resource
    private FollowNoticeMapper followNoticeMapper;


    @Override
    public String encodeValueForRedis(FollowNotice notice) {
            return notice.getId() + ":" + notice.getBusinessId();
    }

    @Override
    public FollowNotice decodeFromRedisValue(String redisValue, double score) throws UnsupportedEncodingException {
        String[] parts = redisValue.split(":");
        if (parts.length < 2) return null;

        FollowNotice notice = new FollowNotice();
        notice.setId(Long.parseLong(parts[0]));
        notice.setBusinessId(Long.parseLong(parts[1]));

        return notice;
    }

    @Override
    public FollowNotice createNotice(Long businessId, Long id, LocalDateTime createTime) {
        FollowNotice notice = new FollowNotice();
        notice.setId(id);
        notice.setCreateTime(createTime);
        notice.setBusinessId(businessId);
        return notice;
    }

    @Override
    public void insert(FollowNotice notice) {
        try {
            followNoticeMapper.insert(notice);
            insertToRedis(notice);
        } catch (Exception e) {
            // 如果是重复键异常，忽略它
            if (e.getMessage().contains("Duplicate entry") || e.getMessage().contains("uniq_user_follower")) {
                // 记录日志但不抛出异常
                System.out.println("Duplicate follow notice ignored: " + notice.getUserId() + " -> " + notice.getBusinessId());
            }
        }
    }

    @Override
    public NoticeQueryResult<FollowNotice> batchQueryNotices(NoticeCursorPageRequest request) {
        request.setNoticeName("FollowNotice");
        List<FollowNotice> notices = queryNoticeFromRedis(request);
        if(notices.isEmpty()){
            notices = followNoticeMapper.cursorQuery(request);
            if(notices.isEmpty()){
                return null;
            }
            batchPushNoticesByPipeline(notices);
            FollowNotice last = notices.get(notices.size()-1);
            request.setLastCreateTime(last.getCreateTime());
            request.setLastId(last.getId());
        }

        freshExpire(request.getUserId(), "FollowNotice");
        saveNoticeReadCursor(request.getUserId(), "FollowNotice");
        NoticeQueryResult<FollowNotice> noticeQueryResult = new NoticeQueryResult<>();
        noticeQueryResult.setNotices(notices);
        noticeQueryResult.setLastCreateTime(request.getLastCreateTime());
        noticeQueryResult.setLastId(request.getLastId());

        return noticeQueryResult;
    }
}
