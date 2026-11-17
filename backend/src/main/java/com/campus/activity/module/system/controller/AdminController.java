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

    @GetMapping("/accounts/page")
    public R<com.baomidou.mybatisplus.core.metadata.IPage<Admin>> page(@RequestParam(defaultValue="1")long current,@RequestParam(defaultValue="20")long size,@RequestParam(required=false)String keyword){return R.ok(adminService.page(current,size,keyword));}
    @PostMapping("/accounts")
    public R<Map<String,String>> create(@RequestBody Admin admin) {
        return R.ok(Map.of("temporaryPassword", adminService.createAdmin(admin)));
    }

    @PutMapping("/accounts/{id}/status")
    public R<Void> status(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        adminService.updateStatus(id, body.get("status"));
        return R.ok();
    }

    @PutMapping("/accounts/{id}/password")
    public R<Map<String,String>> reset(@PathVariable Long id) {
        return R.ok(Map.of("temporaryPassword", adminService.resetPassword(id)));
    }

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }
}
