package com.campus.activity.module.system.controller;

import com.campus.activity.common.R;
import com.campus.activity.module.system.entity.Admin;
import com.campus.activity.module.system.service.AdminService;

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
    public R<List<Admin>> list() {
        return R.ok(adminService.list());
    }

    @PostMapping("/accounts")
    public R<Void> create(@RequestBody Admin admin) {
        adminService.createAdmin(admin);
        return R.ok();
    }

    @PutMapping("/accounts/{id}/status")
    public R<Void> status(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        adminService.updateStatus(id, body.get("status"));
        return R.ok();
    }

    @PutMapping("/accounts/{id}/password")
    public R<Void> reset(@PathVariable Long id) {
        adminService.resetPassword(id);
        return R.ok();
    }

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }
}
