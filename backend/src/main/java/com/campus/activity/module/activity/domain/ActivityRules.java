package com.campus.activity.module.activity.domain;

import com.campus.activity.common.BusinessException;
import com.campus.activity.module.activity.entity.Activity;
import com.campus.activity.util.GeoUtil;
import com.campus.activity.util.InputRules;

import java.util.List;

/** Activity content and time invariants shared by create and update. */
public final class ActivityRules {
    private ActivityRules() { }

    public static void validate(Activity a) {
        if(!List.of("BASIC","ACCURATE_REQUIRED","IDENTITY_REQUIRED","STRICT").contains(a.getCheckinPolicy()))throw new BusinessException("签到策略不正确");
        InputRules.publicUrl(a.getLiveUrl(), "直播链接");
        InputRules.publicUrl(a.getCoverUrl(), "封面地址");
        InputRules.text(a.getIntroduction(), 10000, false, "活动简介");
        InputRules.text(a.getLngLat(), 50, false, "地点坐标");
        if (a.getTypeCode() == null || !List.of("LECTURE", "SEMINAR", "CAREER", "TRAINING", "OTHER").contains(a.getTypeCode())) throw new BusinessException("请选择正确的活动类型");
        if (a.getLocation() == null || a.getLocation().isBlank() || a.getLocation().length() > 200) throw new BusinessException("请填写有效的活动地点");
        if (a.getTitle() != null && a.getTitle().length() > 200) throw new BusinessException("活动名称不能超过 200 字");
        if (a.getCheckinRadius() != null && (a.getCheckinRadius() < 50 || a.getCheckinRadius() > 5000)) throw new BusinessException("签到半径需在 50 至 5000 米之间");
        if (a.getLngLat() != null && !a.getLngLat().isBlank() && GeoUtil.parse(a.getLngLat()) == null) throw new BusinessException("地点坐标格式不正确");
        if (a.getTitle() == null || a.getTitle().isBlank()) {
            throw new BusinessException("请填写活动名称");
        }
        if (a.getStartTime() == null || a.getEndTime() == null || a.getSignupDeadline() == null) {
            throw new BusinessException("请填写完整的时间信息");
        }
        if (!a.getSignupDeadline().isBefore(a.getStartTime())) {
            throw new BusinessException("报名截止时间必须早于活动开始时间");
        }
        if (!a.getStartTime().isBefore(a.getEndTime())) {
            throw new BusinessException("活动开始时间必须早于结束时间");
        }
        if (a.getQuota() == null || a.getQuota() < 1 || a.getQuota() > 5000) {
            throw new BusinessException("人数上限需在 1 ~ 5000 之间");
        }
    }

}
