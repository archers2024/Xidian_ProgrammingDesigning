package com.campus.events.attachment;

import com.campus.events.common.ApiException;
import com.campus.events.common.ErrorCode;
import com.campus.events.common.Ids;
import com.campus.events.config.AppProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

@Component
public class FileStorage {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp");
    private static final Set<String> DOC_EXTENSIONS = Set.of("pdf", "docx", "pptx");
    private static final long COVER_LIMIT = 5L * 1024 * 1024;
    private static final long ATTACHMENT_LIMIT = 20L * 1024 * 1024;

    private final Path covers;
    private final Path attachments;

    public FileStorage(AppProperties properties) {
        Path root = Path.of(properties.storageRoot()).toAbsolutePath().normalize();
        this.covers = root.resolve("uploads").resolve("covers");
        this.attachments = root.resolve("uploads").resolve("attachments");
        try {
            Files.createDirectories(this.covers);
            Files.createDirectories(this.attachments);
        } catch (IOException exception) {
            throw new IllegalStateException("无法创建上传目录 " + root, exception);
        }
    }

    public StoredFile saveCover(MultipartFile file) {
        byte[] head = head(file, COVER_LIMIT, "封面不能超过 5MB");
        String extension = imageExtension(head, file.getOriginalFilename());
        String key = Ids.newId() + "." + extension;
        write(covers.resolve(key), file);
        return new StoredFile(key, contentTypeForImage(extension), file.getSize());
    }

    public StoredFile saveAttachment(MultipartFile file) {
        byte[] head = head(file, ATTACHMENT_LIMIT, "单个附件不能超过 20MB");
        String extension = documentExtension(head, file.getOriginalFilename());
        String key = Ids.newId() + "." + extension;
        write(attachments.resolve(key), file);
        return new StoredFile(key, contentTypeForDocument(extension), file.getSize(), safeName(file.getOriginalFilename()));
    }

    public Resource cover(String key) {
        return read(covers, key);
    }

    public Resource attachment(String key) {
        return read(attachments, key);
    }

    public void deleteCover(String key) {
        delete(covers, key);
    }

    public void deleteAttachment(String key) {
        delete(attachments, key);
    }

    private static byte[] head(MultipartFile file, long limit, String tooLarge) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "请选择要上传的文件");
        }
        if (file.getSize() > limit) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, tooLarge);
        }
        try (InputStream input = file.getInputStream()) {
            return input.readNBytes(16);
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "文件读取失败");
        }
    }

    private static void write(Path target, MultipartFile file) {
        try {
            file.transferTo(target);
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "文件保存失败");
        }
    }

    private static Resource read(Path directory, String key) {
        Path path = directory.resolve(safeKey(key)).normalize();
        if (!path.startsWith(directory) || !Files.isRegularFile(path)) {
            throw new ApiException(ErrorCode.ATTACHMENT_NOT_FOUND);
        }
        return new FileSystemResource(path);
    }

    private static void delete(Path directory, String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            Path path = directory.resolve(safeKey(key)).normalize();
            if (path.startsWith(directory)) {
                Files.deleteIfExists(path);
            }
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "文件清理失败");
        }
    }

    private static String imageExtension(byte[] head, String originalName) {
        String extension = extension(originalName);
        if ("png".equals(extension) && startsWith(head, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47})) {
            return "png";
        }
        if (("jpg".equals(extension) || "jpeg".equals(extension))
                && head.length >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if ("gif".equals(extension) && startsWith(head, new byte[]{'G', 'I', 'F'})) {
            return "gif";
        }
        if ("webp".equals(extension) && head.length >= 12
                && startsWith(head, new byte[]{'R', 'I', 'F', 'F'})
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
            return "webp";
        }
        throw new ApiException(ErrorCode.VALIDATION_ERROR, "封面仅支持 PNG、JPEG、GIF、WEBP");
    }

    private static String documentExtension(byte[] head, String originalName) {
        String extension = extension(originalName);
        if ("pdf".equals(extension) && startsWith(head, new byte[]{'%', 'P', 'D', 'F'})) {
            return "pdf";
        }
        boolean zip = startsWith(head, new byte[]{'P', 'K', 0x03, 0x04}) || startsWith(head, new byte[]{'P', 'K', 0x05, 0x06});
        if (zip && ("docx".equals(extension) || "pptx".equals(extension))) {
            return extension;
        }
        throw new ApiException(ErrorCode.VALIDATION_ERROR, "附件仅支持 PDF、DOCX、PPTX");
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static String extension(String originalName) {
        String name = safeName(originalName).toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1);
    }

    static String safeName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "文件名不能为空");
        }
        String name = Path.of(originalName).getFileName().toString().replace("\\", "").replace("/", "").trim();
        if (name.isBlank() || name.length() > 200) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "文件名需为 1 至 200 个字符");
        }
        return name;
    }

    private static String safeKey(String key) {
        if (key == null || key.contains("..") || key.contains("/") || key.contains("\\")) {
            throw new ApiException(ErrorCode.ATTACHMENT_NOT_FOUND);
        }
        return key;
    }

    private static String contentTypeForImage(String extension) {
        return switch (extension) {
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
    }

    private static String contentTypeForDocument(String extension) {
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            default -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        };
    }

    public record StoredFile(String key, String contentType, long sizeBytes, String originalName) {
        public StoredFile(String key, String contentType, long sizeBytes) {
            this(key, contentType, sizeBytes, null);
        }
    }
}
