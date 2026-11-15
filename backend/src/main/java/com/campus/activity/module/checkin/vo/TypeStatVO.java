package com.campus.activity.module.checkin.vo;


/**
 * 按活动类型统计（FR-11 柱状图）
 */

public class TypeStatVO {

    private String typeCode;

    private Long signupCount;

    private Long checkinCount;

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public Long getSignupCount() {
        return signupCount;
    }

    public void setSignupCount(Long signupCount) {
        this.signupCount = signupCount;
    }

    public Long getCheckinCount() {
        return checkinCount;
    }

    public void setCheckinCount(Long checkinCount) {
        this.checkinCount = checkinCount;
    }
}
