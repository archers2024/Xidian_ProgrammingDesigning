package com.campus.activity.module.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import java.time.LocalDateTime;

@TableName("t_activity_review")
public class ActivityReview {
    @TableId(type = IdType.AUTO)
    private Long reviewId;
    private Long activityId;
    private Long orgId;
    private Long submittedBy;
    private Long reviewerId;
    private String titleSnapshot;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String contentSnapshot;
    private String snapshotHash;
    private Long activityVersion;
    public String getTitleSnapshot(){return titleSnapshot;} public void setTitleSnapshot(String v){titleSnapshot=v;}
    public String getContentSnapshot(){return contentSnapshot;} public void setContentSnapshot(String v){contentSnapshot=v;}
    public String getSnapshotHash(){return snapshotHash;} public void setSnapshotHash(String v){snapshotHash=v;}
    public Long getActivityVersion(){return activityVersion;} public void setActivityVersion(Long v){activityVersion=v;}
    private String decision;
    private String comment;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    @TableField(exist = false)
    private String activityTitle;
    @TableField(exist = false)
    private String orgName;
    @TableField(exist = false)
    private String submitterName;

    public Long getReviewId() {
        return reviewId;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Long getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(Long submittedBy) {
        this.submittedBy = submittedBy;
    }

    public Long getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(Long reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getActivityTitle() {
        return activityTitle;
    }

    public void setActivityTitle(String activityTitle) {
        this.activityTitle = activityTitle;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getSubmitterName() {
        return submitterName;
    }

    public void setSubmitterName(String submitterName) {
        this.submitterName = submitterName;
    }
}
