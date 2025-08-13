package com.aoe4Forum.mapper;

import com.aoe4Forum.entity.ForumStatus;

public interface ForumStatusMapper {
    ForumStatus getInitStatus();
    void insertStatus(ForumStatus forumStatus);
    ForumStatus selectLatest();
}
