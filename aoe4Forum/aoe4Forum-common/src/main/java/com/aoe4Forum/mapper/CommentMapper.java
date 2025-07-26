package com.aoe4Forum.mapper;

import com.aoe4Forum.entity.Comment;
import com.aoe4Forum.entity.Post;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CommentMapper {

    List<Comment> queryCommentByPostId(long postId,int offset,int limit);
    List<Comment> queryCommentByParentId(@Param("parentId") long parentId,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    List<Comment> queryCommentById(long commentId);

    List<Comment> queryCommentByParentIds(@Param("parentIds") List<Long> parentIds);

    List<Comment> queryCommentByIds(@Param("ids") List<Long> ids);

    int insert(Comment comment);

    int permanentDelete(long commentId);

    int delete(long commentId);

    void deleteCommentWithChildren(long commentId);

    void changeChildCount(@Param("commentId") Long commentId, @Param("delta") Integer delta);

    void likeComment(Long commentId,Integer delta);

    @Select("SELECT COUNT(*) FROM comment WHERE post_id = #{postId} AND parent_id = -1")
    int countByPostId(@Param("postId") Long postId);

}
