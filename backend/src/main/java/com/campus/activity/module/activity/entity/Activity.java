package com.campus.activity.module.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;


import java.time.LocalDateTime;

/**
 * 活动表 t_activity（Owner：B岗）
 * 状态取值：DRAFT 未发布 / OPEN 报名中 / CLOSED 已截止 / ONGOING 进行中 / FINISHED 已结束 / CANCELED 已取消
 */

@TableName("t_activity")
public class Activity {

    @TableId(type = IdType.AUTO)
    private Long activityId;

    private String activityNo;

    private String signupGuid;

    private String title;

    private String typeCode;

    private String introduction;

    private String location;

    /** 格式 lng,lat */
    private String lngLat;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime signupDeadline;

    private Integer quota;

    private Integer signedCount;

    private String status;

    private Integer checkinEnabled;

    private String checkinSecret;

    private Integer checkinRadius;

    private String liveUrl;

    private String coverUrl;

    private LocalDateTime publishTime;

    private Long createdBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public String getActivityNo() {
        return activityNo;
    }

    public void setActivityNo(String activityNo) {
        this.activityNo = activityNo;
    }

    public String getSignupGuid() {
        return signupGuid;
    }

    public void setSignupGuid(String signupGuid) {
        this.signupGuid = signupGuid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLngLat() {
        return lngLat;
    }

    public void setLngLat(String lngLat) {
        this.lngLat = lngLat;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getSignupDeadline() {
        return signupDeadline;
    }

    public void setSignupDeadline(LocalDateTime signupDeadline) {
        this.signupDeadline = signupDeadline;
    }

    public Integer getQuota() {
        return quota;
    }

    public void setQuota(Integer quota) {
        this.quota = quota;
    }

    public Integer getSignedCount() {
        return signedCount;
    }

    public void setSignedCount(Integer signedCount) {
        this.signedCount = signedCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCheckinEnabled() {
        return checkinEnabled;
    }

    public void setCheckinEnabled(Integer checkinEnabled) {
        this.checkinEnabled = checkinEnabled;
    }

    public String getCheckinSecret() {
        return checkinSecret;
    }

    public void setCheckinSecret(String checkinSecret) {
        this.checkinSecret = checkinSecret;
    }

    public Integer getCheckinRadius() {
        return checkinRadius;
    }

    public void setCheckinRadius(Integer checkinRadius) {
        this.checkinRadius = checkinRadius;
    }

    public String getLiveUrl() {
        return liveUrl;
    }

    public void setLiveUrl(String liveUrl) {
        this.liveUrl = liveUrl;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
