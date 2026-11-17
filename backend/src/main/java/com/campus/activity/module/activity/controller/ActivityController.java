package com.campus.activity.module.activity.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.activity.common.R;
import com.campus.activity.module.activity.entity.Activity;
import com.campus.activity.module.activity.dto.ActivitySaveRequest;
import com.campus.activity.module.activity.dto.ActivityRequestMapper;
import jakarta.validation.Valid;
import com.campus.activity.module.activity.service.ActivityService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * B岗：活动接口（契约 /activity/**）
 * 说明：/admin/activity/** 需登录；/activity/{guid}/promo 等对外开放，见 C 岗控制器。
 */
@RestController
@RequestMapping("/admin/activity")

public class ActivityController {

    private final ActivityService activityService;

    @PostMapping
    @com.campus.activity.common.Idempotent
    public R<Map<String, Object>> create(@Valid @RequestBody ActivitySaveRequest request) {
        Activity a = ActivityRequestMapper.activity(request);
        Long id = activityService.create(a, ActivityRequestMapper.attachments(request));
        return R.ok(Map.of("activityId", id, "signupGuid", a.getSignupGuid()));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ActivitySaveRequest request) {
        Activity a = ActivityRequestMapper.activity(request);
        a.setActivityId(id);
        activityService.update(a, ActivityRequestMapper.attachments(request));
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        activityService.remove(id);
        return R.ok();
    }

    @PostMapping("/{id}/publish")
    public R<Void> publish(@PathVariable Long id) {
        activityService.publish(id);
        return R.ok();
    }

    @PutMapping("/{id}/checkin-enabled")
    public R<Void> checkinEnabled(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        activityService.setCheckinEnabled(id, body.get("enabled"));
        return R.ok();
    }

    @GetMapping("/list")
    public R<IPage<Activity>> list(@RequestParam(defaultValue = "1") long current,
                                   @RequestParam(defaultValue = "10") long size,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) String typeCode,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime from,
                                   @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime to) {
        return R.ok(activityService.page(current, size, keyword, typeCode, status, from, to));
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        Activity a = activityService.detail(id);
        Map<String, Object> data = new HashMap<>();
        data.put("activity", a);
        data.put("attachments", activityService.attachments(id));
        return R.ok(data);
    }

    /** 报名二维码：B 岗提供数据，二维码渲染由前端统一组件完成 */
    @GetMapping("/{id}/qrcode")
    public R<Map<String, String>> qrcode(@PathVariable Long id) {
        Activity a = activityService.detail(id);
        Map<String, String> data = new HashMap<>();
        data.put("guid", a.getSignupGuid());
        data.put("title", a.getTitle());
        return R.ok(data);
    }

    /** 供 D 岗读取签到配置（地点坐标、半径、开关） */
    @GetMapping("/{id}/checkin-config")
    public R<Activity> checkinConfig(@PathVariable Long id) {
        return R.ok(activityService.checkinConfig(id));
    }

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }
}
