package com.campus.activity.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.activity.common.BusinessException;
import com.campus.activity.module.system.dto.AdminCreateRequest;
import com.campus.activity.module.system.entity.Admin;
import com.campus.activity.module.system.entity.OperationLog;
import com.campus.activity.module.system.mapper.AdminMapper;
import com.campus.activity.module.system.mapper.OperationLogMapper;
import com.campus.activity.module.system.vo.AdminSummary;
import com.campus.activity.security.AdminContext;
import com.campus.activity.security.JwtUtil;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

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

        String token = jwtUtil.createToken(admin.getAdminId(), admin.getUsername(), admin.getRoleCode(),
                admin.getTokenVersion() == null ? 0 : admin.getTokenVersion());
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

    /**
     * FR-12：新增管理员。
     * 密码必须由操作者显式填写并满足策略；原先「留空填 12345678 再被策略拒绝」的自相矛盾逻辑已移除。
     */
    public void createAdmin(AdminCreateRequest req) {
        String username = req.getUsername() == null ? "" : req.getUsername().trim();
        if (adminMapper.selectCount(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, username)) > 0) {
            throw new BusinessException("账号已存在");
        }
        checkPassword(req.getPassword());
        String role = req.getRoleCode();
        if (role == null || role.isBlank()) {
            role = "ADMIN";
        }
        if (!"ADMIN".equals(role) && !"SUPER".equals(role)) {
            throw new BusinessException("角色只能是 ADMIN 或 SUPER");
        }

        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setRealName(req.getRealName());
        admin.setDept(req.getDept());
        admin.setRoleCode(role);
        admin.setPasswordHash(encoder.encode(req.getPassword()));
        admin.setStatus(1);
        admin.setTokenVersion(0);
        admin.setCreateTime(LocalDateTime.now());
        adminMapper.insert(admin);
        writeLog(AdminContext.adminId(), "ADMIN_CREATE", String.valueOf(admin.getAdminId()), "新增管理员", null);
    }

    /**
     * 启用 / 停用账号。
     * 每次切换都递增令牌版本，停用后该账号所有已签发令牌立即失效（启用也不会让旧令牌复活）。
     */
    public void updateStatus(Long adminId, Integer status) {
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException("账号不存在");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态值只能是 0 或 1");
        }
        if (status == 0 && isLastSuper(admin)) {
            throw new BusinessException("系统至少保留一个启用的超级管理员");
        }
        admin.setStatus(status);
        admin.setTokenVersion((admin.getTokenVersion() == null ? 0 : admin.getTokenVersion()) + 1);
        adminMapper.updateById(admin);
        writeLog(AdminContext.adminId(), "ADMIN_STATUS", String.valueOf(adminId), "修改账号状态为 " + status, null);
    }

    /**
     * 重置密码：生成一次性强密码返回给操作者，同时递增令牌版本使旧会话失效。
     * 不再使用固定密码 12345678。
     */
    public String resetPassword(Long adminId) {
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException("账号不存在");
        }
        String tempPassword = generateTempPassword();
        admin.setPasswordHash(encoder.encode(tempPassword));
        admin.setTokenVersion((admin.getTokenVersion() == null ? 0 : admin.getTokenVersion()) + 1);
        adminMapper.updateById(admin);
        writeLog(AdminContext.adminId(), "ADMIN_RESET_PWD", String.valueOf(adminId), "重置密码", null);
        return tempPassword;
    }

    /** 账号列表只返回管理字段，passwordHash / tokenVersion 不进入响应。 */
    public List<AdminSummary> list() {
        return adminMapper.selectList(new LambdaQueryWrapper<Admin>().orderByDesc(Admin::getAdminId))
                .stream().map(AdminSummary::of).toList();
    }

    private boolean isLastSuper(Admin admin) {
        if (!"SUPER".equals(admin.getRoleCode())) {
            return false;
        }
        Long activeSupers = adminMapper.selectCount(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getRoleCode, "SUPER")
                .eq(Admin::getStatus, 1)
                .ne(Admin::getAdminId, admin.getAdminId()));
        return activeSupers == null || activeSupers == 0;
    }

    private String generateTempPassword() {
        int digits = ThreadLocalRandom.current().nextInt(100_000_000);
        return "Ab" + String.format("%08d", digits);
    }

    private void checkPassword(String pwd) {
        if (pwd == null || pwd.length() < 8 || pwd.length() > 64
                || !pwd.matches(".*[A-Za-z].*") || !pwd.matches(".*\\d.*")) {
            throw new BusinessException("密码需 8-64 位且同时包含字母和数字");
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
