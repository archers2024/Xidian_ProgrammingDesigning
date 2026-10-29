package com.campus.events.activity;

import java.time.OffsetDateTime;

public record ActivityInput(
        String name,
        String typeId,
        String description,
        String location,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        OffsetDateTime registrationDeadline,
        int capacity,
        String liveUrl) {
}
