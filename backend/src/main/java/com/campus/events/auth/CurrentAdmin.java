package com.campus.events.auth;

import com.campus.events.common.ApiException;
import com.campus.events.common.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentAdmin {

    private CurrentAdmin() {
    }

    public static AdminPrincipal get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AdminPrincipal principal) {
            return principal;
        }
        throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }
}
