package com.aoe4Forum.web.controller;

import com.aoe4Forum.entity.ResponseVO;
import com.aoe4Forum.service.ElasticsearchInitService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.IOException;


@RestController
@RequestMapping("/admin")  // 注意这里
public class AdminController {

    @Resource
    private ElasticsearchInitService elasticsearchInitService;

    @GetMapping("/init")
    public ResponseVO<String> init() throws IOException {
        elasticsearchInitService.init();
        return ResponseVO.success("ok",null);
    }
}
