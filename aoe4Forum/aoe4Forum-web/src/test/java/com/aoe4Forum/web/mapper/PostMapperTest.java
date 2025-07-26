package com.aoe4Forum.web.mapper;

import com.aoe4Forum.entity.Post;
import com.aoe4Forum.entity.PostContent;
import com.aoe4Forum.mapper.PostContentMapper;
import com.aoe4Forum.mapper.PostMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PostMapperTest {

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private PostContentMapper postContentMapper;

    @Test
    public void testInsertAndQueryPost() {
        Post post = new Post();
        post.setUuid(UUID.randomUUID().toString());
        post.setUserId(1L);
        post.setUserName("TestUser");
        post.setForum("test-forum");
        post.setTitle("测试帖子标题");
        post.setCreateTime(LocalDateTime.now());
        post.setUpdateTime(LocalDateTime.now());
        post.setLastCommentTime(LocalDateTime.now());
        post.setLikeCount(0);
        post.setDislikeCount(0);
        post.setCommentCount(0);
        post.setPageViewCount(0L);

        int result = postMapper.insert(post);
        assertEquals(1, result, "插入应返回 1");
        System.out.println(result);
        PostContent postContent = new PostContent();

        postContent.setPostId(post.getId());
        postContent.setContent("这是测试内容123");

        postContentMapper.insert(postContent);

        List<Post> posts = postMapper.queryPostByForum("test-forum", 0, 10);
        System.out.println(posts.get(0).getTitle());

        List<PostContent> postContents = postContentMapper.queryPostContentByPostId(posts.get(0).getId());

        System.out.println(postContents.get(0).getContent());

    }

    @Test
    public void testCountPostByForum() {
        int count = postMapper.countPostByForum("test-forum");
        assertTrue(count >= 0, "帖子数应为非负");
    }

    @Test
    public void testUpdateAndDeletePost() {
        List<Post> posts = postMapper.queryPostByForum("test-forum", 0, 1);
        assertFalse(posts.isEmpty());

        Post post = posts.get(0);
        post.setTitle("修改标题");

        int updated = postMapper.updateById(post);
        assertEquals(1, updated);

        int deleted = postMapper.deleteById(post.getId());
        assertEquals(1, deleted);
    }
}
