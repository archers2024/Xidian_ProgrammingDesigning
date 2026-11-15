package com.campus.activity.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;


import java.time.LocalDateTime;

/**
 * 操作日志表 t_operation_log（Owner：A岗）
 * 只能通过统一日志接口写入，业务代码不得直接操作该表。
 */

@TableName("t_operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long logId;

    private Long adminId;

    private String opType;

    private String targetId;

    private String opDetail;

    private String ip;

    private LocalDateTime opTime;

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public String getOpType() {
        return opType;
    }

    public void setOpType(String opType) {
        this.opType = opType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getOpDetail() {
        return opDetail;
    }

    public void setOpDetail(String opDetail) {
        this.opDetail = opDetail;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public LocalDateTime getOpTime() {
        return opTime;
    }

    public void setOpTime(LocalDateTime opTime) {
        this.opTime = opTime;
    }
}
