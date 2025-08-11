package com.aoe4Forum.mapper;

import com.aoe4Forum.entity.Post;
import com.aoe4Forum.entity.dto.PostCountDto;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface PostMapper {

    List<Post> queryPostByForum(@Param("forum") String forum,@Param("offset") int offset, @Param("limit") int limit);

    List<Post> queryPostById(Long id);

    List<Post> queryPostByIds(@Param("ids") List<Long> ids);

    int countPostByForum(String forum);

    int insert(Post post);

    int permanentDeleteById(long id);

    int deleteById(long id);

    int updateById(Post post);

    void changeCommentCount(@Param("id") Long id, @Param("delta") Integer delta);

    void postLike(Long id,Integer delta);

    void postDislike(Long id,Integer delta);

//未实现
    int countPostByUser(Long userId);

    @Select("SELECT * FROM post")
    List<Post> getAllPosts();

    @Update("UPDATE post SET comment_count = #{commentCount} WHERE id = #{postId}")
    void updateCommentCount(@Param("postId") Long postId, @Param("commentCount") int commentCount);

    void batchUpdatePostCounts(@Param("list") List<PostCountDto> postCountList);

    // 查询用户发布的帖子
    List<Post> queryPostsByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    // 统计用户发布的帖子数量
    int countPostsByUserId(@Param("userId") Long userId);

}
