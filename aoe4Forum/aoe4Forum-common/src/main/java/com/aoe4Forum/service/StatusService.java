package com.aoe4Forum.service;

import com.aoe4Forum.entity.ForumStatus;

public interface StatusService {
    ForumStatus getStatus();
    void changeCount(String type);
}
