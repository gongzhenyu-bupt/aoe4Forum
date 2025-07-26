package com.aoe4Forum.web.controller;

import com.aoe4Forum.entity.Feed;
import com.aoe4Forum.entity.Post;
import com.aoe4Forum.entity.ResponseVO;
import com.aoe4Forum.entity.dto.FeedQueryResult;
import com.aoe4Forum.entity.request.FeedCursorPageRequest;
import com.aoe4Forum.entity.request.FeedQueryResultVO;
import com.aoe4Forum.service.FeedService;
import com.aoe4Forum.service.PostService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@CrossOrigin
@RestController
@RequestMapping("/feed")
@Validated
public class FeedController {


    @Resource
    private FeedService feedService;

    @Resource
    private PostService postService;
//    拉取推送

    @GetMapping("/pullFeed")
    public ResponseVO<FeedQueryResultVO> pullFeed(Long userId, Long lastId, @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastCreateTime, HttpServletRequest request) {
        if(userId==null){
            return ResponseVO.error("103","查询失败");
        }
        Long userIdWithToken = (Long) request.getAttribute("userId");
        if(userIdWithToken==null||!userIdWithToken.equals(userId)){
            return ResponseVO.error("102","无权限");
        }

        FeedCursorPageRequest feedCursorPageRequest = new FeedCursorPageRequest();
        feedCursorPageRequest.setUserId(userId);
        feedCursorPageRequest.setLastId(lastId);
        feedCursorPageRequest.setLastCreateTime(lastCreateTime);
        FeedQueryResult feedQueryResult = feedService.batchQueryFeeds(feedCursorPageRequest);
        if(feedQueryResult==null||feedQueryResult.getFeeds().isEmpty()){
            return ResponseVO.error("104","查询失败，没有查询到帖子");
        }
        List<Long> postIds = new ArrayList<>();
        for(Feed feed : feedQueryResult.getFeeds()){
            Long postId = feed.getPostId();
            if (postId != null) {
                postIds.add(postId);
            }
        }
        if (postIds.isEmpty()) {
            return ResponseVO.error("104", "查询失败，没有有效的帖子ID");
        }
        List<Post> posts =  postService.batchQueryPostByIds(postIds);
        if(posts.isEmpty()){
            return ResponseVO.error("104","查询失败，没有查询到帖子");
        }
        FeedQueryResultVO feedQueryResultVO = new FeedQueryResultVO();
        feedQueryResultVO.setPosts(posts);
        feedQueryResultVO.setLastCreateTime(feedQueryResult.getLastCreateTime());
        feedQueryResultVO.setLastId(feedQueryResult.getLastId());
        return ResponseVO.success("查询成功",feedQueryResultVO);
    }

}
