package com.aoe4Forum.web.controller;


import com.aoe4Forum.entity.Comment;
import com.aoe4Forum.entity.ResponseVO;
import com.aoe4Forum.entity.User;
import com.aoe4Forum.entity.constans.Constants;
import com.aoe4Forum.entity.dto.UserInfoDto;
import com.aoe4Forum.entity.notice.*;
import com.aoe4Forum.service.CommentService;
import com.aoe4Forum.service.NoticeService;
import com.aoe4Forum.service.PostAndCommentAbstractService;
import com.aoe4Forum.service.UserService;
import com.aoe4Forum.utils.CopyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin
@RestController
@RequestMapping("/notice")
@Validated
public class NoticeController {
    @Autowired
    private NoticeService<LikeNotice> likeNoticeService;

    @Autowired
    private NoticeService<CommentNotice> commentNoticeService;

    @Autowired
    private NoticeService<FollowNotice> followNoticeService;

    @Autowired
    private PostAndCommentAbstractService postAndCommentAbstractService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserService userService;

    @GetMapping("/likeNotices")
    public ResponseVO<NoticeQueryResultVO<LikeNoticeVO>> getLikeNotices(
            Long userId,
            Long lastId,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastCreateTime,
            HttpServletRequest request) {
        if (userId == null) {
            return ResponseVO.error("103", "查询失败");
        }
        Long userIdWithToken = (Long) request.getAttribute("userId");
        if (userIdWithToken == null || !userIdWithToken.equals(userId)) {
            return ResponseVO.error("102", "无权限");
        }
        NoticeCursorPageRequest noticeCursorPageRequest = new NoticeCursorPageRequest();
        noticeCursorPageRequest.setUserId(userId);
        noticeCursorPageRequest.setLastId(lastId);
        noticeCursorPageRequest.setLastCreateTime(lastCreateTime);

        NoticeQueryResult<LikeNotice> result = likeNoticeService.batchQueryNotices(noticeCursorPageRequest);
        if (result == null || result.getNotices() == null || result.getNotices().isEmpty()) {
            // 优化：无通知时返回空列表而非错误（更符合业务预期）
            NoticeQueryResultVO<LikeNoticeVO> emptyResult = new NoticeQueryResultVO<>();
            emptyResult.setNoticeList(Collections.emptyList());
            return ResponseVO.success("查询成功", emptyResult);
        }
        // 1. 提取所有发送者ID（去重，减少查询量）
        List<Long> senderIds = result.getNotices().stream()
                .map(LikeNotice::getSenderId)
                .filter(Objects::nonNull) // 过滤空ID，避免无效查询
                .distinct() // 去重，减少批量查询压力
                .collect(Collectors.toList());
        // 2. 批量查询发送者信息（优先查缓存，缓存未命中查数据库）
        Map<Long, UserInfoDto> senderMap = new HashMap<>();
        if (!senderIds.isEmpty()) {
            // 假设userService.batchQueryByIds返回用户列表，包含id、avatar、nickname
            List<UserInfoDto> senders = userService.batchQueryByIds(senderIds);
            // 转为Map：key=userId，value=User（便于快速查询）
            senderMap = senders.stream()
                    .collect(Collectors.toMap(UserInfoDto::getId, user -> user, (k1, k2) -> k1)); // 处理重复ID（理论上不会出现）
        }
        // 3. 解析通知内容并组装VO（同时填充发送者信息）
        List<String> contents = postAndCommentAbstractService.parseLikeNotice(result.getNotices());
        List<LikeNoticeVO> responsesList = new ArrayList<>();
        for (int i = 0; i < result.getNotices().size(); i++) {
            LikeNotice notice = result.getNotices().get(i);
            LikeNoticeVO noticeVO = CopyUtil.copy(notice, LikeNoticeVO.class);
            noticeVO.setContent(contents.get(i));

            // 4. 从senderMap中获取发送者的avatar和nickname
            Long senderId = notice.getSenderId();
            if (senderId != null) {
                UserInfoDto sender = senderMap.get(senderId);
                if (sender != null) {
                    noticeVO.setAvatar(sender.getAvatar()); // 填充头像
                    noticeVO.setNickname(sender.getUsername()); // 填充昵称
                } else {
                    // 处理发送者信息不存在的情况（如用户已删除）
                    noticeVO.setAvatar(Constants.DEFAULT_AVATAR); // 可设为默认值
                    noticeVO.setNickname("未知用户");
                }
            } else {
                // 发送者ID为null的情况（理论上不应出现，做兜底）
                noticeVO.setAvatar(Constants.DEFAULT_AVATAR);
                noticeVO.setNickname("未知用户");
            }

            responsesList.add(noticeVO);
        }
        // 5. 组装返回结果
        NoticeQueryResultVO<LikeNoticeVO> likeQueryResultVO = new NoticeQueryResultVO<>();
        likeQueryResultVO.setNoticeList(responsesList);
        likeQueryResultVO.setLastCreateTime(result.getLastCreateTime());
        likeQueryResultVO.setLastId(result.getLastId());
        return ResponseVO.success("查询成功", likeQueryResultVO);
    }

    @GetMapping("/commentNotices")
    public ResponseVO<NoticeQueryResultVO<CommentNoticeVO>> getCommentNotices(
            Long userId,
            Long lastId,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastCreateTime,
            HttpServletRequest request) {

        if (userId == null) {
            return ResponseVO.error("103", "查询失败");
        }

        Long userIdWithToken = (Long) request.getAttribute("userId");
        if (userIdWithToken == null || !userIdWithToken.equals(userId)) {
            return ResponseVO.error("102", "无权限");
        }

        NoticeCursorPageRequest noticeCursorPageRequest = new NoticeCursorPageRequest();
        noticeCursorPageRequest.setUserId(userId);
        noticeCursorPageRequest.setLastId(lastId);
        noticeCursorPageRequest.setLastCreateTime(lastCreateTime);

        NoticeQueryResult<CommentNotice> result = commentNoticeService.batchQueryNotices(noticeCursorPageRequest);

        if (result == null || result.getNotices() == null || result.getNotices().isEmpty()) {
            // 优化：无通知时返回空列表而非错误
            NoticeQueryResultVO<CommentNoticeVO> emptyResult = new NoticeQueryResultVO<>();
            emptyResult.setNoticeList(Collections.emptyList());
            return ResponseVO.success("查询成功", emptyResult);
        }

        // 1. 提取所有评论ID和发送者ID（去重）
        List<Long> commentIds = new ArrayList<>();
        List<Long> senderIds = new ArrayList<>();

        for (CommentNotice commentNotice : result.getNotices()) {
            commentIds.add(commentNotice.getBusinessId());
            // 收集发送者ID（假设CommentNotice有getSenderId()方法）
            Long senderId = commentNotice.getSenderId();
            if (senderId != null) {
                senderIds.add(senderId);
            }
        }

        // 2. 批量查询评论内容
        List<Comment> comments = commentService.getCommentsByIds(commentIds);
        // 将评论转为Map便于查询（key: commentId, value: Comment）
        Map<Long, Comment> commentMap = comments.stream()
                .collect(Collectors.toMap(Comment::getCommentId, comment -> comment, (k1, k2) -> k1));

        // 3. 批量查询发送者信息（去重后查询）
        Map<Long, UserInfoDto> senderMap = new HashMap<>();
        if (!senderIds.isEmpty()) {
            // 去重处理，减少查询量
            List<Long> distinctSenderIds = senderIds.stream().distinct().collect(Collectors.toList());
            List<UserInfoDto> senders = userService.batchQueryByIds(distinctSenderIds);
            // 转为Map便于查询（key: userId, value: User）
            senderMap = senders.stream()
                    .collect(Collectors.toMap(UserInfoDto::getId, user -> user, (k1, k2) -> k1));
        }

        // 4. 组装VO对象，填充评论内容和发送者信息
        List<CommentNoticeVO> responsesList = new ArrayList<>();
        for (CommentNotice commentNotice : result.getNotices()) {
            CommentNoticeVO noticeVO = CopyUtil.copy(commentNotice, CommentNoticeVO.class);

            // 填充评论内容
            Comment comment = commentMap.get(commentNotice.getBusinessId());
            if (comment != null) {
                noticeVO.setContent(comment.getContent());
            } else {
                noticeVO.setContent("该评论已删除");
            }

            // 填充发送者头像和昵称
            Long senderId = commentNotice.getSenderId();
            if (senderId != null) {
                UserInfoDto sender = senderMap.get(senderId);
                if (sender != null) {
                    noticeVO.setAvatar(sender.getAvatar());
                    noticeVO.setNickname(sender.getUsername());
                } else {
                    // 发送者不存在时的默认值
                    noticeVO.setAvatar(Constants.DEFAULT_AVATAR);
                    noticeVO.setNickname("未知用户");
                }
            } else {
                // 发送者ID为空时的默认值
                noticeVO.setAvatar(Constants.DEFAULT_AVATAR);
                noticeVO.setNickname("未知用户");
            }

            responsesList.add(noticeVO);
        }

        // 5. 组装返回结果
        NoticeQueryResultVO<CommentNoticeVO> commentQueryResultVO = new NoticeQueryResultVO<>();
        commentQueryResultVO.setNoticeList(responsesList);
        commentQueryResultVO.setLastCreateTime(result.getLastCreateTime());
        commentQueryResultVO.setLastId(result.getLastId());

        return ResponseVO.success("查询成功", commentQueryResultVO);
    }


    @GetMapping("/followNotices")
    public ResponseVO<NoticeQueryResultVO<FollowNoticeVO>> getFollowNotices(
            Long userId,
            Long lastId,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastCreateTime,
            HttpServletRequest request) {

        if (userId == null) {
            return ResponseVO.error("103", "查询失败");
        }

        Long userIdWithToken = (Long) request.getAttribute("userId");
        if (userIdWithToken == null || !userIdWithToken.equals(userId)) {
            return ResponseVO.error("102", "无权限");
        }

        NoticeCursorPageRequest noticeCursorPageRequest = new NoticeCursorPageRequest();
        noticeCursorPageRequest.setUserId(userId);
        noticeCursorPageRequest.setLastId(lastId);
        noticeCursorPageRequest.setLastCreateTime(lastCreateTime);

        NoticeQueryResult<FollowNotice> result = followNoticeService.batchQueryNotices(noticeCursorPageRequest);

        if (result == null || result.getNotices() == null || result.getNotices().isEmpty()) {
            // 无通知时返回空列表
            NoticeQueryResultVO<FollowNoticeVO> emptyResult = new NoticeQueryResultVO<>();
            emptyResult.setNoticeList(Collections.emptyList());
            return ResponseVO.success("查询成功", emptyResult);
        }

        // 1. 提取所有关注者ID（去重）
        List<Long> followerIds = result.getNotices().stream()
                .map(FollowNotice::getBusinessId)  // 假设关注通知中用senderId表示关注者ID
                .filter(Objects::nonNull)        // 过滤空值
                .distinct()                      // 去重减少查询
                .collect(Collectors.toList());

        // 2. 批量查询关注者信息
        Map<Long, UserInfoDto> followerMap = new HashMap<>();
        if (!followerIds.isEmpty()) {
            List<UserInfoDto> followers = userService.batchQueryByIds(followerIds);
            followerMap = followers.stream()
                    .collect(Collectors.toMap(UserInfoDto::getId, user -> user, (k1, k2) -> k1));
        }
        // 3. 组装VO对象，填充关注者信息
        List<FollowNoticeVO> responsesList = new ArrayList<>();
        for (FollowNotice followNotice : result.getNotices()) {
            FollowNoticeVO noticeVO = CopyUtil.copy(followNotice, FollowNoticeVO.class);

            // 填充关注者头像和昵称
            Long followerId = followNotice.getBusinessId();
            if (followerId != null) {
                UserInfoDto follower = followerMap.get(followerId);
                if (follower != null) {
                    noticeVO.setAvatar(follower.getAvatar());
                    noticeVO.setNickname(follower.getUsername());
                } else {
                    // 关注者不存在时的默认值
                    noticeVO.setAvatar(Constants.DEFAULT_AVATAR);
                    noticeVO.setNickname("未知用户");
                }
            } else {
                // 关注者ID为空时的默认值
                noticeVO.setAvatar(Constants.DEFAULT_AVATAR);
                noticeVO.setNickname("未知用户");
            }

            responsesList.add(noticeVO);
        }

        // 4. 组装返回结果
        NoticeQueryResultVO<FollowNoticeVO> followQueryResultVO = new NoticeQueryResultVO<>();
        followQueryResultVO.setNoticeList(responsesList);
        followQueryResultVO.setLastCreateTime(result.getLastCreateTime());
        followQueryResultVO.setLastId(result.getLastId());

        return ResponseVO.success("查询成功", followQueryResultVO);
    }


}
