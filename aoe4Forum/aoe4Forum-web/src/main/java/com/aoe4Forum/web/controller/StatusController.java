package com.aoe4Forum.web.controller;

import com.aoe4Forum.entity.ForumStatus;
import com.aoe4Forum.entity.ResponseVO;
import com.aoe4Forum.service.StatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/status")
public class StatusController {

    @Resource
    private StatusService statusService;

    @GetMapping
    public ResponseVO<ForumStatus> getStatus(){
        ForumStatus forumStatus = statusService.getStatus();
        return ResponseVO.success("查询成功",forumStatus);
    }
}
