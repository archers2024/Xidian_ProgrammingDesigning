package com.campus.activity.module.system.vo;

import com.campus.activity.module.system.entity.Admin;

import java.time.LocalDateTime;

/**
 * 账号列表视图对象（FR-12）。
 * 只暴露管理所需字段，认证相关字段（passwordHash / tokenVersion）永不出现在响应中。
 */
public class AdminSummary {

    private Long adminId;
    private String username;
    private String realName;
    private String dept;
    private String roleCode;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;

    public static AdminSummary of(Admin a) {
        AdminSummary v = new AdminSummary();
        v.adminId = a.getAdminId();
        v.username = a.getUsername();
        v.realName = a.getRealName();
        v.dept = a.getDept();
        v.roleCode = a.getRoleCode();
        v.status = a.getStatus();
        v.lastLoginTime = a.getLastLoginTime();
        v.createTime = a.getCreateTime();
        return v;
    }

    public Long getAdminId() {
        return adminId;
    }

    public String getUsername() {
        return username;
    }

    public String getRealName() {
        return realName;
    }

    public String getDept() {
        return dept;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public Integer getStatus() {
        return status;
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }
}
