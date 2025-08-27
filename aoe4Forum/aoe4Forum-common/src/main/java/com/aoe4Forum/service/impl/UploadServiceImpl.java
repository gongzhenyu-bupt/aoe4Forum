package com.aoe4Forum.service.impl;

import com.aoe4Forum.exception.ErrorParamsException;
import com.aoe4Forum.service.UploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
public class UploadServiceImpl implements UploadService {

    @Override
    public String uploadImg(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null ||
                !(contentType.equals("image/jpeg") ||
                        contentType.equals("image/png")  ||
                        contentType.equals("image/gif")  ||
                        contentType.equals("image/webp"))) {
            log.error("图片类型错误");
            throw new ErrorParamsException("图片类型错误");
        }
        if(file.isEmpty()){
            log.error("没有文件");
            throw new ErrorParamsException("没有文件");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            log.error("文件大小不能超过10MB");
            throw new ErrorParamsException("文件大小不能超过10MB");
        }
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + extension;

        File dir = new File("/root/aoe4Forum/tempImg");
        if (!dir.exists()) {
            dir.mkdirs(); // 创建目录
        }
        String path;
        try{
            String realPath = dir.getCanonicalPath();
            path = realPath + "/" + fileName;
            file.transferTo(new File(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return "/root/aoe4Forum/tempImg/"+fileName;
    }

}
