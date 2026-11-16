package com.campus.activity.module.checkin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.activity.common.BusinessException;
import com.campus.activity.module.activity.entity.Activity;
import com.campus.activity.module.activity.mapper.ActivityMapper;
import com.campus.activity.module.checkin.entity.CheckIn;
import com.campus.activity.module.checkin.mapper.CheckInMapper;
import com.campus.activity.module.signup.entity.Registration;
import com.campus.activity.module.signup.service.SignupService;
import com.campus.activity.util.GeoUtil;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * D岗：签到域（FR-08 基本签到、FR-09 准确签到）。
 * 防作弊三要素：30 秒刷新动态码（TOTP）+ 地理围栏 + 设备指纹限频。
 */
@Service

public class CheckinService {

    private final CheckInMapper checkinMapper;
    private final ActivityMapper activityMapper;
    private final SignupService signupService;

    @Value("${campus.checkin.time-step}")
    private long timeStep;

    @Value("${campus.checkin.device-interval-seconds}")
    private long deviceIntervalSeconds;

    /** ---------------- 动态签到码（UI-12 大屏刷新） ---------------- */

    /** 生成当前时间片的动态码：HMAC-SHA1(secret, activityId|timeIndex) 取 8 位十六进制 */
    public String currentCode(Long activityId) {
        Activity a = activityMapper.selectById(activityId);
        if (a == null) {
            throw new BusinessException("活动不存在");
        }
        return codeOf(a, timeIndex());
    }

    /** 校验动态码，容忍 1 个时间片的时钟偏差（30 秒） */
    public boolean verifyCode(Activity a, String code) {
        long idx = timeIndex();
        return codeOf(a, idx).equalsIgnoreCase(code) || codeOf(a, idx - 1).equalsIgnoreCase(code);
    }

    private long timeIndex() {
        return System.currentTimeMillis() / 1000 / timeStep;
    }

    private String codeOf(Activity a, long idx) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(a.getCheckinSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] raw = mac.doFinal((a.getActivityId() + "|" + idx).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                sb.append(String.format("%02x", raw[i] & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new BusinessException("动态码生成失败");
        }
    }

    /** 前端大屏用：返回动态码与剩余秒数 */
    public Map<String, Object> codeBoard(Long activityId) {
        Map<String, Object> data = new HashMap<>();
        data.put("code", currentCode(activityId));
        long elapsed = (System.currentTimeMillis() / 1000) % timeStep;
        data.put("remainSeconds", timeStep - elapsed);
        data.put("timeStep", timeStep);
        return data;
    }

    /** ---------------- 签到提交（UI-11） ---------------- */

    @Transactional
    public Map<String, Object> checkin(Long activityId, String phone, String code,
                                       String lngLat, String deviceFingerprint) {
        Activity a = activityMapper.selectById(activityId);
        if (a == null) {
            throw new BusinessException("活动不存在");
        }
        if (a.getCheckinEnabled() == null || a.getCheckinEnabled() == 0) {
            throw new BusinessException("该活动未开启签到");
        }
        if (!"ONGOING".equals(a.getStatus()) && !"CLOSED".equals(a.getStatus())) {
            throw new BusinessException("当前不在签到时间范围内");
        }
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException("请输入正确的手机号");
        }
        Registration reg = signupService.findByPhone(activityId, phone);
        if (reg == null) {
            throw new BusinessException("未查询到报名信息，请先报名");
        }
        CheckIn exist = checkinMapper.selectOne(new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getRegId, reg.getRegId()));
        if (exist != null) {
            throw new BusinessException("您已于 " + exist.getCheckinTime() + " 签到，请勿重复签到");
        }

        String method = "BASIC";
        // 准确签到：动态码 + 会场坐标 + 用户坐标 + 设备标识，四项缺一不可（FR-09）
        if (code != null && !code.isBlank()) {
            if (!verifyCode(a, code)) {
                throw new BusinessException("二维码已失效，请重新扫码");
            }
            method = "ACCURATE";
            double[] center = GeoUtil.parse(a.getLngLat());
            if (center == null) {
                throw new BusinessException("活动未配置有效的会场坐标，暂时无法完成准确签到");
            }
            double[] userPoint = GeoUtil.parse(lngLat);
            if (userPoint == null) {
                throw new BusinessException("准确签到需要获取定位，请允许定位权限后重试");
            }
            if (deviceFingerprint == null || deviceFingerprint.isBlank()) {
                throw new BusinessException("准确签到需要设备标识，请使用手机浏览器打开");
            }
            int radius = a.getCheckinRadius() == null ? 500 : a.getCheckinRadius();
            double distance = GeoUtil.distance(a.getLngLat(), lngLat);
            if (!Double.isFinite(distance) || distance > radius) {
                throw new BusinessException("超出签到范围（当前距离约 " + (long) distance + " 米），请到会场签到");
            }
            LocalDateTime since = LocalDateTime.now().minusSeconds(deviceIntervalSeconds);
            Long recent = checkinMapper.selectCount(new LambdaQueryWrapper<CheckIn>()
                    .eq(CheckIn::getActivityId, activityId)
                    .eq(CheckIn::getDeviceFingerprint, deviceFingerprint)
                    .ge(CheckIn::getCheckinTime, since));
            if (recent != null && recent > 0) {
                throw new BusinessException("该设备刚刚已完成签到，请稍后再试");
            }
        }

        CheckIn c = new CheckIn();
        c.setRegId(reg.getRegId());
        c.setActivityId(activityId);
        c.setCheckinTime(LocalDateTime.now());
        c.setMethod(method);
        c.setLngLat(lngLat);
        c.setDeviceFingerprint(deviceFingerprint);
        checkinMapper.insert(c);

        Map<String, Object> data = new HashMap<>();
        data.put("checkinId", c.getCheckinId());
        data.put("name", reg.getName());
        data.put("activityTitle", a.getTitle());
        data.put("checkinTime", c.getCheckinTime());
        data.put("method", method);
        return data;
    }

    /** ---------------- 记录与统计 ---------------- */

    public List<Map<String, Object>> listByActivity(Long activityId) {
        return new ArrayList<>(checkinMapper.listByActivity(activityId).stream().map(v -> {
            Map<String, Object> m = new HashMap<>();
            m.put("checkinId", v.getCheckinId());
            m.put("name", v.getName());
            // 签到记录与导出统一脱敏，避免绕开报名名单的隐私口径
            m.put("phone", SignupService.mask(v.getPhone()));
            m.put("collegeCode", v.getCollegeCode());
            m.put("checkinTime", v.getCheckinTime());
            m.put("method", v.getMethod());
            return m;
        }).toList());
    }

    /** 供 C 岗名单页读取签到状态（只读，不修改 C 岗页面代码） */
    public Map<Long, Map<String, Object>> statusByActivity(Long activityId) {
        Map<Long, Map<String, Object>> map = new HashMap<>();
        for (Map<String, Object> row : listByActivity(activityId)) {
            Long regId = Long.valueOf(String.valueOf(row.get("checkinId")));
            map.put(regId, row);
        }
        return map;
    }

    public String fingerprint(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", bytes[i] & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public CheckinService(CheckInMapper checkinMapper, ActivityMapper activityMapper, SignupService signupService) {
        this.checkinMapper = checkinMapper;
        this.activityMapper = activityMapper;
        this.signupService = signupService;
    }
}
