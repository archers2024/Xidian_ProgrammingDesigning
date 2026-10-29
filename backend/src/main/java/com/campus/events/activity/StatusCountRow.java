package com.campus.events.activity;

public class StatusCountRow {

    private long total;
    private long draft;
    private long registering;
    private long closed;
    private long ended;

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public long getDraft() { return draft; }
    public void setDraft(long draft) { this.draft = draft; }
    public long getRegistering() { return registering; }
    public void setRegistering(long registering) { this.registering = registering; }
    public long getClosed() { return closed; }
    public void setClosed(long closed) { this.closed = closed; }
    public long getEnded() { return ended; }
    public void setEnded(long ended) { this.ended = ended; }
}
