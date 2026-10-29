package com.campus.events.checkin;

import java.time.OffsetDateTime;

public class CheckinRecordRow {

    private String registrationId;
    private String sessionId;
    private OffsetDateTime checkedAt;

    public String getRegistrationId() { return registrationId; }
    public void setRegistrationId(String registrationId) { this.registrationId = registrationId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public OffsetDateTime getCheckedAt() { return checkedAt; }
    public void setCheckedAt(OffsetDateTime checkedAt) { this.checkedAt = checkedAt; }
}
