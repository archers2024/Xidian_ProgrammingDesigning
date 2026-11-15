package com.campus.activity.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.activity.module.system.entity.Admin;
import com.campus.activity.module.system.mapper.AdminMapper;


import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 首次启动初始化默认超级管理员：admin / admin123（密码 BCrypt 加盐存储）。
 * 若 t_admin 已有数据则跳过，避免覆盖真实账号。
 */

@Component

public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);


    private final AdminMapper adminMapper;

    @Override
    public void run(String... args) {
        Long count = adminMapper.selectCount(new LambdaQueryWrapper<>());
        if (count != null && count > 0) {
            return;
        }
        Admin admin = new Admin();
        admin.setUsername("admin");
        admin.setPasswordHash(new BCryptPasswordEncoder().encode("admin123"));
        admin.setRealName("系统管理员");
        admin.setDept("信息化办公室");
        admin.setRoleCode("SUPER");
        admin.setStatus(1);
        admin.setCreateTime(LocalDateTime.now());
        adminMapper.insert(admin);
        log.info("已初始化默认超级管理员：admin / admin123，请登录后立即修改密码");
    }

    public DataInitializer(AdminMapper adminMapper) {
        this.adminMapper = adminMapper;
    }
}
