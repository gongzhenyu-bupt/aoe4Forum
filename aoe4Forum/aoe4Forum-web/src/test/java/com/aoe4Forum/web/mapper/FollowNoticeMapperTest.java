package com.aoe4Forum.web.mapper;

import com.aoe4Forum.entity.notice.FollowNotice;
import com.aoe4Forum.mapper.FollowNoticeMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;

@SpringBootTest
@RunWith(SpringRunner.class)
public class FollowNoticeMapperTest {

    @Autowired
    private FollowNoticeMapper followNoticeMapper;

    @Test
    public void insert() {
        FollowNotice notice = new FollowNotice();
        notice.setUserId(123L);
        notice.setId(123L);
        notice.setCreateTime(LocalDateTime.now());
        notice.setBusinessId(222L);
        followNoticeMapper.insert(notice);
    }
}
