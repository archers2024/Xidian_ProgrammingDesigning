package com.campus.activity.security;

import com.campus.activity.common.BusinessException;
import com.campus.activity.module.system.entity.Organization;
import com.campus.activity.module.system.mapper.OrganizationMapper;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/** Shared organization boundary for authenticated services and public activity endpoints. */
@Component
public class OrganizationAccess {
    private final OrganizationMapper organizations;

    public OrganizationAccess(OrganizationMapper organizations) {
        this.organizations = organizations;
    }

    public AdminContext.CurrentAdmin current() {
        var admin = AdminContext.get();
        if (admin == null) throw new BusinessException(40002, "请先登录");
        if (!admin.isSuper() && admin.getOrgId() == null) throw new BusinessException(40002, "账号尚未分配组织");
        if (!admin.isSuper() && admin.getOrgId()==null) throw new BusinessException(40002,"账号尚未分配组织");
        return admin;
    }

    public void requireSuper() {
        if (!current().isSuper()) throw new BusinessException(40002, "仅平台管理员可执行此操作");
    }

    public void requireScope(Long orgId) {
        var admin = current();
        if (!admin.isSuper() && !java.util.Objects.equals(admin.getOrgId(), orgId)) {
            throw new BusinessException(40002, "不能访问其他组织的数据");
        }
    }

    public Long resolveForCreate(Long requestedId) {
        Long orgId = requestedId == null ? current().getOrgId() : requestedId;
        requireScope(orgId);
        requireActive(orgId);
        return orgId;
    }

    /** A null query scope means platform-wide access, never anonymous access. */
    public Long queryScope() {
        var admin = current();
        return admin.isSuper() ? null : admin.getOrgId();
    }

    public Organization requireActive(Long id) {
        Organization org = id == null ? null : organizations.selectById(id);
        if (org == null || !Integer.valueOf(1).equals(org.getStatus())) {
            throw new BusinessException("活动所属组织不可用");
        }
        return org;
    }

    public boolean reviewRequired(Long id) {
        Organization org = organizations.selectById(id);
        return org != null && org.getRequireReview();
    }

    public Map<Long, Boolean> reviewPolicies(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return organizations.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Organization::getOrgId, Organization::getRequireReview));
    }
}
