package com.campus.events.attachment;

import com.campus.events.activity.ActivityDtos;
import com.campus.events.common.ApiResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/admin/activities/{id}/cover")
    public ApiResponse<ActivityDtos.Detail> cover(@PathVariable String id, @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(fileService.replaceCover(id, file));
    }

    @GetMapping("/admin/activities/{id}/cover")
    public ResponseEntity<Resource> adminCover(@PathVariable String id) {
        return fileService.adminCover(id);
    }

    @GetMapping("/public/activities/{id}/cover")
    public ResponseEntity<Resource> publicCover(@PathVariable String id) {
        return fileService.publicCover(id);
    }

    @PostMapping("/admin/activities/{id}/attachments")
    public ApiResponse<ActivityDtos.AttachmentSummary> attachment(
            @PathVariable String id, @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(fileService.addAttachment(id, file));
    }

    @GetMapping("/admin/attachments/{id}/download")
    public ResponseEntity<Resource> adminDownload(@PathVariable String id) {
        return fileService.adminDownload(id);
    }

    @GetMapping("/public/attachments/{id}/download")
    public ResponseEntity<Resource> publicDownload(@PathVariable String id) {
        return fileService.publicDownload(id);
    }

    @DeleteMapping("/admin/attachments/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        fileService.deleteAttachment(id);
        return ApiResponse.ok(null);
    }
}
