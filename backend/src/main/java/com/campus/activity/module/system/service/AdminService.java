package com.campus.activity.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.activity.common.BusinessException;
import com.campus.activity.module.system.entity.Admin;
import com.campus.activity.module.system.mapper.AdminMapper;
import com.campus.activity.security.AdminContext;
import com.campus.activity.security.JwtUtil;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A岗：认证与账号管理（FR-01、FR-12）。
 */
@Service

public class AdminService {

    private final AdminMapper adminMapper;
    private final AuditService audit;
    private final JwtUtil jwtUtil;
    private final com.campus.activity.module.system.mapper.OrganizationMapper organizations;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /** 失败次数计数（演示用内存实现，生产应放 Redis） */
    private final com.campus.activity.security.RateLimitStore rateLimits;
    private final com.campus.activity.security.OrganizationAccess access;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private static String temporaryPassword() { return "T9a" + java.util.UUID.randomUUID().toString().replace("-", ""); }

    public Map<String, Object> login(String username, String password, String ip) {
        if (username == null || !username.matches("[A-Za-z0-9_]{3,50}") || password == null
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) throw new BusinessException("账号或密码格式不正确");
        username=username.toLowerCase(java.util.Locale.ROOT);
        rateLimits.take("login-ip:" + ip, 60, 60);
        rateLimits.requireBelow("login-account:" + username, 5);
        Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, username));
        if (admin == null) { rateLimits.increment("login-account:" + username,1800); writeLog(null,"LOGIN_FAILURE",null,"认证失败",ip); throw new BusinessException("账号或密码错误"); }

        if (!encoder.matches(password, admin.getPasswordHash())) {
            int failures=rateLimits.increment("login-account:"+username,1800);
            writeLog(admin.getAdminId(),"LOGIN_FAILURE",String.valueOf(admin.getAdminId()),"认证失败",ip);
            if(failures>=5)throw new BusinessException(40029,"账号已锁定，请稍后重试或联系管理员重置密码");
            throw new BusinessException("账号或密码错误");
        }
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            throw new BusinessException("账号已停用，请联系超级管理员");
        }
        var loginOrg=organizations.selectById(admin.getOrgId());
        if(!"SUPER".equals(admin.getRoleCode())&&(loginOrg==null||!Integer.valueOf(1).equals(loginOrg.getStatus())))throw new BusinessException("所属组织已停用，请联系平台管理员");
        rateLimits.clear("login-account:"+username);

        adminMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Admin>()
                .eq(Admin::getAdminId, admin.getAdminId()).set(Admin::getLastLoginTime, LocalDateTime.now()));
        writeLog(admin.getAdminId(), "LOGIN", String.valueOf(admin.getAdminId()), "登录成功", ip);

        String token = jwtUtil.createToken(admin.getAdminId(), admin.getUsername(), admin.getRoleCode(), admin.getTokenVersion());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        Map<String, Object> profile = new HashMap<>();
        profile.put("adminId", admin.getAdminId());
        profile.put("username", admin.getUsername());
        profile.put("realName", admin.getRealName());
        profile.put("roleCode", admin.getRoleCode());
        profile.put("orgId",admin.getOrgId());
        profile.put("mustChangePassword", admin.getMustChangePassword());
        data.put("profile", profile);
        return data;
    }

    /** 新增管理员，默认临时密码需在首次使用时修改。 */
    @org.springframework.transaction.annotation.Transactional
    public String createAdmin(Admin admin) {
        access.requireSuper();
        String initialPassword = admin.getPasswordHash();
        if(admin.getOrgId()==null)admin.setOrgId(1L);
        var org=organizations.selectById(admin.getOrgId());
        if(org==null||!Integer.valueOf(1).equals(org.getStatus()))throw new BusinessException("请选择有效组织");
        com.campus.activity.util.InputRules.text(admin.getDept(), 100, false, "部门");
        if (admin.getUsername() == null || !admin.getUsername().matches("[A-Za-z0-9_]{3,50}")) throw new BusinessException("账号需为 3 至 50 位字母数字或下划线");
        if (admin.getRoleCode() != null && !List.of("ADMIN", "SUPER").contains(admin.getRoleCode())) throw new BusinessException("管理员角色不正确");
        if (admin.getRealName() == null || admin.getRealName().isBlank() || admin.getRealName().length() > 50) throw new BusinessException("请填写有效姓名");
        admin.setAdminId(null);
        if (adminMapper.selectCount(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, admin.getUsername())) > 0) {
            throw new BusinessException("账号已存在");
        }
        if (admin.getPasswordHash() == null || admin.getPasswordHash().isBlank()) {
            initialPassword = temporaryPassword();
            admin.setPasswordHash(initialPassword);
            admin.setMustChangePassword(true);
        } else {
            admin.setMustChangePassword(false);
        }
        checkPassword(admin.getPasswordHash());
        admin.setPasswordHash(encoder.encode(admin.getPasswordHash()));
        admin.setStatus(1);
        admin.setTokenVersion(0L);
        admin.setCreateTime(LocalDateTime.now());
        if (admin.getRoleCode() == null) {
            admin.setRoleCode("ADMIN");
        }
        adminMapper.insert(admin);
        writeLog(AdminContext.adminId(), "ADMIN_CREATE", String.valueOf(admin.getAdminId()), "新增管理员", null);
        return initialPassword;
    }

    @org.springframework.transaction.annotation.Transactional
    public void updateStatus(Long adminId, Integer status) {
        access.requireSuper();
        jdbc.queryForObject("SELECT guard_key FROM t_business_guard WHERE guard_key='SUPER_ADMIN' FOR UPDATE",String.class);
        if (status == null || (status != 0 && status != 1)) throw new BusinessException("账号状态必须为 0 或 1");
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException("账号不存在");
        }
        if (status == 0 && "SUPER".equals(admin.getRoleCode()) && adminMapper.selectCount(
                new LambdaQueryWrapper<Admin>().eq(Admin::getRoleCode, "SUPER").eq(Admin::getStatus, 1)) <= 1) {
            throw new BusinessException("不能停用最后一个超级管理员");
        }
        adminMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Admin>()
                .eq(Admin::getAdminId, adminId).set(Admin::getStatus, status).setSql("token_version = token_version + 1"));
        writeLog(AdminContext.adminId(), "ADMIN_STATUS", String.valueOf(adminId), "修改账号状态为 " + status, null);
    }

    @org.springframework.transaction.annotation.Transactional
    public String resetPassword(Long adminId) {
        access.requireSuper();
        String password = temporaryPassword();
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException("账号不存在");
        }
        adminMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Admin>()
                .eq(Admin::getAdminId, adminId).set(Admin::getPasswordHash, encoder.encode(password))
                .set(Admin::getMustChangePassword, true).setSql("token_version = token_version + 1"));
        rateLimits.clear("login-account:"+admin.getUsername());
        writeLog(AdminContext.adminId(), "ADMIN_RESET_PWD", String.valueOf(adminId), "重置密码", null);
        return password;
    }

    public com.baomidou.mybatisplus.core.metadata.IPage<Admin> page(long current,long size,String keyword) {
        access.requireSuper();if(current<1||size<1||size>100)throw new BusinessException("分页范围不正确");
        return adminMapper.pageWithOrg(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current,size),keyword==null?"":keyword);
    }
    public List<Admin> list() {
        access.requireSuper();
        return adminMapper.selectList(new LambdaQueryWrapper<Admin>().orderByDesc(Admin::getAdminId).last("LIMIT 100"));
    }

    private void checkPassword(String pwd) {
        if (pwd == null || pwd.length() < 8 || pwd.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72 || !pwd.matches(".*[A-Za-z].*") || !pwd.matches(".*\\d.*")) {
            throw new BusinessException("密码至少 8 位且包含字母和数字");
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public void changePassword(String oldPassword, String newPassword) {
        access.current();
        Admin admin = adminMapper.selectById(AdminContext.adminId());
        if (oldPassword == null || !encoder.matches(oldPassword, admin.getPasswordHash())) throw new BusinessException("原密码不正确");
        checkPassword(newPassword);
        if (oldPassword.equals(newPassword)) throw new BusinessException("新密码不能与原密码相同");
        int updated = adminMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Admin>()
                .eq(Admin::getAdminId, admin.getAdminId()).eq(Admin::getPasswordHash, admin.getPasswordHash())
                .set(Admin::getPasswordHash, encoder.encode(newPassword)).set(Admin::getMustChangePassword, false)
                .setSql("token_version = token_version + 1"));
        if (updated != 1) throw new BusinessException("账号信息已变更，请重新登录");
        writeLog(admin.getAdminId(),"PASSWORD_CHANGE",String.valueOf(admin.getAdminId()),"用户修改密码",null);
    }

    @org.springframework.transaction.annotation.Transactional
    public void logout() {
        access.current();
        writeLog(AdminContext.adminId(),"LOGOUT",String.valueOf(AdminContext.adminId()),"退出登录",null);
        adminMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Admin>()
                .eq(Admin::getAdminId, AdminContext.adminId()).setSql("token_version = token_version + 1"));
    }

    public boolean mustChangePassword() { return adminMapper.selectById(AdminContext.adminId()).getMustChangePassword(); }

    /** 统一日志写入口，B/C/D 三岗都调用它，不直接操作 t_operation_log */
    public void writeLog(Long adminId, String opType, String targetId, String detail, String ip) {
        audit.writeLog(adminId, opType, targetId, detail, ip);
    }

    public AdminService(AdminMapper adminMapper, AuditService audit, JwtUtil jwtUtil, com.campus.activity.module.system.mapper.OrganizationMapper organizations, com.campus.activity.security.RateLimitStore rateLimits, com.campus.activity.security.OrganizationAccess access, org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.adminMapper = adminMapper;
        this.audit = audit;
        this.jwtUtil = jwtUtil;
        this.organizations=organizations;
        this.rateLimits=rateLimits; this.access=access; this.jdbc=jdbc;
    }
}
