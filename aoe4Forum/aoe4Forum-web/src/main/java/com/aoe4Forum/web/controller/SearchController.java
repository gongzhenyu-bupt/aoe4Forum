package com.aoe4Forum.web.controller;

import com.aoe4Forum.entity.Post;
import com.aoe4Forum.entity.ResponseVO;
import com.aoe4Forum.service.ElasticSearchService;
import com.aoe4Forum.service.PostService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/search")
public class SearchController {
    @Resource
    private ElasticSearchService elasticSearchService;

    @Resource
    private PostService postService;

    @GetMapping
    public ResponseVO<List<Post>> search(@RequestParam("keyword") String keyword) {
        if(keyword==null||keyword.equals("")){
            return ResponseVO.error("102","关键词为空");
        }
        List<Long> postIds = elasticSearchService.search(keyword);
        if(postIds ==null|| postIds.size()==0){
            return ResponseVO.error("104","没查询到结果");
        }
        List<Post> posts = postService.batchQueryPostByIds(postIds);
        if(posts==null || posts.size()==0){
            return ResponseVO.error("104","没查询到结果");
        }
        return ResponseVO.success("查询成功",posts);
    }
}
