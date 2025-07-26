package com.aoe4Forum.web.mapper;

import com.aoe4Forum.entity.Comment;
import com.aoe4Forum.mapper.CommentMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CommentMapperTest {

    @Autowired
    private CommentMapper commentMapper;

    // 用于测试的共享 commentId
    private static Long testCommentId;

    @Test
    @Order(1)
    public void testInsert() {
        Comment comment = new Comment();
        comment.setPostId(1L);
        comment.setParentId(-1L);
        comment.setCommentTime(LocalDateTime.now());
        comment.setLikeCount(0);
        comment.setStatus(0);
        comment.setContent("这是一个测试评论");
        comment.setUsername("gzy");
        comment.setUserId(1L);

        int rows = commentMapper.insert(comment);
        assertEquals(1, rows, "插入应成功");

        // 假设数据库配置了自动回填 ID
        assertNotNull(comment.getCommentId());
        testCommentId = comment.getCommentId(); // 保存 ID 给后续测试用
    }

    @Test
    @Order(2)
    public void testSelectByPostId() {
        List<Comment> comments = commentMapper.queryCommentByPostId(1L, 0, 10);
        assertNotNull(comments);
        assertFalse(comments.isEmpty(), "应能查到测试评论");
    }

    @Test
    @Order(3)
    public void testSelectByParentId() {
        List<Comment> comments = commentMapper.queryCommentByParentId(1L,0, 10);
        assertNotNull(comments);
        assertFalse(comments.isEmpty(), "应能查到父ID为1的评论");
    }

    @Test
    @Order(4)
    public void testSoftDelete() {
        int rows = commentMapper.delete(testCommentId);
        assertEquals(1, rows, "软删除应成功");
    }

    @Test
    @Order(5)
    public void testPermanentDelete() {
        int rows = commentMapper.permanentDelete(testCommentId);
        assertEquals(1, rows, "硬删除应成功");
    }
}
