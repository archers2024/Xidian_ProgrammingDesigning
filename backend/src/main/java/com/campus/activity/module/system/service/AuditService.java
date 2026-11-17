package com.campus.activity.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.activity.common.BusinessException;
import com.campus.activity.module.system.entity.OperationLog;
import com.campus.activity.module.system.mapper.OperationLogMapper;
import com.campus.activity.security.OrganizationAccess;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class AuditService {
    private final OperationLogMapper logs;
    private final OrganizationAccess access;

    public AuditService(OperationLogMapper logs, OrganizationAccess access) {
        this.logs = logs;
        this.access = access;
    }

    /** Joins the caller's transaction so business mutations and their audit record commit together. */
    public void writeLog(Long adminId, String type, String targetId, String detail, String ip) {
        OperationLog entry = new OperationLog();
        entry.setAdminId(adminId);
        entry.setOpType(type);
        entry.setTargetId(targetId);
        entry.setOpDetail(detail);
        entry.setIp(ip);
        entry.setOpTime(LocalDateTime.now());
        logs.insert(entry);
    }

    public IPage<OperationLog> page(long current, long size, String opType) {
        access.requireSuper();
        if (current < 1 || size < 1 || size > 100) throw new BusinessException("分页范围不正确");
        return logs.selectPage(new Page<>(current, size), new LambdaQueryWrapper<OperationLog>()
                .eq(opType != null && !opType.isBlank(), OperationLog::getOpType, opType)
                .orderByDesc(OperationLog::getLogId));
    }
}
