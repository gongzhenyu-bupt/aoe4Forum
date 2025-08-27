package com.aoe4Forum.utils;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 图片格式转换工具类
 */
public class ImageConvertUtils {

    /**
     * 将MultipartFile转换为PNG格式（修复流泄漏 + 统一缩放逻辑）
     * @param file 原始图片文件
     * @param scale 缩放比例（0.0-1.0，如0.8表示80%尺寸）
     * @param quality 图片质量（0.0-1.0，如0.8表示80%质量）
     */
    public static InputStream convertToPng(MultipartFile file, float scale, float quality) throws IOException {
        // 1. 基础校验
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("文件不是有效的图片类型");
        }

        // 2. 关键：用try-with-resources自动关闭输入流，释放临时文件
        try (InputStream originalInputStream = file.getInputStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // 3. 一次缩放+转格式，避免重复处理
            Thumbnails.of(originalInputStream)
                    .outputFormat("png")       // 强制PNG
                    .scale(scale)              // 统一缩放比例
                    .outputQuality(quality)    // 统一质量
                    .toOutputStream(outputStream);

            return new ByteArrayInputStream(outputStream.toByteArray());
        }
        // try-with-resources自动关闭originalInputStream，解决临时文件占用问题
    }

    /**
     * 简化重载：默认缩放0.8、质量0.8（兼容原有逻辑）
     */
    public static InputStream convertToPng(MultipartFile file) throws IOException {
        return convertToPng(file, 0.1f, 0.1f);
    }

    /**
     * 转换并保存PNG（删除重复缩放，直接写入目标文件）
     */
    public static void convertToPngAndSave(MultipartFile file, String targetPath) throws IOException {
        // 直接使用原始流处理，避免中间流重复缩放
        try (InputStream originalInputStream = file.getInputStream()) {
            Thumbnails.of(originalInputStream)
                    .outputFormat("png")       // 强制PNG
                    .scale(0.1f)               // 一次缩放（与convertToPng保持一致）
                    .outputQuality(0.1f)       // 一次质量压缩
                    .toFile(targetPath);        // 直接写入目标文件，减少中间环节
        }
    }
}
