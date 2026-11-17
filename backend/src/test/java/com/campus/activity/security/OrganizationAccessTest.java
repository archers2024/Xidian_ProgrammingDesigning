package com.campus.activity.security;

import com.campus.activity.common.BusinessException;
import com.campus.activity.module.system.entity.Organization;
import com.campus.activity.module.system.mapper.OrganizationMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrganizationAccessTest {
    private OrganizationMapper mapper;
    private OrganizationAccess access;

    @BeforeEach void setUp() { mapper = mock(OrganizationMapper.class); access = new OrganizationAccess(mapper); }
    @AfterEach void clearContext() { AdminContext.clear(); }

    @Test void anonymousCallerNeverGetsPlatformScope() {
        assertThrows(BusinessException.class, access::queryScope);
        assertThrows(BusinessException.class, () -> access.resolveForCreate(1L));
    }

    @Test void administratorCannotCrossOrganizationEvenWithoutHttpInterceptor() {
        AdminContext.set(new AdminContext.CurrentAdmin(1L, "user", "ADMIN", 10L));
        assertEquals(10L, access.queryScope());
        assertDoesNotThrow(() -> access.requireScope(10L));
        assertThrows(BusinessException.class, () -> access.requireScope(20L));
        assertThrows(BusinessException.class, () -> access.resolveForCreate(20L));
        assertThrows(BusinessException.class, access::requireSuper);
        verifyNoInteractions(mapper);
    }

    @Test void implicitOrganizationUsesActiveMembership() {
        AdminContext.set(new AdminContext.CurrentAdmin(1L, "user", "ADMIN", 10L));
        Organization org = new Organization(); org.setStatus(1);
        when(mapper.selectById(10L)).thenReturn(org);
        assertEquals(10L, access.resolveForCreate(null));
        org.setStatus(0);
        assertThrows(BusinessException.class, () -> access.resolveForCreate(null));
    }

    @Test void superStillCannotCreateInMissingOrganization() {
        AdminContext.set(new AdminContext.CurrentAdmin(1L, "root", "SUPER", 1L));
        assertNull(access.queryScope());
        assertDoesNotThrow(access::requireSuper);
        assertThrows(BusinessException.class, () -> access.resolveForCreate(20L));
    }
}
