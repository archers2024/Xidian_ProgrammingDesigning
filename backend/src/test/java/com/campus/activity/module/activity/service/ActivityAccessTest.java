package com.campus.activity.module.activity.service;

import com.campus.activity.common.BusinessException;
import com.campus.activity.module.activity.entity.Activity;
import com.campus.activity.module.activity.mapper.ActivityMapper;
import com.campus.activity.module.system.mapper.OrganizationMapper;
import com.campus.activity.security.AdminContext;
import com.campus.activity.security.OrganizationAccess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActivityAccessTest {
    @AfterEach void clearContext() { AdminContext.clear(); }

    @Test void lockRequiresAnExistingTransaction() {
        ActivityMapper mapper = mock(ActivityMapper.class);
        var target = new ActivityAccess(mapper, new OrganizationAccess(mock(OrganizationMapper.class)));
        var factory = new org.springframework.aop.framework.ProxyFactory(target);
        factory.setProxyTargetClass(true);
        var manager = new org.springframework.jdbc.datasource.DataSourceTransactionManager(mock(javax.sql.DataSource.class));
        factory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(manager,
                new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
        ActivityAccess proxy = (ActivityAccess) factory.getProxy();
        assertThrows(org.springframework.transaction.IllegalTransactionStateException.class, () -> proxy.lock(1L));
        verifyNoInteractions(mapper);
    }

    @Test void serviceReadCannotBypassOrganizationBoundary() {
        ActivityMapper mapper = mock(ActivityMapper.class);
        Activity activity = new Activity(); activity.setOrgId(20L);
        when(mapper.selectById(1L)).thenReturn(activity);
        ActivityAccess access = new ActivityAccess(mapper, new OrganizationAccess(mock(OrganizationMapper.class)));
        AdminContext.set(new AdminContext.CurrentAdmin(1L, "user", "ADMIN", 10L));
        assertThrows(BusinessException.class, () -> access.require(1L));
        assertThrows(BusinessException.class, () -> access.require(2L));
        AdminContext.set(new AdminContext.CurrentAdmin(1L, "root", "SUPER", 1L));
        assertSame(activity, access.require(1L));
    }
}
