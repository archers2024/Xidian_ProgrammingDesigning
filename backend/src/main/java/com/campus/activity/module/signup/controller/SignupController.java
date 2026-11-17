package com.campus.activity.module.signup.controller;

import com.campus.activity.common.R;
import com.campus.activity.module.signup.entity.Registration;
import com.campus.activity.module.signup.service.SignupService;
import com.campus.activity.module.signup.vo.RegistrationVO;
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

    /** 宣传海报页数据（对外开放） */
    @GetMapping("/promo/{guid}")
    public R<Map<String, Object>> promo(@PathVariable String guid) {
        return R.ok(signupService.promo(guid));
    }

    /** 提交报名（对外开放） */
    @PostMapping("/signup")
    @com.campus.activity.common.Idempotent
    public R<Map<String, Object>> signup(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Registration r = signupService.signup(
                body.get("guid"),
                body.get("name"),
                body.get("phone"),
                body.get("collegeCode"),
                body.get("channel"),
                body.get("identityProof"));
        Map<String, Object> data = new HashMap<>();
        data.put("regId", r.getRegId());
        data.put("name", r.getName());
        data.put("phone", r.getPhone());
        data.put("signupTime", r.getSignupTime());
        return R.ok(data);
    }

    /** 报名名单（UI-05） */
    @GetMapping("/admin/activity/{id}/registrations")
    public R<List<RegistrationVO>> registrations(@PathVariable Long id,
                                                 @RequestParam(defaultValue = "true") boolean maskPhone) {
        requirePhonePermission(maskPhone);
        return R.ok(signupService.registrations(id, maskPhone));
    }

    @GetMapping("/admin/activity/{id}/registrations/page")
    public R<com.baomidou.mybatisplus.core.metadata.IPage<RegistrationVO>> page(@PathVariable Long id,@RequestParam(defaultValue="true") boolean maskPhone,@RequestParam(defaultValue="1") long current,@RequestParam(defaultValue="20") long size) {return R.ok(signupService.page(id,maskPhone,current,size));}
    /** 名单导出：手机号默认脱敏（NFR-S4） */
    @GetMapping("/admin/activity/{id}/registrations/export")
    public void export(@PathVariable Long id,
                       @RequestParam(defaultValue = "true") boolean maskPhone,
                       HttpServletResponse response) throws IOException {
        requirePhonePermission(maskPhone);
        ExcelUtil.exportStream(response,"活动报名名单_"+id,List.of("姓名","手机号","学院","报名时间","签到时间","签到方式"),sink->signupService.streamRows(id,maskPhone,sink));
    }

    public SignupController(SignupService signupService) {
        this.signupService = signupService;
    }

    private void requirePhonePermission(boolean maskPhone) {
        if (!maskPhone && !com.campus.activity.security.AdminContext.get().isSuper()) throw new com.campus.activity.common.BusinessException(40002, "只有超级管理员可查看完整手机号");
    }
}
