package com.aoe4Forum.web.mapper;

import com.aoe4Forum.entity.Post;
import com.aoe4Forum.mapper.CommentMapper;
import com.aoe4Forum.mapper.PostMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
public class PostCommentCountTest {

    @Resource
    private PostMapper postMapper;

    @Resource
    private CommentMapper commentMapper;

    @Test
    public void updateAllPostCommentCounts() {
        List<Post> posts = postMapper.getAllPosts();
        log.info("共加载 {} 条帖子", posts.size());

        int updated = 0;
        for (Post post : posts) {
            Long postId = post.getId();
            int commentCount = commentMapper.countByPostId(postId);
            postMapper.updateCommentCount(postId, commentCount);
            updated++;
            log.debug("更新 postId = {}, 评论数 = {}", postId, commentCount);
        }

        log.info("已更新 commentCount 字段的帖子数量：{}", updated);
    }
}
