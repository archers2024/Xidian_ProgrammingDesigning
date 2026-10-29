package com.campus.events.auth;

import com.campus.events.common.ApiException;
import com.campus.events.common.ErrorCode;
import com.campus.events.common.TimeSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthService(AdminMapper adminMapper, PasswordEncoder passwordEncoder) {
        this.adminMapper = adminMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthDtos.LoginResult login(AuthDtos.LoginRequest request, HttpServletRequest http, HttpServletResponse response) {
        String username = request == null || request.username() == null ? "" : request.username().trim();
        String password = request == null || request.password() == null ? "" : request.password();
        AdminAccount account = adminMapper.findByUsername(username);
        if (account == null || !account.isEnabled() || !passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        AdminPrincipal principal = new AdminPrincipal(account.getId(), account.getUsername(), account.getDisplayName());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        HttpSession oldSession = http.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        securityContextRepository.saveContext(context, http, response);
        return new AuthDtos.LoginResult(
                new AuthDtos.AdminUser(principal.id(), principal.username(), principal.displayName()),
                TimeSupport.now().plusHours(8));
    }

    public AuthDtos.AdminUser currentUser() {
        AdminPrincipal principal = CurrentAdmin.get();
        return new AuthDtos.AdminUser(principal.id(), principal.username(), principal.displayName());
    }

    public void logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
