package com.campus.activity.module.activity.service;

import com.campus.activity.common.BusinessException;
import com.campus.activity.module.activity.entity.Activity;
import com.campus.activity.module.activity.mapper.ActivityMapper;
import com.campus.activity.security.OrganizationAccess;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Service-level access checks remain effective outside MVC interceptors. */
@Component
public class ActivityAccess {
    private final ActivityMapper activities;
    private final OrganizationAccess organizations;

    public ActivityAccess(ActivityMapper activities, OrganizationAccess organizations) {
        this.activities = activities;
        this.organizations = organizations;
    }

    public Activity require(Long id) {
        return scoped(activities.selectById(id));
    }

    /** The caller owns the transaction; acquiring a lock without one is an error. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Activity lock(Long id) {
        return scoped(activities.selectForUpdate(id));
    }

    private Activity scoped(Activity activity) {
        if (activity == null) throw new BusinessException(40003, "活动不存在");
        organizations.requireScope(activity.getOrgId());
        return activity;
    }
}
