package com.campus.activity.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.activity.common.BusinessException;
import com.campus.activity.module.system.entity.Admin;
import com.campus.activity.module.system.entity.Organization;
import com.campus.activity.module.system.mapper.AdminMapper;
import com.campus.activity.module.system.mapper.OrganizationMapper;
import com.campus.activity.security.AdminContext;
import com.campus.activity.security.OrganizationAccess;
import com.campus.activity.util.InputRules;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrganizationService {
    private final OrganizationMapper organizations;
    private final AdminMapper accounts;
    private final OrganizationAccess access;
    private final AuditService audit;

    public OrganizationService(OrganizationMapper organizations, AdminMapper accounts,
                               OrganizationAccess access, AuditService audit) {
        this.organizations = organizations;
        this.accounts = accounts;
        this.access = access;
        this.audit = audit;
    }

    public List<Organization> available(String keyword) {
        Long scope=access.queryScope();String text=keyword==null?"":keyword.trim();
        if(text.length()>100)throw new BusinessException("组织关键字过长");
        return organizations.selectList(new LambdaQueryWrapper<Organization>().eq(Organization::getStatus,1)
            .eq(scope!=null,Organization::getOrgId,scope)
            .and(!text.isBlank(),q->q.like(Organization::getName,text).or().like(Organization::getCode,text))
            .orderByAsc(Organization::getOrgId).last("LIMIT 50"));
    }

    public com.baomidou.mybatisplus.core.metadata.IPage<Organization> page(long current,long size,String keyword) {
        access.requireSuper();if(current<1||size<1||size>100)throw new BusinessException("分页范围不正确");
        return organizations.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current,size),new LambdaQueryWrapper<Organization>().like(keyword!=null&&!keyword.isBlank(),Organization::getName,keyword).orderByDesc(Organization::getOrgId));
    }
    public List<Organization> list() {
        access.requireSuper();
        return organizations.selectList(new LambdaQueryWrapper<Organization>().orderByDesc(Organization::getOrgId).last("LIMIT 100"));
    }

    @Transactional
    public Organization create(String code, String name) {
        access.requireSuper();
        if (code == null || !code.matches("[A-Za-z0-9_]{2,40}")) {
            throw new BusinessException("组织编码需为2至40位字母数字或下划线");
        }
        InputRules.text(name, 100, true, "组织名称");
        Organization org = new Organization();
        org.setCode(code);
        org.setName(name.trim());
        org.setStatus(1);
        org.setRequireReview(true);
        org.setCreateTime(LocalDateTime.now().withNano(0));
        organizations.insert(org);
        audit("ORG_CREATE", org.getOrgId(), "新建组织：" + org.getName());
        return org;
    }

    @Transactional
    public void changeStatus(Long id, Integer status) {
        access.requireSuper();
        if (status == null || (status != 0 && status != 1)) throw new BusinessException("组织状态不正确");
        if (Long.valueOf(1).equals(id) && status == 0) throw new BusinessException("默认组织不可停用");
        require(id);
        // Sparse updates avoid overwriting a concurrently changed review policy.
        organizations.update(null, new LambdaUpdateWrapper<Organization>()
                .eq(Organization::getOrgId, id).set(Organization::getStatus, status));
        accounts.update(null, new LambdaUpdateWrapper<Admin>().eq(Admin::getOrgId, id)
                .ne(Admin::getRoleCode, "SUPER").setSql("token_version = token_version + 1"));
        audit("ORG_STATUS", id, "组织状态：" + status);
    }

    @Transactional
    public void changeReviewPolicy(Long id, Boolean required) {
        access.requireSuper();
        if (required == null) throw new BusinessException("审批策略必须为布尔值");
        require(id);
        organizations.update(null, new LambdaUpdateWrapper<Organization>()
                .eq(Organization::getOrgId, id).set(Organization::getRequireReview, required));
        audit("ORG_REVIEW_POLICY", id, "发布前审核：" + required);
    }

    private Organization require(Long id) {
        Organization org = organizations.selectById(id);
        if (org == null) throw new BusinessException("组织不存在");
        return org;
    }

    private void audit(String type, Long id, String detail) {
        audit.writeLog(AdminContext.adminId(), type, String.valueOf(id), detail, null);
    }
}
