package com.campus.events.checkin;

import java.time.OffsetDateTime;

public final class CheckinDtos {

    private CheckinDtos() {
    }

    public record OpenRequest(String mode, OffsetDateTime windowStart, OffsetDateTime windowEnd) {
    }

    public record Session(
            String id,
            String activityId,
            String mode,
            OffsetDateTime windowStart,
            OffsetDateTime windowEnd,
            boolean enabled,
            OffsetDateTime createdAt) {
    }

    public record Summary(
            String activityId,
            int registeredCount,
            int checkedInCount,
            String sessionId,
            String sessionMode,
            boolean sessionEnabled,
            OffsetDateTime windowStart,
            OffsetDateTime windowEnd) {
    }

    public record Challenge(String token, OffsetDateTime serverTime, OffsetDateTime expiresAt, long refreshAfterSeconds) {
    }

    public record ContextRequest(String sessionId, String token) {
    }

    public record Context(
            String sessionId,
            String activityId,
            String activityName,
            String mode,
            long expiresInSeconds,
            OffsetDateTime validUntil) {
    }

    public record SubmitRequest(String sessionId, String phone, String token) {
    }

    public record Result(String checkinId, OffsetDateTime checkedAt, String activityName, boolean alreadyCheckedIn) {
    }
}
