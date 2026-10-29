package com.campus.events.checkin;

import com.campus.events.activity.ActivityMapper;
import com.campus.events.activity.ActivityRow;
import com.campus.events.activity.ActivityService;
import com.campus.events.common.ApiException;
import com.campus.events.common.ErrorCode;
import com.campus.events.common.Ids;
import com.campus.events.common.TimeSupport;
import com.campus.events.common.Validators;
import com.campus.events.registration.RegistrationMapper;
import com.campus.events.registration.RegistrationRow;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;

@Service
public class CheckinService {

    private final ActivityService activityService;
    private final ActivityMapper activityMapper;
    private final CheckinMapper checkinMapper;
    private final RegistrationMapper registrationMapper;
    private final ChallengeTokens tokens;

    public CheckinService(
            ActivityService activityService,
            ActivityMapper activityMapper,
            CheckinMapper checkinMapper,
            RegistrationMapper registrationMapper,
            ChallengeTokens tokens) {
        this.activityService = activityService;
        this.activityMapper = activityMapper;
        this.checkinMapper = checkinMapper;
        this.registrationMapper = registrationMapper;
        this.tokens = tokens;
    }

    @Transactional
    public CheckinDtos.Session open(String activityId, CheckinDtos.OpenRequest request) {
        ActivityRow activity = activityMapper.lockById(activityId);
        ensurePublished(activity);
        if (request == null || request.mode() == null
                || (!"BASIC".equals(request.mode()) && !"DYNAMIC".equals(request.mode()))) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "请选择基本签到或动态签到");
        }
        if (request.windowStart() == null || request.windowEnd() == null
                || !request.windowStart().isBefore(request.windowEnd())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "签到开始时间必须早于结束时间");
        }
        checkinMapper.disableEnabled(activityId);
        CheckinSessionRow row = new CheckinSessionRow();
        row.setId(Ids.newId());
        row.setActivityId(activityId);
        row.setMode(request.mode());
        row.setWindowStart(request.windowStart());
        row.setWindowEnd(request.windowEnd());
        row.setTokenVersion(1);
        row.setEnabled(true);
        row.setCreatedAt(TimeSupport.now());
        checkinMapper.insert(row);
        return toSession(row);
    }

    @Transactional
    public CheckinDtos.Session close(String sessionId) {
        CheckinSessionRow existing = checkinMapper.findById(sessionId);
        if (existing == null) {
            throw new ApiException(ErrorCode.SESSION_NOT_FOUND);
        }
        activityMapper.lockById(existing.getActivityId());
        CheckinSessionRow locked = checkinMapper.lockById(sessionId);
        if (locked.isEnabled()) {
            checkinMapper.close(sessionId);
            locked = checkinMapper.findById(sessionId);
        }
        return toSession(locked);
    }

    public CheckinDtos.Summary summary(String activityId) {
        activityService.requireActive(activityId, false);
        CheckinSessionRow session = checkinMapper.latest(activityId);
        return new CheckinDtos.Summary(
                activityId,
                registrationMapper.countByActivity(activityId),
                checkinMapper.countByActivity(activityId),
                session == null ? null : session.getId(),
                session == null ? null : session.getMode(),
                session != null && session.isEnabled(),
                session == null ? null : session.getWindowStart(),
                session == null ? null : session.getWindowEnd());
    }

    public CheckinDtos.Challenge challenge(String sessionId) {
        CheckinSessionRow session = requireEnabled(sessionId);
        if (!"DYNAMIC".equals(session.getMode())) {
            throw new ApiException(ErrorCode.CHECKIN_NOT_DYNAMIC);
        }
        Instant now = Instant.now();
        long expiresAt = ((now.getEpochSecond() / 30) + 1) * 30;
        String token = tokens.issue(session.getId(), session.getActivityId(), session.getTokenVersion(), now);
        OffsetDateTime serverTime = TimeSupport.now();
        OffsetDateTime expires = OffsetDateTime.ofInstant(Instant.ofEpochSecond(expiresAt), TimeSupport.ZONE);
        long refreshAfter = Math.max(1, expiresAt - now.getEpochSecond());
        return new CheckinDtos.Challenge(token, serverTime, expires, refreshAfter);
    }

    public CheckinDtos.Context context(CheckinDtos.ContextRequest request) {
        if (request == null || request.sessionId() == null || request.sessionId().isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "缺少签到会话");
        }
        Prepared prepared = prepare(request.sessionId(), request.token(), false);
        long seconds = Math.max(0, Duration.between(prepared.now, prepared.validUntil).getSeconds());
        return new CheckinDtos.Context(
                prepared.session.getId(),
                prepared.activity.getId(),
                prepared.activity.getName(),
                prepared.session.getMode(),
                seconds,
                prepared.validUntil);
    }

    @Transactional
    public CheckinDtos.Result submit(CheckinDtos.SubmitRequest request) {
        if (request == null || request.sessionId() == null || request.sessionId().isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "缺少签到会话");
        }
        CheckinSessionRow preview = checkinMapper.findById(request.sessionId());
        if (preview == null) {
            throw new ApiException(ErrorCode.SESSION_NOT_FOUND);
        }
        activityMapper.lockById(preview.getActivityId());
        CheckinSessionRow session = checkinMapper.lockById(request.sessionId());
        Prepared prepared = prepareLocked(session, request.token());
        String phone = Validators.phone(request.phone());
        RegistrationRow registration = registrationMapper.findByActivityAndPhone(prepared.activity.getId(), phone);
        if (registration == null) {
            throw new ApiException(ErrorCode.NOT_REGISTERED);
        }
        CheckinRecordRow existing = checkinMapper.findRecord(registration.getId());
        if (existing != null) {
            return new CheckinDtos.Result(
                    existing.getRegistrationId(), existing.getCheckedAt(), prepared.activity.getName(), true);
        }
        CheckinRecordRow record = new CheckinRecordRow();
        record.setRegistrationId(registration.getId());
        record.setSessionId(session.getId());
        record.setCheckedAt(prepared.now);
        try {
            checkinMapper.insertRecord(record);
        } catch (DataIntegrityViolationException exception) {
            CheckinRecordRow raced = checkinMapper.findRecord(registration.getId());
            return new CheckinDtos.Result(
                    raced.getRegistrationId(), raced.getCheckedAt(), prepared.activity.getName(), true);
        }
        return new CheckinDtos.Result(registration.getId(), prepared.now, prepared.activity.getName(), false);
    }

    private Prepared prepare(String sessionId, String token, boolean locked) {
        CheckinSessionRow session = locked ? checkinMapper.lockById(sessionId) : checkinMapper.findById(sessionId);
        if (session == null) {
            throw new ApiException(ErrorCode.SESSION_NOT_FOUND);
        }
        return prepareLocked(session, token);
    }

    private Prepared prepareLocked(CheckinSessionRow session, String token) {
        if (!session.isEnabled()) {
            throw new ApiException(ErrorCode.CHECKIN_CLOSED);
        }
        OffsetDateTime now = TimeSupport.now();
        if (now.isBefore(session.getWindowStart()) || now.isAfter(session.getWindowEnd())) {
            throw new ApiException(ErrorCode.CHECKIN_NOT_OPEN);
        }
        ActivityRow activity = activityMapper.findById(session.getActivityId());
        ensurePublished(activity);
        OffsetDateTime validUntil;
        if ("DYNAMIC".equals(session.getMode())) {
            ChallengeTokens.Decoded decoded = tokens.verify(token, now.toInstant());
            if (!session.getId().equals(decoded.sessionId())
                    || !session.getActivityId().equals(decoded.activityId())
                    || session.getTokenVersion() != decoded.version()) {
                throw new ApiException(ErrorCode.CHECKIN_TOKEN_INVALID);
            }
            validUntil = OffsetDateTime.ofInstant(decoded.expiresAt(), TimeSupport.ZONE);
        } else {
            validUntil = session.getWindowEnd();
        }
        return new Prepared(session, activity, now, validUntil);
    }

    private CheckinSessionRow requireEnabled(String sessionId) {
        CheckinSessionRow session = checkinMapper.findById(sessionId);
        if (session == null) {
            throw new ApiException(ErrorCode.SESSION_NOT_FOUND);
        }
        if (!session.isEnabled()) {
            throw new ApiException(ErrorCode.CHECKIN_CLOSED);
        }
        return session;
    }

    private static void ensurePublished(ActivityRow activity) {
        if (activity == null) {
            throw new ApiException(ErrorCode.ACTIVITY_NOT_FOUND);
        }
        if (activity.getDeletedAt() != null) {
            throw new ApiException(410, "ACTIVITY_UNAVAILABLE", "活动已失效");
        }
        if (!"PUBLISHED".equals(activity.getPublicationState())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "请先发布活动再开启签到");
        }
    }

    private static CheckinDtos.Session toSession(CheckinSessionRow row) {
        return new CheckinDtos.Session(
                row.getId(),
                row.getActivityId(),
                row.getMode(),
                row.getWindowStart(),
                row.getWindowEnd(),
                row.isEnabled(),
                row.getCreatedAt());
    }

    private record Prepared(
            CheckinSessionRow session, ActivityRow activity, OffsetDateTime now, OffsetDateTime validUntil) {
    }
}
