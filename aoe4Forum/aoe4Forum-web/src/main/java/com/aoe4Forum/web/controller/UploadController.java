package com.aoe4Forum.web.controller;

import com.aoe4Forum.entity.ResponseVO;
import com.aoe4Forum.service.UploadService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;

@RestController
@RequestMapping("/upload")
public class UploadController {
    @Resource
    private UploadService uploadService;

    @PostMapping("/image")
    public ResponseVO<String> uploadImage(MultipartFile file) throws IOException {
        String url = uploadService.uploadImg(file);
        return ResponseVO.success("上传成功",url);
    }
}
