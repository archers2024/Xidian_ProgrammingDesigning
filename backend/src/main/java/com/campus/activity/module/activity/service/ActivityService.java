package com.campus.activity.module.activity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.activity.common.BusinessException;
import com.campus.activity.module.activity.entity.Activity;
import com.campus.activity.module.activity.entity.Attachment;
import com.campus.activity.module.activity.mapper.ActivityMapper;
import com.campus.activity.module.activity.mapper.AttachmentMapper;
import com.campus.activity.module.system.service.AdminService;
import com.campus.activity.security.AdminContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * B岗：活动生命周期管理（FR-02 ~ FR-05）。
 * 状态机：DRAFT → OPEN → CLOSED → ONGOING → FINISHED，任意状态可到 CANCELED。
 */
@Service

public class ActivityService {

    private final ActivityMapper activityMapper;
    private final AttachmentMapper attachmentMapper;
    private final AdminService adminService;

    /** ---------------- 创建 / 编辑 / 删除 ---------------- */

    @Transactional
    public Long create(Activity activity, List<Attachment> attachments) {
        validate(activity);
        activity.setActivityNo(generateNo());
        activity.setSignupGuid(UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        activity.setStatus("DRAFT");
        activity.setSignedCount(0);
        activity.setCheckinEnabled(0);
        if (activity.getCheckinRadius() == null) {
            activity.setCheckinRadius(500);
        }
        activity.setCheckinSecret(UUID.randomUUID().toString().replace("-", ""));
        activity.setCreatedBy(AdminContext.adminId());
        activity.setCreateTime(LocalDateTime.now());
        activity.setUpdateTime(LocalDateTime.now());
        activityMapper.insert(activity);
        saveAttachments(activity.getActivityId(), attachments);
        adminService.writeLog(AdminContext.adminId(), "ACTIVITY_CREATE",
                String.valueOf(activity.getActivityId()), "创建活动：" + activity.getTitle(), null);
        return activity.getActivityId();
    }

    @Transactional
    public void update(Activity activity, List<Attachment> attachments) {
        Activity old = require(activity.getActivityId());
        validate(activity);
        // Guid 不变，保证已转发的旧链接仍可访问（FR-05）
        activity.setSignupGuid(old.getSignupGuid());
        activity.setSignedCount(old.getSignedCount());
        activity.setStatus(old.getStatus());
        activity.setCreatedBy(old.getCreatedBy());
        activity.setCreateTime(old.getCreateTime());
        activity.setUpdateTime(LocalDateTime.now());
        if (activity.getQuota() != null && activity.getQuota() < old.getSignedCount()) {
            // 保留已有报名，但停止新报名（名额判定以 signed_count >= quota 为准）
            activity.setQuota(old.getSignedCount());
        }
        activityMapper.updateById(activity);
        if (attachments != null && !attachments.isEmpty()) {
            saveAttachments(activity.getActivityId(), attachments);
        }
        adminService.writeLog(AdminContext.adminId(), "ACTIVITY_UPDATE",
                String.valueOf(activity.getActivityId()), "编辑活动：" + activity.getTitle(), null);
    }

    @Transactional
    public void remove(Long activityId) {
        Activity old = require(activityId);
        old.setStatus("CANCELED");
        old.setUpdateTime(LocalDateTime.now());
        activityMapper.updateById(old);
        activityMapper.deleteById(activityId); // 逻辑删除（deleted=1）
        adminService.writeLog(AdminContext.adminId(), "ACTIVITY_CANCEL",
                String.valueOf(activityId), "取消活动：" + old.getTitle(), null);
    }

    /** ---------------- 发布（FR-04） ---------------- */

    @Transactional
    public void publish(Long activityId) {
        Activity a = require(activityId);
        if ("CANCELED".equals(a.getStatus())) {
            throw new BusinessException("已取消的活动不能发布");
        }
        if ("OPEN".equals(a.getStatus()) || "CLOSED".equals(a.getStatus())
                || "ONGOING".equals(a.getStatus()) || "FINISHED".equals(a.getStatus())) {
            throw new BusinessException("活动已发布，请勿重复操作");
        }
        a.setStatus("OPEN");
        a.setPublishTime(LocalDateTime.now());
        a.setUpdateTime(LocalDateTime.now());
        activityMapper.updateById(a);
        adminService.writeLog(AdminContext.adminId(), "ACTIVITY_PUBLISH",
                String.valueOf(activityId), "发布活动：" + a.getTitle(), null);
    }

    /** ---------------- 查询（FR-03） ---------------- */

    public IPage<Activity> page(long current, long size, String keyword, String typeCode,
                                String status, LocalDateTime from, LocalDateTime to) {
        refreshAllStatus();
        LambdaQueryWrapper<Activity> q = new LambdaQueryWrapper<Activity>()
                .like(keyword != null && !keyword.isBlank(), Activity::getTitle, keyword)
                .eq(typeCode != null && !typeCode.isBlank(), Activity::getTypeCode, typeCode)
                .eq(status != null && !status.isBlank(), Activity::getStatus, status)
                .ge(from != null, Activity::getStartTime, from)
                .le(to != null, Activity::getStartTime, to)
                .orderByDesc(Activity::getPublishTime)
                .orderByDesc(Activity::getActivityId);
        return activityMapper.selectPage(new Page<>(current, size), q);
    }

    public Activity detail(Long activityId) {
        Activity a = require(activityId);
        refreshStatus(a);
        return a;
    }

    public Activity byGuid(String guid) {
        Activity a = activityMapper.selectOne(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getSignupGuid, guid));
        if (a == null) {
            throw new BusinessException(40003, "活动不存在或链接已失效");
        }
        refreshStatus(a);
        return a;
    }

    public List<Attachment> attachments(Long activityId) {
        return attachmentMapper.selectList(new LambdaQueryWrapper<Attachment>()
                .eq(Attachment::getActivityId, activityId));
    }

    /** 供 D 岗签到使用：地点坐标、半径、开关 */
    public Activity checkinConfig(Long activityId) {
        return require(activityId);
    }

    /** ---------------- 状态机 ---------------- */

    /** 按当前时间推进单个活动状态 */
    public void refreshStatus(Activity a) {
        if (a == null || "CANCELED".equals(a.getStatus()) || "DRAFT".equals(a.getStatus())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String expected;
        if (now.isAfter(a.getEndTime())) {
            expected = "FINISHED";
        } else if (!now.isBefore(a.getStartTime())) {
            expected = "ONGOING";
        } else if (now.isAfter(a.getSignupDeadline())) {
            expected = "CLOSED";
        } else {
            expected = "OPEN";
        }
        if (!expected.equals(a.getStatus())) {
            String old = a.getStatus();
            a.setStatus(expected);
            a.setUpdateTime(now);
            activityMapper.updateById(a);
            adminService.writeLog(null, "ACTIVITY_STATUS_AUTO", String.valueOf(a.getActivityId()),
                    "状态自动流转：" + old + " → " + expected, null);
        }
    }

    /** 定时扫描：每 60 秒推进一次全量状态（配合前端展示实时性） */
    @Transactional
    public void refreshAllStatus() {
        List<Activity> list = activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                .in(Activity::getStatus, List.of("OPEN", "CLOSED", "ONGOING")));
        for (Activity a : list) {
            refreshStatus(a);
        }
    }

    /** ---------------- 内部方法 ---------------- */

    private Activity require(Long activityId) {
        Activity a = activityMapper.selectById(activityId);
        if (a == null) {
            throw new BusinessException(40003, "活动不存在");
        }
        return a;
    }

    private void validate(Activity a) {
        if (a.getTitle() == null || a.getTitle().isBlank()) {
            throw new BusinessException("请填写活动名称");
        }
        if (a.getStartTime() == null || a.getEndTime() == null || a.getSignupDeadline() == null) {
            throw new BusinessException("请填写完整的时间信息");
        }
        if (!a.getSignupDeadline().isBefore(a.getStartTime())) {
            throw new BusinessException("报名截止时间必须早于活动开始时间");
        }
        if (!a.getStartTime().isBefore(a.getEndTime())) {
            throw new BusinessException("活动开始时间必须早于结束时间");
        }
        if (a.getQuota() == null || a.getQuota() < 1 || a.getQuota() > 5000) {
            throw new BusinessException("人数上限需在 1 ~ 5000 之间");
        }
    }

    private String generateNo() {
        return "ACT" + System.currentTimeMillis();
    }

    private void saveAttachments(Long activityId, List<Attachment> attachments) {
        if (attachments == null) {
            return;
        }
        for (Attachment att : attachments) {
            if (att.getFileUrl() == null) {
                continue;
            }
            att.setAttId(null);
            att.setActivityId(activityId);
            att.setUploadTime(LocalDateTime.now());
            attachmentMapper.insert(att);
        }
    }

    public ActivityService(ActivityMapper activityMapper, AttachmentMapper attachmentMapper, AdminService adminService) {
        this.activityMapper = activityMapper;
        this.attachmentMapper = attachmentMapper;
        this.adminService = adminService;
    }
}
