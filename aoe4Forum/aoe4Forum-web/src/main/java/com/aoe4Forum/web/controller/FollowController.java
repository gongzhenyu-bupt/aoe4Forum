package com.aoe4Forum.web.controller;


import com.aoe4Forum.entity.FollowRelation;
import com.aoe4Forum.entity.ResponseVO;
import com.aoe4Forum.entity.request.FollowCursorPageRequest;
import com.aoe4Forum.entity.dto.FollowQueryResult;
import com.aoe4Forum.service.impl.FollowServiceImpl;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@CrossOrigin
@RestController
@RequestMapping("/follow")
@Validated
public class FollowController {

    @Resource
    private FollowServiceImpl followServiceimpl;

//    用户关注另一个用户（写操作）
    @PostMapping("/follow")
    public ResponseVO<Map<String,String>> follow(Long followerId, Long followeeId, HttpServletRequest request) {
        Long requestUserId = (Long) request.getAttribute("userId");
        if(Objects.equals(followerId, followeeId)){
            return ResponseVO.error("104","你不能关注你自己");
        }
        if(!followerId.equals(requestUserId)){
            return ResponseVO.error("103","无权限");
        }
        FollowRelation followRelation = new FollowRelation();
        followRelation.setFollower(followerId);
        followRelation.setFollowee(followeeId);
        followRelation.setCreateTime(LocalDateTime.now());
        followServiceimpl.addFollower(followRelation);
        return ResponseVO.success();
    }
//    用户取关另一个用户（写操作）
    @PostMapping("/unfollow")
    public ResponseVO<Map<String,String>> unfollow(Long followerId, Long followeeId, HttpServletRequest request){
        Long requestUserId = (Long) request.getAttribute("userId");
        if(Objects.equals(followerId, followeeId)){
            return ResponseVO.error("104","你不能取关你自己");
        }
        if(!followerId.equals(requestUserId)){
            return ResponseVO.error("103","无权限");
        }
        FollowRelation followRelation = new FollowRelation();
        followRelation.setFollower(followerId);
        followRelation.setFollowee(followeeId);
        followRelation.setCreateTime(LocalDateTime.now());
        followServiceimpl.deleteFollower(followRelation);
        return ResponseVO.success();
    }

//    查询某个用户的关注列表（读操作）
    @GetMapping("followerList")
    public ResponseVO<FollowQueryResult> followerList(
                                                    Long userId,
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastCreateTime,
                                                    Long lastId,
                                                    Boolean needCursor
                                                        ){
        FollowCursorPageRequest followCursorPageRequest = new FollowCursorPageRequest();
        followCursorPageRequest.setUserId(userId);
        if(needCursor){
            followCursorPageRequest.setLastCreateTime(lastCreateTime);
            followCursorPageRequest.setLastId(lastId);
        }
        FollowQueryResult followQueryResult = followServiceimpl.queryFollower(followCursorPageRequest);
        return ResponseVO.success("查询成功",followQueryResult);
    }
//    查询某个用户的粉丝列表（读操作）
    @GetMapping("followeeList")
    public ResponseVO<FollowQueryResult> followeeList(
                                                            Long userId,
                                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastCreateTime,
                                                            Long lastId,
                                                            Boolean needCursor
                                                        ){
        FollowCursorPageRequest followCursorPageRequest = new FollowCursorPageRequest();
        followCursorPageRequest.setUserId(userId);
        if(needCursor){
            followCursorPageRequest.setLastCreateTime(lastCreateTime);
            followCursorPageRequest.setLastId(lastId);
        }
        FollowQueryResult followQueryResult = followServiceimpl.queryFollowee(followCursorPageRequest);
        return ResponseVO.success("查询成功",followQueryResult);
}

//    查询是否已关注（读操作）
    @GetMapping("isFollow")
    public ResponseVO<Map<String,String>> isFollow(Long follower,
                                                   Long followee){
        FollowRelation followRelation = new FollowRelation();
        followRelation.setFollower(follower);
        followRelation.setFollowee(followee);
        if(followServiceimpl.checkFollower(followRelation)!=0){
            return ResponseVO.error("105","未关注");
        }
        return ResponseVO.success();
    }

}
