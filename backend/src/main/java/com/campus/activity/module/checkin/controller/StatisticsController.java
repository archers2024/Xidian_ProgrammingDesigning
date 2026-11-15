package com.campus.activity.module.checkin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.activity.common.R;
import com.campus.activity.module.activity.entity.Activity;
import com.campus.activity.module.activity.entity.ActivityType;
import com.campus.activity.module.activity.mapper.ActivityMapper;
import com.campus.activity.module.activity.mapper.ActivityTypeMapper;
import com.campus.activity.module.checkin.mapper.CheckInMapper;
import com.campus.activity.module.checkin.entity.CheckIn;
import com.campus.activity.module.checkin.vo.DailyStatVO;
import com.campus.activity.module.checkin.vo.TypeStatVO;
import com.campus.activity.module.signup.entity.Registration;
import com.campus.activity.module.signup.mapper.RegistrationMapper;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * D岗：数据统计报表（FR-11，契约 /admin/statistics/overview）
 */
@RestController
@RequestMapping("/admin/statistics")

public class StatisticsController {

    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final CheckInMapper checkinMapper;
    private final ActivityTypeMapper typeMapper;

    @GetMapping("/overview")
    public R<Map<String, Object>> overview(@RequestParam(defaultValue = "30") int days) {
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusDays(days);

        Long activityCount = activityMapper.selectCount(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getDeleted, 0)
                .ne(Activity::getStatus, "CANCELED")
                .between(Activity::getCreateTime, from, to));
        Long signupCount = registrationMapper.selectCount(new LambdaQueryWrapper<Registration>()
                .between(Registration::getSignupTime, from, to));
        Long checkinCount = checkinMapper.selectCount(new LambdaQueryWrapper<CheckIn>()
                .between(CheckIn::getCheckinTime, from, to));

        double rate = signupCount == null || signupCount == 0 ? 0
                : checkinCount * 100.0 / signupCount;

        List<TypeStatVO> typeStats = checkinMapper.statByType(from, to);
        Map<String, String> typeNames = new HashMap<>();
        for (ActivityType t : typeMapper.selectList(null)) {
            typeNames.put(t.getTypeCode(), t.getTypeName());
        }
        List<Map<String, Object>> pie = new ArrayList<>();
        for (TypeStatVO v : typeStats) {
            Map<String, Object> m = new HashMap<>();
            m.put("typeName", typeNames.getOrDefault(v.getTypeCode(), v.getTypeCode()));
            m.put("signupCount", v.getSignupCount());
            m.put("checkinCount", v.getCheckinCount());
            pie.add(m);
        }

        List<DailyStatVO> daily = checkinMapper.statDaily(from, to);
        List<Map<String, Object>> line = new ArrayList<>();
        for (DailyStatVO v : daily) {
            Map<String, Object> m = new HashMap<>();
            m.put("date", v.getStatDate());
            m.put("count", v.getCnt());
            line.add(m);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("activityCount", activityCount == null ? 0 : activityCount);
        data.put("signupCount", signupCount == null ? 0 : signupCount);
        data.put("checkinCount", checkinCount == null ? 0 : checkinCount);
        data.put("checkinRate", Math.round(rate * 10) / 10.0);
        data.put("typeStats", pie);
        data.put("dailyStats", line);
        return R.ok(data);
    }

    public StatisticsController(ActivityMapper activityMapper, RegistrationMapper registrationMapper, CheckInMapper checkinMapper, ActivityTypeMapper typeMapper) {
        this.activityMapper = activityMapper;
        this.registrationMapper = registrationMapper;
        this.checkinMapper = checkinMapper;
        this.typeMapper = typeMapper;
    }
}
