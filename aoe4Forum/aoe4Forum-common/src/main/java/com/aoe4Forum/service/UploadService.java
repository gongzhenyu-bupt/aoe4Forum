package com.aoe4Forum.service;

import org.springframework.web.multipart.MultipartFile;

public interface UploadService {
    String uploadImg(MultipartFile file);
}
