package com.campus.events.activity;

import java.time.OffsetDateTime;
import java.util.List;

public final class ActivityDtos {

    private ActivityDtos() {
    }

    public record WriteRequest(
            String name,
            String typeId,
            String description,
            String location,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            OffsetDateTime registrationDeadline,
            Integer capacity,
            String liveUrl,
            Integer version) {
    }

    public record Created(String id) {
    }

    public record AttachmentSummary(
            String id,
            String originalName,
            String contentType,
            long sizeBytes,
            OffsetDateTime createdAt) {
    }

    public record Summary(
            String id,
            String name,
            String typeId,
            String typeName,
            String location,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            OffsetDateTime registrationDeadline,
            int capacity,
            int registeredCount,
            String publicationState,
            OffsetDateTime publishedAt,
            String displayStatus,
            boolean canRegister,
            String unavailableReason,
            int version,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }

    public record Detail(
            String id,
            String name,
            String typeId,
            String typeName,
            String location,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            OffsetDateTime registrationDeadline,
            int capacity,
            int registeredCount,
            String publicationState,
            OffsetDateTime publishedAt,
            String displayStatus,
            boolean canRegister,
            String unavailableReason,
            int version,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String description,
            String liveUrl,
            String coverUrl,
            List<AttachmentSummary> attachments,
            String shareUrl,
            String registrationUrl) {
    }

    public record PublishResult(
            String activityId,
            String shareUrl,
            String registrationUrl,
            OffsetDateTime validFrom,
            OffsetDateTime validUntil) {
    }

    public record PublicView(
            String id,
            String name,
            String typeName,
            String description,
            String location,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            OffsetDateTime registrationDeadline,
            int capacity,
            int registeredCount,
            String displayStatus,
            boolean canRegister,
            String unavailableReason,
            String liveUrl,
            String coverUrl,
            List<AttachmentSummary> attachments,
            OffsetDateTime publishedAt) {
    }

    public record StatusCounts(long total, long draft, long registering, long closed, long ended) {
    }
}
