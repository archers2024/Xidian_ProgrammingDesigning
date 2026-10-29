package com.campus.events.checkin;

import java.time.OffsetDateTime;

public class CheckinSessionRow {

    private String id;
    private String activityId;
    private String mode;
    private OffsetDateTime windowStart;
    private OffsetDateTime windowEnd;
    private int tokenVersion;
    private boolean enabled;
    private OffsetDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getActivityId() { return activityId; }
    public void setActivityId(String activityId) { this.activityId = activityId; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public OffsetDateTime getWindowStart() { return windowStart; }
    public void setWindowStart(OffsetDateTime windowStart) { this.windowStart = windowStart; }
    public OffsetDateTime getWindowEnd() { return windowEnd; }
    public void setWindowEnd(OffsetDateTime windowEnd) { this.windowEnd = windowEnd; }
    public int getTokenVersion() { return tokenVersion; }
    public void setTokenVersion(int tokenVersion) { this.tokenVersion = tokenVersion; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
