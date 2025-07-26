package com.aoe4Forum.service.impl;

import com.aoe4Forum.entity.Comment;
import com.aoe4Forum.entity.Post;
import com.aoe4Forum.entity.notice.LikeNotice;
import com.aoe4Forum.service.CommentService;
import com.aoe4Forum.service.PostAndCommentAbstractService;
import com.aoe4Forum.service.PostService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PostAndCommentAbstractServiceImpl implements PostAndCommentAbstractService {

    @Resource
    PostService postService;
    @Resource
    CommentService commentService;

    @Override
    public List<String> parseLikeNotice(List<LikeNotice> likeNoticeList){
        List<Long> postIds = new ArrayList<>();
        List<Long> commentIds = new ArrayList<>();
        for (LikeNotice likeNotice : likeNoticeList) {
            String businessType = likeNotice.getBusinessType();
            if ("post".equals(businessType)) {
                postIds.add(likeNotice.getBusinessId());
            } else if ("comment".equals(businessType)) { // 明确判断comment
                commentIds.add(likeNotice.getBusinessId());
            } else {
                // 处理非法类型（如记录日志、跳过等）
//                log.warn("未知的businessType: {}", businessType);
                continue;
            }
        }
        // 批量查
        List<Post> posts = Collections.emptyList();
        if (!postIds.isEmpty()) {
            posts = postService.batchQueryPostByIds(postIds);
        }

// 处理comments查询
        List<Comment> comments = Collections.emptyList();
        if (!commentIds.isEmpty()) {
            comments = commentService.getCommentsByIds(commentIds);
        }

        // 放入Map
        Map<Long, Post> postMap = posts.stream().collect(Collectors.toMap(Post::getId, p -> p));
        Map<Long, Comment> commentMap = comments.stream().collect(Collectors.toMap(Comment::getCommentId, c -> c));

        // 按原顺序组装
        List<String> result = new ArrayList<>();
        for (LikeNotice likeNotice : likeNoticeList) {
            if ("post".equals(likeNotice.getBusinessType())) {
                Post post = postMap.get(likeNotice.getBusinessId());
                String content = (post != null) ? getAbstract(post.getTitle()) : "该内容已删除";
                result.add(content);
            } else {
                Comment comment = commentMap.get(likeNotice.getBusinessId());
                String content = (comment != null) ? getAbstract(comment.getContent()) : "该评论已删除";
                result.add(content);
            }
        }
        return result;
    }




    private String getAbstract(String content){
        if (content == null) return " ";
        // 去除HTML标签
        String plainText = content.replaceAll("<[^>]*>", "");
        // 去除多余空白
        plainText = plainText.replaceAll("\\s+", "");
        // 截取前50个字符
        return plainText.length() > 10 ? plainText.substring(0, 10) : plainText;
    }



}
