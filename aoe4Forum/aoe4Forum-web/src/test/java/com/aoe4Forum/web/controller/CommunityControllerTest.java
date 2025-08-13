package com.aoe4Forum.web.controller;

import com.aoe4Forum.entity.request.CreatePostRequest;
import com.aoe4Forum.entity.request.PostRequest;
import com.aoe4Forum.entity.request.QueryPostRequest;
import com.aoe4Forum.service.impl.PostServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class CommunityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PostServiceImpl postServiceImpl;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testCreatePost() throws Exception {
        CreatePostRequest request = new CreatePostRequest();
        request.setUserId(1L);
        request.setForum("general");
        request.setTitle("测试标题");
        request.setContent("测试内容");
        request.setToken("f825dc05-b38e-4b7f-ae22-065532a4f5bb");
        mockMvc.perform(post("/community/createPost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("发帖成功"));
    }

    @Test
    public void testDeletePost() throws Exception {
        PostRequest request = new PostRequest();
        request.setPostId(1L);

        mockMvc.perform(post("/community/deletePost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("删除成功"));
    }

    @Test
    public void testUpdatePost() throws Exception {
        PostRequest request = new PostRequest();
        request.setPostId(1L);
//        request.setTitle("新标题");

        mockMvc.perform(post("/community/updatePost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("更新成功"));
    }

    @Test
    public void testQueryPostByForum() throws Exception {
        QueryPostRequest request = new QueryPostRequest();
        request.setForum("general");
        request.setOffset(0);
        request.setLimit(10);

        when(postServiceImpl.queryPostByForum(request)).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/community/queryPostByForum")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("查询成功"));
    }

//    @Test
//    public void testQueryPostByHot() throws Exception {
//        QueryPostRequest request = new QueryPostRequest();
//        request.setForum("general");
//        request.setOffset(0);
//        request.setLimit(10);
//
//        when(postServiceImpl.queryPostByHot(request)).thenReturn(Collections.emptyList());
//
//        mockMvc.perform(post("/community/queryPostByHot")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.message").value("查询成功"));
//    }

    @Test
    public void testQueryPostContent() throws Exception {
        long postId = 1L;

        when(postServiceImpl.queryPostContent(postId)).thenReturn(null);

        mockMvc.perform(get("/community/queryPostContent")
                        .param("postId", String.valueOf(postId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }
}
