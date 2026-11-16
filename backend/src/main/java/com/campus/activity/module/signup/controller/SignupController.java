package com.campus.activity.module.signup.controller;

import com.campus.activity.common.BusinessException;
import com.campus.activity.common.R;
import com.campus.activity.module.signup.entity.Registration;
import com.campus.activity.module.signup.service.SignupService;
import com.campus.activity.module.signup.vo.RegistrationVO;
import com.campus.activity.module.system.service.AdminService;
import com.campus.activity.security.AdminContext;
import com.campus.activity.util.ExcelUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * C岗：报名接口（契约 /promo/{guid}、/signup、/admin/activity/{id}/registrations）
 */
@RestController

public class SignupController {

    private final SignupService signupService;
    private final AdminService adminService;

    /** 宣传海报页数据（对外开放） */
    @GetMapping("/promo/{guid}")
    public R<Map<String, Object>> promo(@PathVariable String guid) {
        return R.ok(signupService.promo(guid));
    }

    /** 提交报名（对外开放） */
    @PostMapping("/signup")
    public R<Map<String, Object>> signup(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Registration r = signupService.signup(
                body.get("guid"),
                body.get("name"),
                body.get("phone"),
                body.get("collegeCode"),
                body.get("channel"),
                request.getRemoteAddr());
        Map<String, Object> data = new HashMap<>();
        data.put("regId", r.getRegId());
        data.put("name", r.getName());
        data.put("phone", r.getPhone());
        data.put("signupTime", r.getSignupTime());
        return R.ok(data);
    }

    /** 报名名单（UI-05）：默认脱敏；查看完整号码仅限超级管理员，并写入操作日志。 */
    @GetMapping("/admin/activity/{id}/registrations")
    public R<List<RegistrationVO>> registrations(@PathVariable Long id,
                                                 @RequestParam(defaultValue = "true") boolean maskPhone) {
        requireFullPhonePrivilege(id, maskPhone, "查看报名名单完整手机号");
        return R.ok(signupService.registrations(id, maskPhone));
    }

    /** 名单导出：手机号默认脱敏（NFR-S4） */
    @GetMapping("/admin/activity/{id}/registrations/export")
    public void export(@PathVariable Long id,
                       @RequestParam(defaultValue = "true") boolean maskPhone,
                       HttpServletResponse response) throws IOException {
        requireFullPhonePrivilege(id, maskPhone, "导出报名名单完整手机号");
        List<List<String>> rows = signupService.exportRows(id, maskPhone);
        ExcelUtil.export(response, "活动报名名单_" + id,
                List.of("姓名", "手机号", "学院", "报名时间", "签到时间", "签到方式"), rows);
    }

    /**
     * 完整手机号不是客户端翻个开关就能看：必须超级管理员，且每次查看/导出都留痕。
     */
    private void requireFullPhonePrivilege(Long activityId, boolean maskPhone, String action) {
        if (maskPhone) {
            return;
        }
        if (AdminContext.get() == null || !AdminContext.get().isSuper()) {
            throw new BusinessException(40002, "仅超级管理员可查看完整手机号");
        }
        adminService.writeLog(AdminContext.adminId(), "PRIVACY_PHONE_VIEW",
                String.valueOf(activityId), action, null);
    }

    public SignupController(SignupService signupService, AdminService adminService) {
        this.signupService = signupService;
        this.adminService = adminService;
    }
}
