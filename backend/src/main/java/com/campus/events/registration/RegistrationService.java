package com.campus.events.registration;

import com.campus.events.activity.ActivityMapper;
import com.campus.events.activity.ActivityRow;
import com.campus.events.activity.ActivityService;
import com.campus.events.common.ApiException;
import com.campus.events.common.ErrorCode;
import com.campus.events.common.Ids;
import com.campus.events.common.PageResult;
import com.campus.events.common.Pages;
import com.campus.events.common.TimeSupport;
import com.campus.events.common.Validators;
import com.campus.events.dictionary.DictionaryMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class RegistrationService {

    private final ActivityMapper activityMapper;
    private final ActivityService activityService;
    private final RegistrationMapper registrationMapper;
    private final DictionaryMapper dictionaryMapper;

    public RegistrationService(
            ActivityMapper activityMapper,
            ActivityService activityService,
            RegistrationMapper registrationMapper,
            DictionaryMapper dictionaryMapper) {
        this.activityMapper = activityMapper;
        this.activityService = activityService;
        this.registrationMapper = registrationMapper;
        this.dictionaryMapper = dictionaryMapper;
    }

    @Transactional
    public RegistrationDtos.Result register(String activityId, RegistrationDtos.CreateRequest request) {
        ActivityRow activity = activityMapper.lockById(activityId);
        if (activity == null) {
            throw new ApiException(ErrorCode.ACTIVITY_NOT_FOUND);
        }
        if (activity.getDeletedAt() != null) {
            throw new ApiException(410, "ACTIVITY_UNAVAILABLE", "活动已失效");
        }
        if (!"PUBLISHED".equals(activity.getPublicationState())) {
            throw new ApiException(ErrorCode.ACTIVITY_UNAVAILABLE);
        }
        OffsetDateTime now = TimeSupport.now();
        if (!now.isBefore(activity.getEndAt()) || !now.isBefore(activity.getRegistrationDeadline())) {
            throw new ApiException(ErrorCode.REGISTRATION_CLOSED);
        }
        if (request == null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "请填写报名信息");
        }
        String name = Validators.personName(request.name());
        String phone = Validators.phone(request.phone());
        String collegeId = request.collegeId() == null ? "" : request.collegeId().trim();
        if (dictionaryMapper.countCollege(collegeId) == 0) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "请选择所属学院");
        }
        if (registrationMapper.findByActivityAndPhone(activityId, phone) != null) {
            throw new ApiException(ErrorCode.REGISTRATION_DUPLICATE);
        }
        if (registrationMapper.countByActivity(activityId) >= activity.getCapacity()) {
            throw new ApiException(ErrorCode.REGISTRATION_FULL);
        }
        RegistrationRow row = new RegistrationRow();
        row.setId(Ids.newId());
        row.setActivityId(activityId);
        row.setParticipantName(name);
        row.setPhone(phone);
        row.setCollegeId(collegeId);
        row.setCreatedAt(now);
        try {
            registrationMapper.insert(row);
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(ErrorCode.REGISTRATION_DUPLICATE);
        }
        return new RegistrationDtos.Result(row.getId(), now, activity.getName(), activity.getStartAt(), activity.getLocation());
    }

    public PageResult<RegistrationDtos.Summary> list(String activityId, Integer page, Integer pageSize) {
        activityService.requireActive(activityId, false);
        int current = Pages.page(page);
        int size = Pages.pageSize(pageSize, 100);
        long total = registrationMapper.countByActivityId(activityId);
        List<RegistrationDtos.Summary> items = registrationMapper.listByActivity(activityId, size, (current - 1) * size)
                .stream()
                .map(row -> new RegistrationDtos.Summary(
                        row.getId(),
                        row.getName(),
                        row.getPhone(),
                        row.getCollegeId(),
                        row.getCollegeName(),
                        row.getRegisteredAt(),
                        row.getCheckedInAt() != null,
                        row.getCheckedInAt()))
                .toList();
        return new PageResult<>(items, current, size, total);
    }
}
