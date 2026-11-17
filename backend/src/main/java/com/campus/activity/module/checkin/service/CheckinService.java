package com.campus.activity.module.checkin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.activity.common.BusinessException;
import com.campus.activity.security.OrganizationAccess;
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
    private final com.campus.activity.security.IdentityVerifier identities;
    private final OrganizationAccess organizations;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Value("${campus.checkin.time-step}")
    private long timeStep;

    @Value("${campus.checkin.device-interval-seconds}")
    private long deviceIntervalSeconds;

    public Map<String,Object> publicInfo(Long id){Activity a=activityMapper.selectById(id);if(a==null||"DRAFT".equals(a.getStatus())||"CANCELED".equals(a.getStatus()))throw new BusinessException(40003,"活动不存在");organizations.requireActive(a.getOrgId());return Map.of("title",a.getTitle(),"checkinPolicy",a.getCheckinPolicy(),"timeZone","Asia/Shanghai");}
    /** ---------------- 动态签到码（UI-12 大屏刷新） ---------------- */

    /** 生成当前时间片的动态码：HMAC-SHA1(secret, activityId|timeIndex) 取 8 位十六进制 */
    public String currentCode(Long activityId) {
        Activity a = activityMapper.selectById(activityId);
        if (a == null) {
            throw new BusinessException("活动不存在");
        }
        organizations.requireActive(a.getOrgId());
        if (!Integer.valueOf(1).equals(a.getCheckinEnabled()) || LocalDateTime.now().isBefore(a.getStartTime()) || !LocalDateTime.now().isBefore(a.getEndTime())) throw new BusinessException("当前未开放签到");
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
        data.put("expiresAt",((System.currentTimeMillis()/1000/timeStep)+1)*timeStep*1000);
        data.put("checkinCount",checkinMapper.selectCount(new LambdaQueryWrapper<CheckIn>().eq(CheckIn::getActivityId,activityId)));
        return data;
    }

    /** ---------------- 签到提交（UI-11） ---------------- */

    @Transactional
    public Map<String, Object> checkin(Long activityId, String phone, String code,
                                       String lngLat, String deviceFingerprint, String identityProof) {
        if (activityId == null) throw new BusinessException("请选择活动");
        Activity a = activityMapper.selectForShare(activityId);
        if (a == null) {
            throw new BusinessException("活动不存在");
        }
        organizations.requireActive(a.getOrgId());
        if (a.getCheckinEnabled() == null || a.getCheckinEnabled() == 0) {
            throw new BusinessException("该活动未开启签到");
        }
        if (Integer.valueOf(1).equals(a.getDeleted()) || "DRAFT".equals(a.getStatus()) || "CANCELED".equals(a.getStatus())
                || LocalDateTime.now().isBefore(a.getStartTime()) || !LocalDateTime.now().isBefore(a.getEndTime())) {
            throw new BusinessException("当前不在签到时间范围内");
        }
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException("请输入正确的手机号");
        }
        jdbc.queryForList("SELECT reg_id FROM t_registration WHERE activity_id=? AND phone=? AND deleted=0 FOR UPDATE",activityId,phone);
        Registration reg = signupService.findByPhone(activityId, phone);
        if (reg == null) {
            throw new BusinessException("未查询到报名信息，请先报名");
        }
        CheckIn exist = checkinMapper.selectOne(new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getRegId, reg.getRegId()));
        if (exist != null) {
            throw new BusinessException("您已于 " + exist.getCheckinTime() + " 签到，请勿重复签到");
        }

        identities.verify(a.getCheckinPolicy(),phone,identityProof);
        if(java.util.Set.of("ACCURATE_REQUIRED","STRICT").contains(a.getCheckinPolicy())&&(code==null||code.isBlank()))throw new BusinessException("该活动必须扫描现场二维码准确签到");
        String method = "BASIC";
        // 准确签到：动态码 + 地理围栏 + 设备指纹（FR-09，加分项）
        if (code != null && !code.isBlank()) {
            if (!verifyCode(a, code)) {
                throw new BusinessException("二维码已失效，请重新扫码");
            }
            method = "ACCURATE";
            if (GeoUtil.parse(a.getLngLat()) == null || GeoUtil.parse(lngLat) == null
                    || deviceFingerprint == null || deviceFingerprint.isBlank()) throw new BusinessException("准确签到需要有效定位和设备标识");
            if (a.getLngLat() != null && !a.getLngLat().isBlank() && lngLat != null && !lngLat.isBlank()) {
                int radius = a.getCheckinRadius() == null ? 500 : a.getCheckinRadius();
                double distance = GeoUtil.distance(a.getLngLat(), lngLat);
                if (distance > radius) {
                    throw new BusinessException("超出签到范围（当前距离约 " + (long) distance + " 米），请到会场签到");
                }
            }
            if (deviceFingerprint != null && !deviceFingerprint.isBlank()) {
                jdbc.update("INSERT INTO t_checkin_device(activity_id,fingerprint) VALUES (?,?) ON DUPLICATE KEY UPDATE fingerprint=VALUES(fingerprint)",activityId,deviceFingerprint);
                var last=jdbc.queryForObject("SELECT last_checkin FROM t_checkin_device WHERE activity_id=? AND fingerprint=? FOR UPDATE",java.sql.Timestamp.class,activityId,deviceFingerprint);
                if(last!=null&&last.toLocalDateTime().isAfter(LocalDateTime.now().minusSeconds(deviceIntervalSeconds)))throw new BusinessException("该设备刚刚已完成签到，请稍后再试");
                jdbc.update("UPDATE t_checkin_device SET last_checkin=NOW() WHERE activity_id=? AND fingerprint=?",activityId,deviceFingerprint);
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
        Activity activity=activityMapper.selectById(activityId);
        if(activity==null)throw new BusinessException(40003,"活动不存在");
        organizations.requireScope(activity.getOrgId());
        return new ArrayList<>(checkinMapper.listByActivity(activityId).stream().map(v -> {
            Map<String, Object> m = new HashMap<>();
            m.put("checkinId", v.getCheckinId());
            m.put("regId", v.getRegId());
            m.put("name", v.getName());
            m.put("phone", SignupService.mask(v.getPhone()));
            m.put("collegeCode", v.getCollegeCode());
            m.put("checkinTime", v.getCheckinTime());
            m.put("method", v.getMethod());
            return m;
        }).toList());
    }

    public java.util.Map<String,Object> summary(Long id) {
        Activity a=activityMapper.selectById(id);if(a==null)throw new BusinessException(40003,"活动不存在");organizations.requireScope(a.getOrgId());
        return java.util.Map.of("signedCount",a.getSignedCount(),"checkinCount",checkinMapper.selectCount(new LambdaQueryWrapper<CheckIn>().eq(CheckIn::getActivityId,id)));
    }
    public com.baomidou.mybatisplus.core.metadata.IPage<com.campus.activity.module.checkin.vo.CheckinVO> page(Long id,long current,long size) {
        summary(id);if(current<1||size<1||size>100)throw new BusinessException("分页范围不正确");
        var result=checkinMapper.pageByActivity(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current,size),id);result.getRecords().forEach(v->v.setPhone(SignupService.mask(v.getPhone())));return result;
    }
    public void streamRows(Long id,java.util.function.Consumer<List<String>> sink) {
        summary(id);checkinMapper.stream(id,ctx->{var v=ctx.getResultObject();sink.accept(java.util.Arrays.asList(v.getName(),SignupService.mask(v.getPhone()),v.getCollegeCode(),String.valueOf(v.getCheckinTime()),v.getMethod()));});
    }
    /** 供 C 岗名单页读取签到状态（只读，不修改 C 岗页面代码） */
    public Map<Long, Map<String, Object>> statusByActivity(Long activityId) {
        Map<Long, Map<String, Object>> map = new HashMap<>();
        for (Map<String, Object> row : listByActivity(activityId)) {
            Long regId = Long.valueOf(String.valueOf(row.get("regId")));
            map.put(regId, row);
        }
        return map;
    }

    public String fingerprint(String raw) {
        if (raw == null || raw.isBlank() || raw.length() > 256) {
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

    public CheckinService(CheckInMapper checkinMapper, ActivityMapper activityMapper, SignupService signupService, OrganizationAccess organizations, org.springframework.jdbc.core.JdbcTemplate jdbc, com.campus.activity.security.IdentityVerifier identities) {
        this.checkinMapper = checkinMapper;
        this.activityMapper = activityMapper;
        this.signupService = signupService;
        this.organizations=organizations; this.jdbc=jdbc; this.identities=identities;
    }
}
