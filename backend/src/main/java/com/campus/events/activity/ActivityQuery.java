package com.campus.events.activity;

import java.time.OffsetDateTime;

public class ActivityQuery {

    private String keyword;
    private String typeId;
    private String status;
    private OffsetDateTime dateFrom;
    private OffsetDateTime dateTo;
    private OffsetDateTime now;
    private int limit;
    private int offset;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(OffsetDateTime dateFrom) {
        this.dateFrom = dateFrom;
    }

    public OffsetDateTime getDateTo() {
        return dateTo;
    }

    public void setDateTo(OffsetDateTime dateTo) {
        this.dateTo = dateTo;
    }

    public OffsetDateTime getNow() {
        return now;
    }

    public void setNow(OffsetDateTime now) {
        this.now = now;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }
}
