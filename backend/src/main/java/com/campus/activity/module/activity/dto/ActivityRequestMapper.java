package com.campus.activity.module.activity.dto;

import com.campus.activity.common.BusinessException;
import com.campus.activity.module.activity.entity.Activity;
import com.campus.activity.module.activity.entity.Attachment;
import com.campus.activity.util.InputRules;

import java.util.List;

/** Boundary mapping only: business invariants remain in domain policies. */
public final class ActivityRequestMapper {
    private ActivityRequestMapper() { }

    public static Activity activity(ActivitySaveRequest request) {
        Activity activity = new Activity();
        activity.setOrgId(request.orgId());
        activity.setVersion(request.version());
        activity.setCheckinPolicy(request.checkinPolicy());
        activity.setTitle(request.title());
        activity.setTypeCode(request.typeCode());
        activity.setIntroduction(request.introduction());
        activity.setLocation(request.location());
        activity.setLngLat(request.lngLat());
        activity.setLiveUrl(request.liveUrl());
        activity.setCoverUrl(request.coverUrl());
        activity.setStartTime(request.startTime());
        activity.setEndTime(request.endTime());
        activity.setSignupDeadline(request.signupDeadline());
        activity.setQuota(request.quota());
        activity.setCheckinRadius(request.checkinRadius() == null ? 500 : request.checkinRadius());
        return activity;
    }

    /** Null preserves old attachments on update; an empty list explicitly clears them. */
    public static List<Attachment> attachments(ActivitySaveRequest request) {
        if (request.attachments() == null) return null;
        return request.attachments().stream().map(item -> {
            InputRules.publicUrl(item.url(), "附件地址");
            if (item.fileSize() != null && (item.fileSize() < 0 || item.fileSize() > 20L * 1024 * 1024)) {
                throw new BusinessException("附件大小错误");
            }
            Attachment attachment = new Attachment();
            attachment.setFileName(item.fileName());
            attachment.setFileUrl(item.url());
            attachment.setFileSize(item.fileSize());
            return attachment;
        }).toList();
    }
}
