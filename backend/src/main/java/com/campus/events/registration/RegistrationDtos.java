package com.campus.events.registration;

import java.time.OffsetDateTime;

public final class RegistrationDtos {

    private RegistrationDtos() {
    }

    public record CreateRequest(String name, String phone, String collegeId) {
    }

    public record Result(
            String registrationId,
            OffsetDateTime registeredAt,
            String activityName,
            OffsetDateTime startAt,
            String location) {
    }

    public record Summary(
            String id,
            String name,
            String phone,
            String collegeId,
            String collegeName,
            OffsetDateTime registeredAt,
            boolean checkedIn,
            OffsetDateTime checkedInAt) {
    }
}
