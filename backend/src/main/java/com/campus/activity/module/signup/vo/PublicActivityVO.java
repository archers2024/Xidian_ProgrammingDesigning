package com.campus.activity.module.signup.vo;

import com.campus.activity.module.activity.entity.Activity;

import java.time.LocalDateTime;

/**
 * 公开宣传页视图对象（UI-08）。
 * 只承载海报页真正需要的字段，签到密钥、签到半径、创建人等内部信息一律不出现在公开响应里。
 */
public class PublicActivityVO {

    private Long activityId;
    private String title;
    private String typeCode;
    private String introduction;
    private String location;
    private String liveUrl;
    private String coverUrl;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime signupDeadline;
    private LocalDateTime publishTime;
    private Integer quota;
    private Integer signedCount;
    private String status;

    public static PublicActivityVO of(Activity a) {
        PublicActivityVO v = new PublicActivityVO();
        v.activityId = a.getActivityId();
        v.title = a.getTitle();
        v.typeCode = a.getTypeCode();
        v.introduction = a.getIntroduction();
        v.location = a.getLocation();
        v.liveUrl = a.getLiveUrl();
        v.coverUrl = a.getCoverUrl();
        v.startTime = a.getStartTime();
        v.endTime = a.getEndTime();
        v.signupDeadline = a.getSignupDeadline();
        v.publishTime = a.getPublishTime();
        v.quota = a.getQuota();
        v.signedCount = a.getSignedCount();
        v.status = a.getStatus();
        return v;
    }

    public Long getActivityId() {
        return activityId;
    }

    public String getTitle() {
        return title;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public String getIntroduction() {
        return introduction;
    }

    public String getLocation() {
        return location;
    }

    public String getLiveUrl() {
        return liveUrl;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public LocalDateTime getSignupDeadline() {
        return signupDeadline;
    }

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public Integer getQuota() {
        return quota;
    }

    public Integer getSignedCount() {
        return signedCount;
    }

    public String getStatus() {
        return status;
    }
}
