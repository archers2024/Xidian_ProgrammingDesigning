package com.campus.activity.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;


import java.time.LocalDateTime;

/**
 * 管理员表 t_admin（Owner：A岗）
 */

@TableName("t_admin")
public class Admin {

    @TableId(type = IdType.AUTO)
    private Long adminId;

    private String username;

    /** BCrypt 密文，不返回前端 */
    private String passwordHash;

    private String realName;

    private String dept;

    /** ADMIN / SUPER */
    private String roleCode;

    /** 1 启用 / 0 停用 */
    private Integer status;

    private LocalDateTime lastLoginTime;

    private LocalDateTime createTime;

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getDept() {
        return dept;
    }

    public void setDept(String dept) {
        this.dept = dept;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
