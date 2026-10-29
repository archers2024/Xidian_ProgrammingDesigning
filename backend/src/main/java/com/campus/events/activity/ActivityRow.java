package com.campus.events.activity;

import java.time.OffsetDateTime;

public class ActivityRow {

    private String id;
    private String typeId;
    private String typeName;
    private String name;
    private String description;
    private String location;
    private OffsetDateTime startAt;
    private OffsetDateTime endAt;
    private OffsetDateTime registrationDeadline;
    private int capacity;
    private int registeredCount;
    private String publicationState;
    private OffsetDateTime publishedAt;
    private String liveUrl;
    private String coverStorageKey;
    private String coverContentType;
    private String createdBy;
    private int version;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTypeId() { return typeId; }
    public void setTypeId(String typeId) { this.typeId = typeId; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public OffsetDateTime getStartAt() { return startAt; }
    public void setStartAt(OffsetDateTime startAt) { this.startAt = startAt; }
    public OffsetDateTime getEndAt() { return endAt; }
    public void setEndAt(OffsetDateTime endAt) { this.endAt = endAt; }
    public OffsetDateTime getRegistrationDeadline() { return registrationDeadline; }
    public void setRegistrationDeadline(OffsetDateTime registrationDeadline) { this.registrationDeadline = registrationDeadline; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public int getRegisteredCount() { return registeredCount; }
    public void setRegisteredCount(int registeredCount) { this.registeredCount = registeredCount; }
    public String getPublicationState() { return publicationState; }
    public void setPublicationState(String publicationState) { this.publicationState = publicationState; }
    public OffsetDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(OffsetDateTime publishedAt) { this.publishedAt = publishedAt; }
    public String getLiveUrl() { return liveUrl; }
    public void setLiveUrl(String liveUrl) { this.liveUrl = liveUrl; }
    public String getCoverStorageKey() { return coverStorageKey; }
    public void setCoverStorageKey(String coverStorageKey) { this.coverStorageKey = coverStorageKey; }
    public String getCoverContentType() { return coverContentType; }
    public void setCoverContentType(String coverContentType) { this.coverContentType = coverContentType; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }
}
