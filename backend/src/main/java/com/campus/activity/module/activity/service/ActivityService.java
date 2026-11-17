package com.campus.activity.module.activity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.activity.module.activity.entity.ActivityReview;
import com.campus.activity.module.activity.mapper.ActivityReviewMapper;
import com.campus.activity.module.activity.domain.ApprovalPolicy;
import com.campus.activity.security.OrganizationAccess;
import com.campus.activity.module.activity.domain.ActivityRules;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.activity.common.BusinessException;
import com.campus.activity.module.activity.entity.Activity;
import com.campus.activity.module.activity.entity.Attachment;
import com.campus.activity.module.activity.mapper.ActivityMapper;
import com.campus.activity.module.activity.mapper.AttachmentMapper;
import com.campus.activity.module.system.service.AuditService;
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
    private final AuditService auditService;
    private final OrganizationAccess organizations;
    private final ActivityAccess access;
    private final ActivityReviewMapper reviews;
    private final com.campus.activity.module.system.service.FileAssetService assets;

    /** ---------------- 创建 / 编辑 / 删除 ---------------- */

    @Transactional
    public Long create(Activity activity, List<Attachment> attachments) {
        activity.setOrgId(organizations.resolveForCreate(activity.getOrgId()));
        ActivityRules.validate(activity);
        assets.link(activity.getCoverUrl(),activity.getOrgId());
        activity.setActivityNo(generateNo());
        activity.setSignupGuid(UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        activity.setStatus("DRAFT");
        activity.setVersion(0L);
        activity.setApprovalStatus("NONE");
        activity.setSignedCount(0);
        activity.setCheckinEnabled(0);
        if (activity.getCheckinRadius() == null) {
            activity.setCheckinRadius(500);
        }
        activity.setCheckinSecret(UUID.randomUUID().toString().replace("-", ""));
        activity.setCreatedBy(AdminContext.adminId());
        activity.setCreateTime(LocalDateTime.now().withNano(0));
        activity.setUpdateTime(LocalDateTime.now());
        activityMapper.insert(activity);
        saveAttachments(activity.getActivityId(), attachments);
        auditService.writeLog(AdminContext.adminId(), "ACTIVITY_CREATE",
                String.valueOf(activity.getActivityId()), "创建活动：" + activity.getTitle(), null);
        return activity.getActivityId();
    }

    @Transactional
    public void update(Activity activity, List<Attachment> attachments) {
        Activity old = requireForUpdate(activity.getActivityId());
        if (activity.getVersion()==null || !activity.getVersion().equals(old.getVersion())) throw new BusinessException(40009,"活动已更新，请刷新后重新编辑");
        activity.setVersion(old.getVersion()+1);
        // 题目要求所有生命周期状态可编辑；历史审批快照保留，待审旧稿撤销。
        activity.setApprovalStatus("NONE");
        if(activity.getOrgId()!=null&&!activity.getOrgId().equals(old.getOrgId()))throw new BusinessException("活动归属组织不能变更");
        activity.setOrgId(old.getOrgId());
        ActivityRules.validate(activity);
        assets.link(activity.getCoverUrl(),activity.getOrgId());
        // Guid 不变，保证已转发的旧链接仍可访问（FR-05）
        activity.setSignupGuid(old.getSignupGuid());
        activity.setSignedCount(old.getSignedCount());
        activity.setStatus(old.getStatus());
        refreshStatus(activity);
        activity.setCreatedBy(old.getCreatedBy());
        activity.setCreateTime(old.getCreateTime());
        activity.setUpdateTime(LocalDateTime.now());
        if (activity.getQuota() != null && activity.getQuota() < old.getSignedCount()) {
            // 保留已有报名，但停止新报名（名额判定以 signed_count >= quota 为准）
            activity.setQuota(old.getSignedCount());
        }
        reviews.update(null,new LambdaUpdateWrapper<ActivityReview>()
                .eq(ActivityReview::getActivityId,activity.getActivityId()).eq(ActivityReview::getDecision,"PENDING")
                .set(ActivityReview::getDecision,"CANCELED").set(ActivityReview::getReviewedAt,LocalDateTime.now())
                .set(ActivityReview::getComment,"活动已编辑，原申请撤销，请以新版本为准"));
        activityMapper.updateById(activity);
        if (attachments != null) {
            attachmentMapper.delete(new LambdaQueryWrapper<Attachment>().eq(Attachment::getActivityId, activity.getActivityId()));
            saveAttachments(activity.getActivityId(), attachments);
        }
        auditService.writeLog(AdminContext.adminId(), "ACTIVITY_UPDATE",
                String.valueOf(activity.getActivityId()), "编辑活动：" + activity.getTitle(), null);
    }

    @Transactional
    public void remove(Long activityId) {
        Activity old = requireForUpdate(activityId);
        reviews.update(null,new LambdaUpdateWrapper<ActivityReview>()
                .eq(ActivityReview::getActivityId,activityId).eq(ActivityReview::getDecision,"PENDING")
                .set(ActivityReview::getDecision,"CANCELED").set(ActivityReview::getReviewedAt,LocalDateTime.now()));
        old.setStatus("CANCELED");
        old.setUpdateTime(LocalDateTime.now());
        activityMapper.updateById(old);
        activityMapper.deleteById(activityId); // 逻辑删除（deleted=1）
        auditService.writeLog(AdminContext.adminId(), "ACTIVITY_CANCEL",
                String.valueOf(activityId), "删除活动（保留历史记录）：" + old.getTitle(), null);
    }

    /** ---------------- 发布（FR-04） ---------------- */

    @Transactional
    public void publish(Long activityId) {
        Activity a = requireForUpdate(activityId);
        requireOrganization(a.getOrgId());
        ApprovalPolicy.requirePublishable(a.getApprovalStatus(), reviewRequired(a.getOrgId()));
        if ("CANCELED".equals(a.getStatus())) {
            throw new BusinessException("已取消的活动不能发布");
        }
        if ("OPEN".equals(a.getStatus()) || "CLOSED".equals(a.getStatus())
                || "ONGOING".equals(a.getStatus()) || "FINISHED".equals(a.getStatus())) {
            throw new BusinessException("活动已发布，请勿重复操作");
        }
        if (!a.getEndTime().isAfter(LocalDateTime.now())) throw new BusinessException("已过期的活动不能发布");
        a.setStatus("OPEN");
        a.setPublishTime(LocalDateTime.now());
        a.setUpdateTime(LocalDateTime.now());
        activityMapper.updateById(a);
        auditService.writeLog(AdminContext.adminId(), "ACTIVITY_PUBLISH",
                String.valueOf(activityId), "发布活动：" + a.getTitle(), null);
    }

    /** ---------------- 查询（FR-03） ---------------- */

    public IPage<Activity> page(long current, long size, String keyword, String typeCode,
                                String status, LocalDateTime from, LocalDateTime to) {
        if (current < 1 || size < 1 || size > 100) throw new BusinessException("分页参数超出范围，每页 1 至 100 条");
        Long scope = organizations.queryScope();
        LambdaQueryWrapper<Activity> q = new LambdaQueryWrapper<Activity>()
                .eq(scope != null, Activity::getOrgId, scope)
                .like(keyword != null && !keyword.isBlank(), Activity::getTitle, keyword)
                .eq(typeCode != null && !typeCode.isBlank(), Activity::getTypeCode, typeCode)
                .eq(status != null && !status.isBlank(), Activity::getStatus, status)
                .ge(from != null, Activity::getStartTime, from)
                .le(to != null, Activity::getStartTime, to)
                .orderByDesc(Activity::getPublishTime)
                .orderByDesc(Activity::getActivityId);
        IPage<Activity> result = activityMapper.selectPage(new Page<>(current, size), q);
        var policies = organizations.reviewPolicies(result.getRecords().stream().map(Activity::getOrgId).distinct().toList());
        result.getRecords().forEach(row -> row.setReviewRequired(policies.getOrDefault(row.getOrgId(), false)));
        return result;
    }

    public Activity detail(Long activityId) {
        Activity a = require(activityId);
        a.setReviewRequired(reviewRequired(a.getOrgId()));
        refreshStatus(a);
        return a;
    }

    public Activity byGuid(String guid) {
        Activity a = activityMapper.selectOne(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getSignupGuid, guid));
        if (a == null) {
            throw new BusinessException(40003, "活动不存在或链接已失效");
        }
        requireOrganization(a.getOrgId());
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

    @Transactional
    public void setCheckinEnabled(Long id, Integer enabled) {
        if (enabled == null || (enabled != 0 && enabled != 1)) throw new BusinessException("签到开关必须为 0 或 1");
        Activity a = requireForUpdate(id);
        refreshStatus(a);
        if (enabled == 1 && !"ONGOING".equals(a.getStatus())) throw new BusinessException("只有进行中的活动可开启签到");
        a.setCheckinEnabled(enabled);
        a.setUpdateTime(LocalDateTime.now());
        activityMapper.updateById(a);
        auditService.writeLog(AdminContext.adminId(), "CHECKIN_SWITCH", String.valueOf(id), "签到开关：" + enabled, null);
    }

    /** ---------------- 状态机 ---------------- */

    /** 按当前时间推进单个活动状态 */
    public void refreshStatus(Activity a) {
        if (a == null || "CANCELED".equals(a.getStatus()) || "DRAFT".equals(a.getStatus())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String expected;
        if (!now.isBefore(a.getEndTime())) {
            expected = "FINISHED";
        } else if (!now.isBefore(a.getStartTime())) {
            expected = "ONGOING";
        } else if (!now.isBefore(a.getSignupDeadline())) {
            expected = "CLOSED";
        } else {
            expected = "OPEN";
        }
        if (!expected.equals(a.getStatus())) {
            a.setStatus(expected);
        }
    }

    /** 定时扫描：每 60 秒推进一次全量状态（配合前端展示实时性） */
    @Transactional
    public void refreshAllStatus() {
        activityMapper.refreshStatuses();
    }

    /** ---------------- 内部方法 ---------------- */

    private Activity require(Long activityId) {
        return access.require(activityId);
    }

    private Activity requireForUpdate(Long id) {
        return access.lock(id);
    }

    private String generateNo() {
        return "ACT" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
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
            var owner=activityMapper.selectById(activityId);assets.link(att.getFileUrl(),owner.getOrgId());
            attachmentMapper.insert(att);
        }
    }

    public void requireOrganization(Long id) {
        organizations.requireActive(id);
    }

    public boolean reviewRequired(Long id) {
        return organizations.reviewRequired(id);
    }

    public ActivityService(ActivityMapper activityMapper, AttachmentMapper attachmentMapper,
                           AuditService auditService, OrganizationAccess organizations,
                           ActivityReviewMapper reviews, ActivityAccess access, com.campus.activity.module.system.service.FileAssetService assets) {
        this.activityMapper = activityMapper;
        this.attachmentMapper = attachmentMapper;
        this.auditService = auditService;
        this.organizations = organizations;
        this.reviews = reviews;
        this.access = access; this.assets=assets;
    }
}
