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


    private final com.campus.activity.module.system.service.FileAssetService assets;
    public FileController(com.campus.activity.module.system.service.FileAssetService assets){this.assets=assets;}

    private static final List<String> WHITE_EXT = List.of("jpg", "jpeg", "png", "gif", "pdf", "doc", "docx", "ppt", "pptx");

    @Value("${campus.file.upload-dir}")
    private String uploadDir;

    @Value("${campus.file.url-prefix}")
    private String urlPrefix;

    @PostMapping("/file/upload")
    public R<Map<String, String>> upload(@RequestParam("file") MultipartFile file,@RequestParam(defaultValue="PRIVATE")String visibility,@RequestParam(required=false)Long orgId) {
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
            try (var input = file.getInputStream()) {
                byte[] header = input.readNBytes(8);
                String magic = new String(header, java.nio.charset.StandardCharsets.ISO_8859_1);
                boolean valid = switch (ext) {
                    case "pdf" -> magic.startsWith("%PDF-");
                    case "png" -> header.length >= 8 && java.util.Arrays.equals(header, new byte[]{(byte)137,80,78,71,13,10,26,10});
                    case "jpg", "jpeg" -> header.length >= 3 && (header[0]&255)==255 && (header[1]&255)==216 && (header[2]&255)==255;
                    case "gif" -> magic.startsWith("GIF87a") || magic.startsWith("GIF89a");
                    case "docx", "pptx" -> magic.startsWith("PK") && validOffice(file, ext);
                    default -> header.length >= 4 && (header[0]&255)==208 && (header[1]&255)==207 && (header[2]&255)==17 && (header[3]&255)==224;
                };
                if (!valid) return R.business("文件内容与扩展名不符");
            }
            return R.ok(assets.upload(file,ext,visibility,orgId));
        } catch (com.campus.activity.common.BusinessException e) { throw e;
        } catch (Exception e) {
            log.error("上传失败", e);
            return R.business("上传失败，请稍后重试");
        }
    }

    private boolean validOffice(MultipartFile file, String ext) throws java.io.IOException {
        try (var zip = new java.util.zip.ZipInputStream(file.getInputStream())) {
            boolean types = false, document = false;
            long expanded = 0;
            int entries = 0;
            byte[] buffer = new byte[8192];
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > 1000) return false;
                types |= "[Content_Types].xml".equals(entry.getName());
                document |= ("docx".equals(ext) ? "word/document.xml" : "ppt/presentation.xml").equals(entry.getName());
                int count;
                while ((count = zip.read(buffer)) != -1) {
                    expanded += count;
                    if (expanded > 100L * 1024 * 1024) return false;
                }
            }
            return types && document;
        }
    }
}
