package com.campus.activity.module.signup.vo;


import java.time.LocalDateTime;

/**
 * 报名名单视图对象（含签到信息，签到字段来自 D 岗只读数据）。
 */

public class RegistrationVO {

    private Long regId;

    private String name;

    private String phone;

    private String collegeName;

    private LocalDateTime signupTime;

    private LocalDateTime checkinTime;

    private String checkinMethod;

    /** 已脱敏手机号，如 138****1234（NFR-S4） */
    private String phoneMasked;

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

    public String getCollegeName() {
        return collegeName;
    }

    public void setCollegeName(String collegeName) {
        this.collegeName = collegeName;
    }

    public LocalDateTime getSignupTime() {
        return signupTime;
    }

    public void setSignupTime(LocalDateTime signupTime) {
        this.signupTime = signupTime;
    }

    public LocalDateTime getCheckinTime() {
        return checkinTime;
    }

    public void setCheckinTime(LocalDateTime checkinTime) {
        this.checkinTime = checkinTime;
    }

    public String getCheckinMethod() {
        return checkinMethod;
    }

    public void setCheckinMethod(String checkinMethod) {
        this.checkinMethod = checkinMethod;
    }

    public String getPhoneMasked() {
        return phoneMasked;
    }

    public void setPhoneMasked(String phoneMasked) {
        this.phoneMasked = phoneMasked;
    }
}
