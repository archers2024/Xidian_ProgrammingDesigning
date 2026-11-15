package com.campus.activity.module.system.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.campus.activity.common.R;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A岗：文件上传抽象（契约 /file/upload）。
 * 当前落本地磁盘，后续切换对象存储只需替换本类实现，B岗调用方式不变。
 */

@RestController
public class FileController {
    private static final Logger log = LoggerFactory.getLogger(FileController.class);


    private static final List<String> WHITE_EXT = List.of("jpg", "jpeg", "png", "gif", "pdf", "doc", "docx", "ppt", "pptx");

    @Value("${campus.file.upload-dir}")
    private String uploadDir;

    @Value("${campus.file.url-prefix}")
    private String urlPrefix;

    @PostMapping("/file/upload")
    public R<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.business("文件为空");
        }
        String origin = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = origin.contains(".") ? origin.substring(origin.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!WHITE_EXT.contains(ext)) {
            return R.business("不支持的文件类型：" + ext);
        }
        if (file.getSize() > 20 * 1024 * 1024) {
            return R.business("文件大小不能超过 20MB");
        }
        try {
            String dir = uploadDir + File.separator + LocalDate.now();
            File folder = new File(dir);
            if (!folder.exists() && !folder.mkdirs()) {
                return R.business("创建上传目录失败");
            }
            String name = UUID.randomUUID() + "." + ext;
            File dest = new File(folder, name);
            file.transferTo(dest);
            Map<String, String> data = new HashMap<>();
            data.put("url", urlPrefix + LocalDate.now() + "/" + name);
            data.put("fileName", origin);
            data.put("fileSize", String.valueOf(file.getSize()));
            return R.ok(data);
        } catch (Exception e) {
            log.error("上传失败", e);
            return R.business("上传失败，请稍后重试");
        }
    }
}
