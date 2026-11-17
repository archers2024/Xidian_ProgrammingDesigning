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
    public R<Map<String, Object>> login(@RequestBody Map<String, String> body, HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) {
            return R.business("请输入账号和密码");
        }
        var data = adminService.login(username, password, request.getRemoteAddr());
        cookie(response,"CAMPUS_SESSION",String.valueOf(data.get("token")),true,request.isSecure(),28800);
        cookie(response,"CAMPUS_CSRF",java.util.UUID.randomUUID().toString(),false,request.isSecure(),28800);
        return R.ok(data);
    }

    @PostMapping("/auth/logout")
    public R<Void> logout(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
        cookie(response,"CAMPUS_SESSION","",true,request.isSecure(),0);
        cookie(response,"CAMPUS_CSRF","",false,request.isSecure(),0);
        adminService.logout();
        AdminContext.clear();
        return R.ok();
    }

    @PutMapping("/admin/password")
    public R<Void> password(@RequestBody Map<String, String> body) {
        adminService.changePassword(body.get("oldPassword"), body.get("newPassword"));
        return R.ok();
    }

    @GetMapping("/admin/current")
    public R<Map<String, Object>> current() {
        AdminContext.CurrentAdmin a = AdminContext.get();
        Map<String, Object> data = new HashMap<>();
        data.put("adminId", a.getAdminId());
        data.put("username", a.getUsername());
        data.put("roleCode", a.getRoleCode());
        data.put("orgId", a.getOrgId());
        data.put("mustChangePassword", adminService.mustChangePassword());
        return R.ok(data);
    }

    private void cookie(jakarta.servlet.http.HttpServletResponse response,String name,String value,boolean httpOnly,boolean secure,long seconds) {
        response.addHeader("Set-Cookie",org.springframework.http.ResponseCookie.from(name,value).httpOnly(httpOnly).secure(secure).sameSite("Strict").path("/").maxAge(seconds).build().toString());
    }

    public AuthController(AdminService adminService) {
        this.adminService = adminService;
    }
}
