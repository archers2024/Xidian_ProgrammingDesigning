package com.campus.events.activity;

import com.campus.events.common.TimeSupport;
import com.campus.events.config.AppProperties;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class ActivityViews {

    private final AppProperties properties;

    public ActivityViews(AppProperties properties) {
        this.properties = properties;
    }

    public String displayStatus(ActivityRow row, OffsetDateTime now) {
        if ("DRAFT".equals(row.getPublicationState())) {
            return "DRAFT";
        }
        if (!now.isBefore(row.getEndAt())) {
            return "ENDED";
        }
        if (!now.isBefore(row.getRegistrationDeadline())) {
            return "CLOSED";
        }
        return "REGISTERING";
    }

    public String unavailableReason(ActivityRow row, OffsetDateTime now) {
        if (row.getDeletedAt() != null) {
            return "ACTIVITY_DELETED";
        }
        if ("DRAFT".equals(row.getPublicationState())) {
            return "NOT_PUBLISHED";
        }
        if (!now.isBefore(row.getEndAt())) {
            return "ACTIVITY_ENDED";
        }
        if (!now.isBefore(row.getRegistrationDeadline())) {
            return "REGISTRATION_CLOSED";
        }
        if (row.getRegisteredCount() >= row.getCapacity()) {
            return "CAPACITY_FULL";
        }
        return null;
    }

    public ActivityDtos.Summary toSummary(ActivityRow row) {
        OffsetDateTime now = TimeSupport.now();
        String reason = unavailableReason(row, now);
        return new ActivityDtos.Summary(
                row.getId(),
                row.getName(),
                row.getTypeId(),
                row.getTypeName(),
                row.getLocation(),
                row.getStartAt(),
                row.getEndAt(),
                row.getRegistrationDeadline(),
                row.getCapacity(),
                row.getRegisteredCount(),
                row.getPublicationState(),
                row.getPublishedAt(),
                displayStatus(row, now),
                reason == null,
                reason,
                row.getVersion(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    public ActivityDtos.Detail toDetail(ActivityRow row, List<ActivityDtos.AttachmentSummary> attachments) {
        ActivityDtos.Summary summary = toSummary(row);
        boolean published = row.getDeletedAt() == null && "PUBLISHED".equals(row.getPublicationState());
        return new ActivityDtos.Detail(
                summary.id(),
                summary.name(),
                summary.typeId(),
                summary.typeName(),
                summary.location(),
                summary.startAt(),
                summary.endAt(),
                summary.registrationDeadline(),
                summary.capacity(),
                summary.registeredCount(),
                summary.publicationState(),
                summary.publishedAt(),
                summary.displayStatus(),
                summary.canRegister(),
                summary.unavailableReason(),
                summary.version(),
                summary.createdAt(),
                summary.updatedAt(),
                row.getDescription(),
                row.getLiveUrl(),
                coverUrl(row, false),
                attachments,
                published ? shareUrl(row.getId()) : null,
                published ? registrationUrl(row.getId()) : null);
    }

    public ActivityDtos.PublicView toPublic(ActivityRow row, List<ActivityDtos.AttachmentSummary> attachments) {
        OffsetDateTime now = TimeSupport.now();
        String reason = unavailableReason(row, now);
        return new ActivityDtos.PublicView(
                row.getId(),
                row.getName(),
                row.getTypeName(),
                row.getDescription(),
                row.getLocation(),
                row.getStartAt(),
                row.getEndAt(),
                row.getRegistrationDeadline(),
                row.getCapacity(),
                row.getRegisteredCount(),
                displayStatus(row, now),
                reason == null,
                reason,
                row.getLiveUrl(),
                coverUrl(row, true),
                attachments,
                row.getPublishedAt());
    }

    public String shareUrl(String activityId) {
        return base() + "/events/" + activityId;
    }

    public String registrationUrl(String activityId) {
        return base() + "/events/" + activityId + "/register";
    }

    public String coverUrl(ActivityRow row, boolean publicPage) {
        if (row.getCoverStorageKey() == null || row.getCoverStorageKey().isBlank()) {
            return null;
        }
        if (publicPage) {
            return "/api/v1/public/activities/" + row.getId() + "/cover";
        }
        if (row.getDeletedAt() == null && "PUBLISHED".equals(row.getPublicationState())) {
            return "/api/v1/public/activities/" + row.getId() + "/cover";
        }
        return "/api/v1/admin/activities/" + row.getId() + "/cover";
    }

    private String base() {
        String base = properties.publicBaseUrl() == null ? "http://localhost:5173" : properties.publicBaseUrl();
        if (base.endsWith("/")) {
            return base.substring(0, base.length() - 1);
        }
        return base;
    }
}
