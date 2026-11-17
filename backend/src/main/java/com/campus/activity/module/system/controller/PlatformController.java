package com.campus.activity.module.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.activity.common.BusinessException;
import com.campus.activity.common.R;
import com.campus.activity.module.system.entity.OperationLog;
import com.campus.activity.module.system.entity.Organization;
import com.campus.activity.module.system.service.AuditService;
import com.campus.activity.module.system.service.OrganizationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class PlatformController {
    private final OrganizationService organizations;
    private final AuditService audit;

    public PlatformController(OrganizationService organizations, AuditService audit) {
        this.organizations = organizations;
        this.audit = audit;
    }

    @GetMapping("/admin/organizations")
    public R<List<Organization>> available(@RequestParam(required=false) String keyword) { return R.ok(organizations.available(keyword)); }

    @GetMapping("/super/organizations")
    public R<List<Organization>> list() { return R.ok(organizations.list()); }

    public record CreateOrganizationRequest(String code, String name) { }

    @GetMapping("/super/organizations/page")
    public R<IPage<Organization>> page(@RequestParam(defaultValue="1")long current,@RequestParam(defaultValue="20")long size,@RequestParam(required=false)String keyword){return R.ok(organizations.page(current,size,keyword));}
    @PostMapping("/super/organizations")
    public R<Organization> create(@RequestBody CreateOrganizationRequest request) {
        return R.ok(organizations.create(request.code(), request.name()));
    }

    @PutMapping("/super/organizations/{id}/status")
    public R<Void> status(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        // Preserve strict integer parsing; silently truncating a decimal could disable an organization.
        organizations.changeStatus(id, Integer.valueOf(String.valueOf(body.get("status"))));
        return R.ok();
    }

    @PutMapping("/super/organizations/{id}/review-policy")
    public R<Void> reviewPolicy(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (!(body.get("requireReview") instanceof Boolean required)) {
            throw new BusinessException("审批策略必须为布尔值");
        }
        organizations.changeReviewPolicy(id, required);
        return R.ok();
    }

    @GetMapping("/super/audit-logs")
    public R<IPage<OperationLog>> audit(@RequestParam(defaultValue = "1") long current,
                                       @RequestParam(defaultValue = "20") long size,
                                       @RequestParam(required = false) String opType) {
        return R.ok(audit.page(current, size, opType));
    }
}
