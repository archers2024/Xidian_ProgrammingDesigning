package com.campus.activity.module.system.controller;

import com.campus.activity.common.R;
import com.campus.activity.module.system.dto.AdminCreateRequest;
import com.campus.activity.module.system.service.AdminService;
import com.campus.activity.module.system.vo.AdminSummary;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * A岗：管理员账号管理（FR-12），仅超级管理员可访问（/super/**）
 */
@RestController
@RequestMapping("/super/admin")

public class AdminController {

    private final AdminService adminService;

    @GetMapping("/accounts")
    public R<List<AdminSummary>> list() {
        return R.ok(adminService.list());
    }

    @PostMapping("/accounts")
    public R<Void> create(@Valid @RequestBody AdminCreateRequest req) {
        adminService.createAdmin(req);
        return R.ok();
    }

    @PutMapping("/accounts/{id}/status")
    public R<Void> status(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        adminService.updateStatus(id, body.get("status"));
        return R.ok();
    }

    /** 重置为一次性强密码，明文仅在本次响应中返回一次，由超管转交本人。 */
    @PutMapping("/accounts/{id}/password")
    public R<Map<String, String>> reset(@PathVariable Long id) {
        String tempPassword = adminService.resetPassword(id);
        return R.ok(Map.of("tempPassword", tempPassword));
    }

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }
}
