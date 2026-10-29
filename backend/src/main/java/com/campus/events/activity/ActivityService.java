package com.campus.events.activity;

import com.campus.events.attachment.AttachmentMapper;
import com.campus.events.attachment.AttachmentRow;
import com.campus.events.auth.AdminPrincipal;
import com.campus.events.auth.CurrentAdmin;
import com.campus.events.checkin.CheckinMapper;
import com.campus.events.common.ApiException;
import com.campus.events.common.ErrorCode;
import com.campus.events.common.Ids;
import com.campus.events.common.PageResult;
import com.campus.events.common.Pages;
import com.campus.events.common.TimeSupport;
import com.campus.events.common.Validators;
import com.campus.events.dictionary.DictionaryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ActivityService {

    private final ActivityMapper activityMapper;
    private final AttachmentMapper attachmentMapper;
    private final DictionaryMapper dictionaryMapper;
    private final CheckinMapper checkinMapper;
    private final ActivityViews views;

    public ActivityService(
            ActivityMapper activityMapper,
            AttachmentMapper attachmentMapper,
            DictionaryMapper dictionaryMapper,
            CheckinMapper checkinMapper,
            ActivityViews views) {
        this.activityMapper = activityMapper;
        this.attachmentMapper = attachmentMapper;
        this.dictionaryMapper = dictionaryMapper;
        this.checkinMapper = checkinMapper;
        this.views = views;
    }

    public PageResult<ActivityDtos.Summary> list(
            String keyword, String typeId, String status, OffsetDateTime dateFrom, OffsetDateTime dateTo,
            Integer page, Integer pageSize) {
        int current = Pages.page(page);
        int size = Pages.pageSize(pageSize, 50);
        ActivityQuery query = new ActivityQuery();
        query.setKeyword(Validators.likePattern(keyword));
        query.setTypeId(blankToNull(typeId));
        query.setStatus(blankToNull(status));
        query.setDateFrom(dateFrom);
        query.setDateTo(dateTo);
        query.setNow(TimeSupport.now());
        query.setLimit(size);
        query.setOffset((current - 1) * size);
        long total = activityMapper.countSearch(query);
        List<ActivityDtos.Summary> items = activityMapper.search(query).stream().map(views::toSummary).toList();
        return new PageResult<>(items, current, size, total);
    }

    public ActivityDtos.Detail getAdmin(String id) {
        return views.toDetail(requireActive(id, false), attachmentsOf(id));
    }

    public ActivityDtos.PublicView getPublic(String id) {
        ActivityRow row = activityMapper.findById(id);
        if (row == null || row.getDeletedAt() != null || !"PUBLISHED".equals(row.getPublicationState())) {
            throw new ApiException(ErrorCode.ACTIVITY_UNAVAILABLE);
        }
        return views.toPublic(row, attachmentsOf(id));
    }

    @Transactional
    public ActivityDtos.Created create(ActivityDtos.WriteRequest request) {
        ActivityInput input = validate(request, 1);
        ensureType(input.typeId());
        AdminPrincipal admin = CurrentAdmin.get();
        OffsetDateTime now = TimeSupport.now();
        ActivityRow row = new ActivityRow();
        row.setId(Ids.newId());
        row.setTypeId(input.typeId());
        row.setName(input.name());
        row.setDescription(input.description());
        row.setLocation(input.location());
        row.setStartAt(input.startAt());
        row.setEndAt(input.endAt());
        row.setRegistrationDeadline(input.registrationDeadline());
        row.setCapacity(input.capacity());
        row.setPublicationState("DRAFT");
        row.setLiveUrl(input.liveUrl());
        row.setCreatedBy(admin.id());
        row.setVersion(1);
        row.setCreatedAt(now);
        activityMapper.insert(row);
        return new ActivityDtos.Created(row.getId());
    }

    @Transactional
    public ActivityDtos.Detail update(String id, ActivityDtos.WriteRequest request) {
        if (request == null || request.version() == null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "缺少活动版本号");
        }
        ActivityRow locked = lockExisting(id);
        if (locked.getVersion() != request.version()) {
            throw new ApiException(ErrorCode.VERSION_CONFLICT);
        }
        ActivityInput input = validate(request, locked.getRegisteredCount());
        ensureType(input.typeId());
        boolean scheduleChanged = !locked.getLocation().equals(input.location())
                || !locked.getStartAt().toInstant().equals(input.startAt().toInstant())
                || !locked.getEndAt().toInstant().equals(input.endAt().toInstant());
        locked.setTypeId(input.typeId());
        locked.setName(input.name());
        locked.setDescription(input.description());
        locked.setLocation(input.location());
        locked.setStartAt(input.startAt());
        locked.setEndAt(input.endAt());
        locked.setRegistrationDeadline(input.registrationDeadline());
        locked.setCapacity(input.capacity());
        locked.setLiveUrl(input.liveUrl());
        locked.setUpdatedAt(TimeSupport.now());
        if (activityMapper.updateContent(locked) == 0) {
            throw new ApiException(ErrorCode.VERSION_CONFLICT);
        }
        if (scheduleChanged) {
            checkinMapper.disableEnabled(id);
        }
        return getAdmin(id);
    }

    @Transactional
    public void delete(String id) {
        ActivityRow locked = lockExisting(id);
        OffsetDateTime now = TimeSupport.now();
        if (activityMapper.markDeleted(id, now, locked.getVersion()) == 0) {
            throw new ApiException(ErrorCode.VERSION_CONFLICT);
        }
        checkinMapper.disableEnabled(id);
    }

    @Transactional
    public ActivityDtos.PublishResult publish(String id) {
        ActivityRow locked = lockExisting(id);
        OffsetDateTime now = TimeSupport.now();
        if ("DRAFT".equals(locked.getPublicationState())) {
            if (activityMapper.publishIfDraft(id, now, locked.getVersion()) == 0) {
                throw new ApiException(ErrorCode.VERSION_CONFLICT);
            }
        }
        ActivityRow fresh = activityMapper.findById(id);
        OffsetDateTime validFrom = fresh.getPublishedAt() == null ? now : fresh.getPublishedAt();
        return new ActivityDtos.PublishResult(
                id,
                views.shareUrl(id),
                views.registrationUrl(id),
                validFrom,
                fresh.getRegistrationDeadline());
    }

    public ActivityRow requireActive(String id, boolean lock) {
        ActivityRow row = lock ? activityMapper.lockById(id) : activityMapper.findById(id);
        if (row == null) {
            throw new ApiException(ErrorCode.ACTIVITY_NOT_FOUND);
        }
        if (row.getDeletedAt() != null) {
            throw new ApiException(410, "ACTIVITY_UNAVAILABLE", "活动已失效");
        }
        return row;
    }

    public ActivityRow lockExisting(String id) {
        return requireActive(id, true);
    }

    private ActivityInput validate(ActivityDtos.WriteRequest request, int minCapacity) {
        if (request == null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "请填写活动信息");
        }
        return Validators.activity(
                request.name(),
                request.typeId(),
                request.description(),
                request.location(),
                request.startAt(),
                request.endAt(),
                request.registrationDeadline(),
                request.capacity(),
                request.liveUrl(),
                minCapacity);
    }

    private void ensureType(String typeId) {
        if (dictionaryMapper.countType(typeId) == 0) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "请选择有效的活动类型");
        }
    }

    private List<ActivityDtos.AttachmentSummary> attachmentsOf(String activityId) {
        return attachmentMapper.listByActivity(activityId).stream()
                .map(this::toAttachment)
                .toList();
    }

    private ActivityDtos.AttachmentSummary toAttachment(AttachmentRow row) {
        return new ActivityDtos.AttachmentSummary(
                row.getId(), row.getOriginalName(), row.getContentType(), row.getSizeBytes(), row.getCreatedAt());
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
