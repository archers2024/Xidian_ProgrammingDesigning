package com.campus.activity.module.system.controller;

import com.campus.activity.common.R;
import com.campus.activity.module.system.service.AdminService;
import com.campus.activity.security.AdminContext;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * A岗：认证接口（契约 /auth/login、/auth/logout、/auth/current）
 */
@RestController

public class AuthController {

    private final AdminService adminService;

    @PostMapping("/auth/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) {
            return R.business("请输入账号和密码");
        }
        return R.ok(adminService.login(username, password, request.getRemoteAddr()));
    }

    @PostMapping("/auth/logout")
    public R<Void> logout() {
        AdminContext.clear();
        return R.ok();
    }

    @GetMapping("/admin/current")
    public R<Map<String, Object>> current() {
        AdminContext.CurrentAdmin a = AdminContext.get();
        Map<String, Object> data = new HashMap<>();
        data.put("adminId", a.getAdminId());
        data.put("username", a.getUsername());
        data.put("roleCode", a.getRoleCode());
        return R.ok(data);
    }

    public AuthController(AdminService adminService) {
        this.adminService = adminService;
    }
}
