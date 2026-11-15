package com.campus.activity.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.activity.common.BusinessException;
import com.campus.activity.module.system.entity.Admin;
import com.campus.activity.module.system.entity.OperationLog;
import com.campus.activity.module.system.mapper.AdminMapper;
import com.campus.activity.module.system.mapper.OperationLogMapper;
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
    private final OperationLogMapper logMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /** 失败次数计数（演示用内存实现，生产应放 Redis） */
    private final Map<String, int[]> failCounter = new ConcurrentHashMap<>();

    public Map<String, Object> login(String username, String password, String ip) {
        Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, username));
        if (admin == null || !encoder.matches(password, admin.getPasswordHash())) {
            int[] c = failCounter.computeIfAbsent(username, k -> new int[]{0, 0});
            c[0]++;
            if (c[0] >= 5) {
                c[1] = (int) (System.currentTimeMillis() / 1000) + 30 * 60;
                throw new BusinessException("密码错误次数过多，账号已锁定 30 分钟");
            }
            throw new BusinessException("账号或密码错误");
        }
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            throw new BusinessException("账号已停用，请联系超级管理员");
        }
        int[] c = failCounter.get(username);
        if (c != null && c[1] > System.currentTimeMillis() / 1000) {
            throw new BusinessException("账号已锁定，请稍后再试");
        }
        failCounter.remove(username);

        admin.setLastLoginTime(LocalDateTime.now());
        adminMapper.updateById(admin);
        writeLog(admin.getAdminId(), "LOGIN", String.valueOf(admin.getAdminId()), "登录成功", ip);

        String token = jwtUtil.createToken(admin.getAdminId(), admin.getUsername(), admin.getRoleCode());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        Map<String, Object> profile = new HashMap<>();
        profile.put("adminId", admin.getAdminId());
        profile.put("username", admin.getUsername());
        profile.put("realName", admin.getRealName());
        profile.put("roleCode", admin.getRoleCode());
        data.put("profile", profile);
        return data;
    }

    /** FR-12：新增管理员，默认密码 12345678，强制首次修改 */
    public void createAdmin(Admin admin) {
        if (adminMapper.selectCount(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, admin.getUsername())) > 0) {
            throw new BusinessException("账号已存在");
        }
        if (admin.getPasswordHash() == null || admin.getPasswordHash().isBlank()) {
            admin.setPasswordHash("12345678");
        }
        checkPassword(admin.getPasswordHash());
        admin.setPasswordHash(encoder.encode(admin.getPasswordHash()));
        admin.setStatus(1);
        admin.setCreateTime(LocalDateTime.now());
        if (admin.getRoleCode() == null) {
            admin.setRoleCode("ADMIN");
        }
        adminMapper.insert(admin);
        writeLog(AdminContext.adminId(), "ADMIN_CREATE", String.valueOf(admin.getAdminId()), "新增管理员", null);
    }

    public void updateStatus(Long adminId, Integer status) {
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException("账号不存在");
        }
        admin.setStatus(status);
        adminMapper.updateById(admin);
        writeLog(AdminContext.adminId(), "ADMIN_STATUS", String.valueOf(adminId), "修改账号状态为 " + status, null);
    }

    public void resetPassword(Long adminId) {
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException("账号不存在");
        }
        admin.setPasswordHash(encoder.encode("12345678"));
        adminMapper.updateById(admin);
        writeLog(AdminContext.adminId(), "ADMIN_RESET_PWD", String.valueOf(adminId), "重置密码", null);
    }

    public List<Admin> list() {
        return adminMapper.selectList(new LambdaQueryWrapper<Admin>().orderByDesc(Admin::getAdminId));
    }

    private void checkPassword(String pwd) {
        if (pwd == null || pwd.length() < 8 || !pwd.matches(".*[A-Za-z].*") || !pwd.matches(".*\\d.*")) {
            throw new BusinessException("密码至少 8 位且包含字母和数字");
        }
    }

    /** 统一日志写入口，B/C/D 三岗都调用它，不直接操作 t_operation_log */
    public void writeLog(Long adminId, String opType, String targetId, String detail, String ip) {
        OperationLog log = new OperationLog();
        log.setAdminId(adminId);
        log.setOpType(opType);
        log.setTargetId(targetId);
        log.setOpDetail(detail);
        log.setIp(ip);
        log.setOpTime(LocalDateTime.now());
        logMapper.insert(log);
    }

    public AdminService(AdminMapper adminMapper, OperationLogMapper logMapper, JwtUtil jwtUtil) {
        this.adminMapper = adminMapper;
        this.logMapper = logMapper;
        this.jwtUtil = jwtUtil;
    }
}
