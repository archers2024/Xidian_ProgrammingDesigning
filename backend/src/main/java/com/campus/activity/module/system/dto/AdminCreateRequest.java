package com.campus.activity.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 新增管理员请求（FR-12）。
 * 密码必须显式填写且满足策略，不再使用固定弱默认值。
 */
public class AdminCreateRequest {

    @NotBlank(message = "请填写登录账号")
    @Size(min = 3, max = 32, message = "账号长度需在 3-32 个字符之间")
    private String username;

    @Size(max = 32, message = "姓名不能超过 32 个字符")
    private String realName;

    @Size(max = 64, message = "部门名称不能超过 64 个字符")
    private String dept;

    @NotBlank(message = "请填写初始密码")
    @Size(min = 8, max = 64, message = "密码至少 8 位")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码需同时包含字母和数字")
    private String password;

    private String roleCode;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }
}
