package com.campus.events.attachment;

import com.campus.events.activity.ActivityDtos;
import com.campus.events.activity.ActivityMapper;
import com.campus.events.activity.ActivityRow;
import com.campus.events.activity.ActivityService;
import com.campus.events.common.ApiException;
import com.campus.events.common.ErrorCode;
import com.campus.events.common.Ids;
import com.campus.events.common.TimeSupport;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class FileService {

    private final ActivityService activityService;
    private final ActivityMapper activityMapper;
    private final AttachmentMapper attachmentMapper;
    private final FileStorage fileStorage;

    public FileService(
            ActivityService activityService,
            ActivityMapper activityMapper,
            AttachmentMapper attachmentMapper,
            FileStorage fileStorage) {
        this.activityService = activityService;
        this.activityMapper = activityMapper;
        this.attachmentMapper = attachmentMapper;
        this.fileStorage = fileStorage;
    }

    @Transactional
    public ActivityDtos.Detail replaceCover(String activityId, MultipartFile file) {
        ActivityRow activity = activityService.requireActive(activityId, false);
        FileStorage.StoredFile stored = fileStorage.saveCover(file);
        String previous = activity.getCoverStorageKey();
        activityMapper.updateCover(activityId, stored.key(), stored.contentType(), TimeSupport.now());
        if (previous != null && !previous.equals(stored.key())) {
            fileStorage.deleteCover(previous);
        }
        return activityService.getAdmin(activityId);
    }

    @Transactional
    public ActivityDtos.AttachmentSummary addAttachment(String activityId, MultipartFile file) {
        activityService.requireActive(activityId, false);
        FileStorage.StoredFile stored = fileStorage.saveAttachment(file);
        AttachmentRow row = new AttachmentRow();
        row.setId(Ids.newId());
        row.setActivityId(activityId);
        row.setOriginalName(stored.originalName());
        row.setStorageKey(stored.key());
        row.setContentType(stored.contentType());
        row.setSizeBytes(stored.sizeBytes());
        row.setCreatedAt(TimeSupport.now());
        attachmentMapper.insert(row);
        return new ActivityDtos.AttachmentSummary(
                row.getId(), row.getOriginalName(), row.getContentType(), row.getSizeBytes(), row.getCreatedAt());
    }

    @Transactional
    public void deleteAttachment(String attachmentId) {
        AttachmentRow row = attachmentMapper.findById(attachmentId);
        if (row == null) {
            throw new ApiException(ErrorCode.ATTACHMENT_NOT_FOUND);
        }
        activityService.requireActive(row.getActivityId(), false);
        attachmentMapper.deleteById(attachmentId);
        fileStorage.deleteAttachment(row.getStorageKey());
    }

    public ResponseEntity<Resource> adminDownload(String attachmentId) {
        AttachmentRow row = requireAttachment(attachmentId);
        activityService.requireActive(row.getActivityId(), false);
        return download(row);
    }

    public ResponseEntity<Resource> publicDownload(String attachmentId) {
        AttachmentRow row = requireAttachment(attachmentId);
        ActivityRow activity = activityMapper.findById(row.getActivityId());
        if (activity == null || activity.getDeletedAt() != null || !"PUBLISHED".equals(activity.getPublicationState())) {
            throw new ApiException(ErrorCode.ATTACHMENT_NOT_FOUND);
        }
        return download(row);
    }

    public ResponseEntity<Resource> adminCover(String activityId) {
        ActivityRow activity = activityService.requireActive(activityId, false);
        return cover(activity);
    }

    public ResponseEntity<Resource> publicCover(String activityId) {
        ActivityRow activity = activityMapper.findById(activityId);
        if (activity == null || activity.getDeletedAt() != null || !"PUBLISHED".equals(activity.getPublicationState())) {
            throw new ApiException(ErrorCode.ACTIVITY_UNAVAILABLE);
        }
        return cover(activity);
    }

    private ResponseEntity<Resource> cover(ActivityRow activity) {
        if (activity.getCoverStorageKey() == null) {
            throw new ApiException(ErrorCode.ATTACHMENT_NOT_FOUND, "活动还没有封面");
        }
        Resource resource = fileStorage.cover(activity.getCoverStorageKey());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(activity.getCoverContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate())
                .body(resource);
    }

    private ResponseEntity<Resource> download(AttachmentRow row) {
        Resource resource = fileStorage.attachment(row.getStorageKey());
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(row.getOriginalName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(row.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .cacheControl(CacheControl.noStore())
                .body(resource);
    }

    private AttachmentRow requireAttachment(String id) {
        AttachmentRow row = attachmentMapper.findById(id);
        if (row == null) {
            throw new ApiException(ErrorCode.ATTACHMENT_NOT_FOUND);
        }
        return row;
    }
}
