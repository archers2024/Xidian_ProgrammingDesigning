package com.campus.activity.module.checkin.vo;


import java.time.LocalDateTime;

/**
 * 签到记录视图对象（UI-06）
 */

public class CheckinVO {

    private Long checkinId;

    private Long regId;

    private String name;

    private String phone;

    private String collegeCode;

    private LocalDateTime checkinTime;

    private String method;

    public Long getCheckinId() {
        return checkinId;
    }

    public void setCheckinId(Long checkinId) {
        this.checkinId = checkinId;
    }

    public Long getRegId() {
        return regId;
    }

    public void setRegId(Long regId) {
        this.regId = regId;
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

    public LocalDateTime getCheckinTime() {
        return checkinTime;
    }

    public void setCheckinTime(LocalDateTime checkinTime) {
        this.checkinTime = checkinTime;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}
