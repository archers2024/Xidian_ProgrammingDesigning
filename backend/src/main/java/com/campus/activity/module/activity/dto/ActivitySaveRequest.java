package com.campus.activity.module.activity.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/** Explicit writable fields prevent mass assignment of lifecycle, counters and ownership metadata. */
public record ActivitySaveRequest(
        Long orgId,
        Long version,
        String checkinPolicy,
        @JsonDeserialize(using = ActivityTextDeserializer.class) String title,
        @JsonDeserialize(using = ActivityTextDeserializer.class) String typeCode,
        @JsonDeserialize(using = ActivityTextDeserializer.class) String introduction,
        @JsonDeserialize(using = ActivityTextDeserializer.class) String location,
        @JsonDeserialize(using = ActivityTextDeserializer.class) String lngLat,
        @JsonDeserialize(using = ActivityTextDeserializer.class) String liveUrl,
        @JsonDeserialize(using = ActivityTextDeserializer.class) String coverUrl,
        @JsonDeserialize(using = ActivityTimeDeserializer.class) LocalDateTime startTime,
        @JsonDeserialize(using = ActivityTimeDeserializer.class) LocalDateTime endTime,
        @JsonDeserialize(using = ActivityTimeDeserializer.class) LocalDateTime signupDeadline,
        Integer quota,
        Integer checkinRadius,
        @Size(max = 100, message = "附件数量过多") List<@NotNull(message = "附件不能为空") @Valid AttachmentRequest> attachments) {

    public record AttachmentRequest(
            @NotBlank(message = "请填写附件名称") @Size(max = 200, message = "附件名称过长") @JsonDeserialize(using = ActivityTextDeserializer.class) String fileName,
            @NotBlank(message = "请填写附件地址") @Size(max = 500, message = "附件地址过长") @JsonDeserialize(using = ActivityTextDeserializer.class) String url,
            Long fileSize) { }
}
