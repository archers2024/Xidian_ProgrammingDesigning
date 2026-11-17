package com.campus.activity.module.activity.domain;

import com.campus.activity.common.BusinessException;

/** Approval invariants independent of HTTP, persistence and transaction plumbing. */
public final class ApprovalPolicy {
    public static final String NONE = "NONE";
    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String CANCELED = "CANCELED";

    private ApprovalPolicy() { }

    public static void requireSubmittable(String status, String approval) {
        if (!"DRAFT".equals(status)) throw new BusinessException("仅草稿可提交审核");
        if (PENDING.equals(approval) || APPROVED.equals(approval)) {
            throw new BusinessException("活动正在审核或已通过，请勿重复提交");
        }
    }

    public static void requirePublishable(String approval, boolean required) {
        if (PENDING.equals(approval) || (required && !APPROVED.equals(approval))) {
            throw new BusinessException("活动需要审核通过后才能发布");
        }
    }

    public static void requireDecidable(String status, String approval, String decision,
                                        Long submittedBy, Long reviewerId) {
        if (!"DRAFT".equals(status) || !PENDING.equals(approval) || !PENDING.equals(decision)) {
            throw new BusinessException("该审批已处理或活动已变更");
        }
        if (java.util.Objects.equals(submittedBy, reviewerId)) {
            throw new BusinessException(40002, "提交者不能审批自己的申请");
        }
    }
}
