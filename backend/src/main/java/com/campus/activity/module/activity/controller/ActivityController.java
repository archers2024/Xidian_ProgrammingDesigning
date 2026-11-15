package com.campus.activity.module.activity.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.activity.common.R;
import com.campus.activity.module.activity.entity.Activity;
import com.campus.activity.module.activity.entity.Attachment;
import com.campus.activity.module.activity.service.ActivityService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
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
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        Activity a = toEntity(body);
        Long id = activityService.create(a, toAttachments(body));
        return R.ok(Map.of("activityId", id, "signupGuid", a.getSignupGuid()));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Activity a = toEntity(body);
        a.setActivityId(id);
        activityService.update(a, toAttachments(body));
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

    @SuppressWarnings("unchecked")
    private List<Attachment> toAttachments(Map<String, Object> body) {
        List<Map<String, Object>> list = (List<Map<String, Object>>) body.get("attachments");
        if (list == null) {
            return List.of();
        }
        return list.stream().map(m -> {
            Attachment att = new Attachment();
            att.setFileName((String) m.get("fileName"));
            att.setFileUrl((String) m.get("url"));
            Object size = m.get("fileSize");
            att.setFileSize(size == null ? null : Long.valueOf(String.valueOf(size)));
            return att;
        }).toList();
    }

    private Activity toEntity(Map<String, Object> body) {
        Activity a = new Activity();
        a.setTitle((String) body.get("title"));
        a.setTypeCode((String) body.get("typeCode"));
        a.setIntroduction((String) body.get("introduction"));
        a.setLocation((String) body.get("location"));
        a.setLngLat((String) body.get("lngLat"));
        a.setLiveUrl((String) body.get("liveUrl"));
        a.setCoverUrl((String) body.get("coverUrl"));
        a.setStartTime(parseTime(body.get("startTime")));
        a.setEndTime(parseTime(body.get("endTime")));
        a.setSignupDeadline(parseTime(body.get("signupDeadline")));
        Object quota = body.get("quota");
        a.setQuota(quota == null ? null : Integer.valueOf(String.valueOf(quota)));
        Object radius = body.get("checkinRadius");
        a.setCheckinRadius(radius == null ? 500 : Integer.valueOf(String.valueOf(radius)));
        return a;
    }

    /** 前端 datetime-local 与后端统一使用 yyyy-MM-dd HH:mm:ss，同时兼容 ISO 的 T 分隔格式。 */
    private static final DateTimeFormatter SPACE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private LocalDateTime parseTime(Object v) {
        if (v == null) {
            return null;
        }
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) {
            return null;
        }
        if (s.length() == 16) {
            s = s + ":00";                  // yyyy-MM-ddTHH:mm / yyyy-MM-dd HH:mm
        }
        if (s.length() == 10) {
            s = s + " 00:00:00";           // 纯日期
        }
        if (s.indexOf(' ') > 0) {
            return LocalDateTime.parse(s.replace('T', ' '), SPACE_FORMATTER);
        }
        return LocalDateTime.parse(s);      // yyyy-MM-ddTHH:mm:ss
    }

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }
}
