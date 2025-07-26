package com.aoe4Forum.service;

import com.aoe4Forum.entity.Comment;
import com.aoe4Forum.entity.request.CommentRequest;

import java.util.List;
import java.util.Map;

public interface CommentService {
    void createComment(CommentRequest commentRequest);
    void deleteComment(CommentRequest commentRequest);
    Comment getComment(Long commentId);
    List<Comment> getCommentsByPostId(Long postId,int offset,int limit);
    List<Comment> getCommentsByParentId(Long parentId,int offset,int limit);
    List<Comment> getCommentsByIds(List<Long> ids);
    Map<Long,List<Comment>> getCommentsByParentIds(List<Long> parentIds, int limit);
    void likeComment(CommentRequest commentRequest);
}
