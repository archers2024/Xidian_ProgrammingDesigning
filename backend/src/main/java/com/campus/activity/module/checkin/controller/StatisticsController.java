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

    /**
     * 报表口径：以「时间窗口内创建、未删除、未取消」的有效活动为唯一数据集。
     * 总量、分类、签到率都基于同一批活动，趋势在活动集合内按报名时间展开，
     * 避免出现「活动数 0、报名人次却大于 0」这类自相矛盾的结果。
     */
    @GetMapping("/overview")
    public R<Map<String, Object>> overview(@RequestParam(defaultValue = "30") int days) {
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusDays(days);

        List<Activity> validActivities = activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getDeleted, 0)
                .ne(Activity::getStatus, "CANCELED")
                .between(Activity::getCreateTime, from, to));
        List<Long> activityIds = validActivities.stream().map(Activity::getActivityId).toList();

        long activityCount = activityIds.size();
        long signupCount = 0;
        long checkinCount = 0;
        List<TypeStatVO> typeStats = List.of();
        List<DailyStatVO> daily = List.of();

        if (!activityIds.isEmpty()) {
            signupCount = registrationMapper.selectCount(new LambdaQueryWrapper<Registration>()
                    .in(Registration::getActivityId, activityIds));
            checkinCount = checkinMapper.selectCount(new LambdaQueryWrapper<CheckIn>()
                    .in(CheckIn::getActivityId, activityIds));
            typeStats = checkinMapper.statByType(activityIds);
            daily = checkinMapper.statDaily(activityIds, from, to);
        }

        double rate = signupCount == 0 ? 0 : checkinCount * 100.0 / signupCount;

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

        List<Map<String, Object>> line = new ArrayList<>();
        for (DailyStatVO v : daily) {
            Map<String, Object> m = new HashMap<>();
            m.put("date", v.getStatDate());
            m.put("count", v.getCnt());
            line.add(m);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("activityCount", activityCount);
        data.put("signupCount", signupCount);
        data.put("checkinCount", checkinCount);
        data.put("checkinRate", Math.round(rate * 10) / 10.0);
        data.put("typeStats", pie);
        data.put("dailyStats", line);
        // 明确口径说明，便于前端与验收对账
        data.put("scope", Map.of(
                "activityFilter", "未删除且未取消的活动",
                "timeField", "活动创建时间",
                "days", days));
        return R.ok(data);
    }

    public StatisticsController(ActivityMapper activityMapper, RegistrationMapper registrationMapper, CheckInMapper checkinMapper, ActivityTypeMapper typeMapper) {
        this.activityMapper = activityMapper;
        this.registrationMapper = registrationMapper;
        this.checkinMapper = checkinMapper;
        this.typeMapper = typeMapper;
    }
}
