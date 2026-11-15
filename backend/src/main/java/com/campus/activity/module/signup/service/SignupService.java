package com.campus.activity.module.signup.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.activity.common.BusinessException;
import com.campus.activity.module.activity.entity.Activity;
import com.campus.activity.module.activity.entity.Attachment;
import com.campus.activity.module.activity.mapper.ActivityMapper;
import com.campus.activity.module.activity.service.ActivityService;
import com.campus.activity.module.signup.entity.Registration;
import com.campus.activity.module.signup.mapper.CollegeMapper;
import com.campus.activity.module.signup.mapper.RegistrationMapper;
import com.campus.activity.module.signup.vo.RegistrationVO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * C岗：报名域（FR-06、FR-07、FR-10）。
 * 并发要点：名额判定在事务内 select for update 锁定活动行，杜绝超卖（NFR-R2 / 风险 R3）。
 */
@Service

public class SignupService {

    private final ActivityService activityService;
    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final CollegeMapper collegeMapper;

    @Value("${campus.signup.ip-limit-per-minute}")
    private int ipLimitPerMinute;

    @Value("${campus.mobile.base-url}")
    private String mobileBaseUrl;

    /** 简易 IP 限流（演示用内存实现，生产建议 Redis） */
    private final Map<String, long[]> ipWindow = new ConcurrentHashMap<>();

    /** ---------------- 宣传海报页（UI-08） ---------------- */

    public Map<String, Object> promo(String guid) {
        Activity a = activityService.byGuid(guid);
        Map<String, Object> data = new HashMap<>();
        data.put("activity", a);
        data.put("attachments", activityService.attachments(a.getActivityId()));
        // 时效性：仅报名时段内可报名，超时给前端友好提示（FR-04 可选项）
        boolean signupOpen = "OPEN".equals(a.getStatus());
        data.put("signupOpen", signupOpen);
        data.put("signupTip", signupOpen ? "" : tipOf(a.getStatus()));
        data.put("signupUrl", mobileBaseUrl + "/signup/" + guid);
        return data;
    }

    private String tipOf(String status) {
        return switch (status) {
            case "DRAFT" -> "活动尚未发布";
            case "CLOSED" -> "报名已截止，欢迎关注后续活动";
            case "ONGOING" -> "活动已开始，报名通道已关闭";
            case "FINISHED" -> "活动已结束";
            case "CANCELED" -> "活动已取消";
            default -> "当前不可报名";
        };
    }

    /** ---------------- 报名（FR-06 / FR-07） ---------------- */

    @Transactional
    public Registration signup(String guid, String name, String phone, String collegeCode,
                               String channel, String ip) {
        checkIpLimit(ip);
        Activity a = activityService.byGuid(guid);

        // 1）活动是否失效
        if (!"OPEN".equals(a.getStatus())) {
            throw new BusinessException(tipOf(a.getStatus()));
        }
        // 2）报名时间（状态机已保证 OPEN 时未截止，这里再兜底一次）
        if (LocalDateTime.now().isAfter(a.getSignupDeadline())) {
            throw new BusinessException("报名已于 " + a.getSignupDeadline() + " 截止");
        }
        // 3）个人信息格式
        if (name == null || name.trim().length() < 2 || name.trim().length() > 20) {
            throw new BusinessException("请输入 2-20 个字符的姓名");
        }
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException("请输入正确的手机号");
        }
        if (collegeCode == null || collegeMapper.selectById(collegeCode) == null) {
            throw new BusinessException("请选择正确的学院");
        }
        // 4）重复报名（唯一索引 uk_activity_phone 兜底）
        Long exists = registrationMapper.selectCount(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, a.getActivityId())
                .eq(Registration::getPhone, phone)
                .eq(Registration::getStatus, "VALID"));
        if (exists != null && exists > 0) {
            throw new BusinessException("该手机号已报名，请勿重复提交");
        }
        // 5）名额：事务内锁行判定，防超卖
        Activity locked = activityMapper.selectForUpdate(a.getActivityId());
        if (locked.getSignedCount() >= locked.getQuota()) {
            throw new BusinessException("活动名额已满，报名失败");
        }

        Registration r = new Registration();
        r.setActivityId(a.getActivityId());
        r.setName(name.trim());
        r.setPhone(phone);
        r.setCollegeCode(collegeCode);
        r.setSourceChannel(channel == null ? "LINK" : channel);
        r.setSignupTime(LocalDateTime.now());
        r.setStatus("VALID");
        registrationMapper.insert(r);
        activityMapper.incrSignedCount(a.getActivityId(), 1);
        return r;
    }

    private void checkIpLimit(String ip) {
        if (ip == null) {
            return;
        }
        long minute = System.currentTimeMillis() / 60000;
        long[] w = ipWindow.get(ip);
        if (w == null || w[0] != minute) {
            ipWindow.put(ip, new long[]{minute, 1});
            return;
        }
        w[1]++;
        if (w[1] > ipLimitPerMinute) {
            throw new BusinessException("操作过于频繁，请稍后再试");
        }
    }

    /** ---------------- 名单管理（FR-10） ---------------- */

    public List<RegistrationVO> registrations(Long activityId, boolean maskPhone) {
        List<RegistrationVO> list = registrationMapper.listWithCheckin(activityId);
        if (maskPhone) {
            list.forEach(v -> v.setPhoneMasked(mask(v.getPhone())));
        } else {
            list.forEach(v -> v.setPhoneMasked(v.getPhone()));
        }
        return list;
    }

    /** 供 D 岗签到校验：按活动 + 手机号查报名记录 */
    public Registration findByPhone(Long activityId, String phone) {
        return registrationMapper.selectOne(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, activityId)
                .eq(Registration::getPhone, phone)
                .eq(Registration::getStatus, "VALID"));
    }

    public List<List<String>> exportRows(Long activityId, boolean maskPhone) {
        List<RegistrationVO> list = registrations(activityId, maskPhone);
        List<List<String>> rows = new ArrayList<>();
        for (RegistrationVO v : list) {
            List<String> row = new ArrayList<>();
            row.add(v.getName());
            row.add(v.getPhoneMasked());
            row.add(v.getCollegeName());
            row.add(String.valueOf(v.getSignupTime()));
            row.add(v.getCheckinTime() == null ? "未签到" : String.valueOf(v.getCheckinTime()));
            row.add(v.getCheckinMethod() == null ? "-" : v.getCheckinMethod());
            rows.add(row);
        }
        return rows;
    }

    public static String mask(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    public List<Attachment> attachments(Long activityId) {
        return activityService.attachments(activityId);
    }

    public SignupService(ActivityService activityService, ActivityMapper activityMapper, RegistrationMapper registrationMapper, CollegeMapper collegeMapper) {
        this.activityService = activityService;
        this.activityMapper = activityMapper;
        this.registrationMapper = registrationMapper;
        this.collegeMapper = collegeMapper;
    }
}
