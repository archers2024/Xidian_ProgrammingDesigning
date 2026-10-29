package com.campus.events.common;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class TimeSupport {

    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private TimeSupport() {
    }

    public static OffsetDateTime now() {
        return OffsetDateTime.now(ZONE);
    }
}
