package com.campus.activity.module.checkin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;


import java.time.LocalDateTime;

/**
 * 签到记录 t_checkin（Owner：D岗）
 * method：BASIC 基本签到 / ACCURATE 准确签到（动态码 + 定位）
 */

@TableName("t_checkin")
public class CheckIn {

    @TableId(type = IdType.AUTO)
    private Long checkinId;

    private Long regId;

    private Long activityId;

    private LocalDateTime checkinTime;

    private String method;

    private String lngLat;

    private String deviceFingerprint;

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

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
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

    public String getLngLat() {
        return lngLat;
    }

    public void setLngLat(String lngLat) {
        this.lngLat = lngLat;
    }

    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }

    public void setDeviceFingerprint(String deviceFingerprint) {
        this.deviceFingerprint = deviceFingerprint;
    }
}
