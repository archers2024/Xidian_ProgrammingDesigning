package com.campus.activity.module.checkin.vo;


/**
 * 按日报名量（FR-11 折线图）
 */

public class DailyStatVO {

    private String statDate;

    private Long cnt;

    public String getStatDate() {
        return statDate;
    }

    public void setStatDate(String statDate) {
        this.statDate = statDate;
    }

    public Long getCnt() {
        return cnt;
    }

    public void setCnt(Long cnt) {
        this.cnt = cnt;
    }
}
