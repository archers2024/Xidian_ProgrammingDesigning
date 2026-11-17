package com.campus.activity.module.activity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.activity.common.BusinessException;
import com.campus.activity.module.activity.domain.ApprovalPolicy;
import com.campus.activity.module.activity.dto.ReviewDecisionRequest;
import com.campus.activity.module.activity.entity.Activity;
import com.campus.activity.module.activity.entity.ActivityReview;
import com.campus.activity.module.activity.mapper.ActivityMapper;
import com.campus.activity.module.activity.mapper.ActivityReviewMapper;
import com.campus.activity.module.system.service.AuditService;
import com.campus.activity.security.AdminContext;
import com.campus.activity.security.OrganizationAccess;
import com.campus.activity.util.InputRules;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewService {
    private final ActivityMapper activities;
    private final ActivityReviewMapper reviews;
    private final ActivityAccess access;
    private final OrganizationAccess organizations;
    private final AuditService audit;
    private final com.fasterxml.jackson.databind.ObjectMapper json;
    private final com.campus.activity.module.activity.mapper.AttachmentMapper attachments;

    public ReviewService(ActivityMapper activities, ActivityReviewMapper reviews, ActivityAccess access,
                         OrganizationAccess organizations, AuditService audit, com.fasterxml.jackson.databind.ObjectMapper json, com.campus.activity.module.activity.mapper.AttachmentMapper attachments) {
        this.activities = activities;
        this.reviews = reviews;
        this.access = access;
        this.organizations = organizations;
        this.audit = audit; this.json=json; this.attachments=attachments;
    }

    @Transactional
    public ActivityReview submit(Long activityId) {
        Activity activity = activeLocked(activityId);
        ApprovalPolicy.requireSubmittable(activity.getStatus(), activity.getApprovalStatus());
        ActivityReview review = new ActivityReview();
        review.setActivityId(activityId);
        review.setOrgId(activity.getOrgId());
        review.setSubmittedBy(AdminContext.adminId());
        review.setDecision(ApprovalPolicy.PENDING);
        review.setSubmittedAt(LocalDateTime.now());
        review.setTitleSnapshot(activity.getTitle()); review.setActivityVersion(activity.getVersion());
        try {
            String snapshot=json.writeValueAsString(java.util.Map.of("activity",activity,"attachments",attachments.selectList(new LambdaQueryWrapper<com.campus.activity.module.activity.entity.Attachment>().eq(com.campus.activity.module.activity.entity.Attachment::getActivityId,activityId))));
            review.setContentSnapshot(snapshot);
            review.setSnapshotHash(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(snapshot.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
        } catch(Exception e) { throw new IllegalStateException("审批快照生成失败",e); }
        reviews.insert(review);
        activity.setApprovalStatus(ApprovalPolicy.PENDING);
        activities.updateById(activity);
        audit.writeLog(AdminContext.adminId(), "REVIEW_SUBMIT", String.valueOf(activityId),
                "提交审核：" + activity.getTitle(), null);
        return review;
    }

    @Transactional
    public void decide(Long reviewId, ReviewDecisionRequest request) {
        organizations.requireSuper();
        if (request == null || request.decision() == null) throw new BusinessException("请选择审批结果");
        String decision = request.decision().name();
        InputRules.text(request.comment(), 500, ApprovalPolicy.REJECTED.equals(decision), "审批意见");
        ActivityReview found = reviews.selectById(reviewId);
        if (found == null) throw new BusinessException(40003, "审批记录不存在");
        // All mutations acquire the activity lock first, then the review lock.
        Activity activity = activeLocked(found.getActivityId());
        ActivityReview review = reviews.selectForUpdate(reviewId);
        ApprovalPolicy.requireDecidable(activity.getStatus(), activity.getApprovalStatus(),
                review.getDecision(), review.getSubmittedBy(), AdminContext.adminId());
        review.setDecision(decision);
        review.setComment(request.comment() == null ? "" : request.comment().trim());
        review.setReviewerId(AdminContext.adminId());
        review.setReviewedAt(LocalDateTime.now());
        reviews.updateById(review);
        activity.setApprovalStatus(decision);
        activities.updateById(activity);
        audit.writeLog(AdminContext.adminId(), "REVIEW_" + decision, String.valueOf(activity.getActivityId()),
                "审批活动：" + activity.getTitle(), null);
    }

    public IPage<ActivityReview> queue(long current, long size, String decision) {
        if (current < 1 || size < 1 || size > 100) throw new BusinessException("分页范围不正确");
        if (decision != null && !decision.isBlank() && !List.of(ApprovalPolicy.PENDING,
                ApprovalPolicy.APPROVED, ApprovalPolicy.REJECTED, ApprovalPolicy.CANCELED).contains(decision)) {
            throw new BusinessException("审批筛选不正确");
        }
        return reviews.queue(new Page<>(current, size), organizations.queryScope(), decision);
    }

    public List<ActivityReview> history(Long activityId) {
        access.require(activityId);
        return reviews.selectList(new LambdaQueryWrapper<ActivityReview>()
                .eq(ActivityReview::getActivityId, activityId).orderByDesc(ActivityReview::getReviewId));
    }

    public java.util.Map<String,Object> snapshot(Long reviewId) {
        var review=reviews.selectById(reviewId);if(review==null)throw new BusinessException(40003,"审批记录不存在");
        organizations.requireScope(review.getOrgId());
        if(review.getContentSnapshot()==null)throw new BusinessException("该历史记录没有内容快照");
        try{return java.util.Map.of("content",json.readTree(review.getContentSnapshot()),"hash",review.getSnapshotHash(),"version",review.getActivityVersion());}catch(java.io.IOException e){throw new IllegalStateException("Invalid stored snapshot",e);}
    }
    private Activity activeLocked(Long activityId) {
        Activity activity = access.lock(activityId);
        organizations.requireActive(activity.getOrgId());
        return activity;
    }
}
