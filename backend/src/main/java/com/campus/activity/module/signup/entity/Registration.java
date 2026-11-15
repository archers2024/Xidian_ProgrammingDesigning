package com.campus.activity.module.signup.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;


import java.time.LocalDateTime;

/**
 * 报名记录 t_registration（Owner：C岗）
 */

@TableName("t_registration")
public class Registration {

    @TableId(type = IdType.AUTO)
    private Long regId;

    private Long activityId;

    private String name;

    private String phone;

    private String collegeCode;

    /** SCAN 扫码 / LINK 链接 */
    private String sourceChannel;

    private LocalDateTime signupTime;

    /** VALID / CANCELED */
    private String status;

    @TableLogic
    private Integer deleted;

    public Long getRegId() {
        return regId;
    }

    public void setRegId(Long regId) {
        this.regId = regId;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCollegeCode() {
        return collegeCode;
    }

    public void setCollegeCode(String collegeCode) {
        this.collegeCode = collegeCode;
    }

    public String getSourceChannel() {
        return sourceChannel;
    }

    public void setSourceChannel(String sourceChannel) {
        this.sourceChannel = sourceChannel;
    }

    public LocalDateTime getSignupTime() {
        return signupTime;
    }

    public void setSignupTime(LocalDateTime signupTime) {
        this.signupTime = signupTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
