package com.campus.events.auth;

import com.campus.events.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/csrf")
    public ApiResponse<AuthDtos.CsrfTokenView> csrf(CsrfToken csrfToken) {
        return ApiResponse.ok(new AuthDtos.CsrfTokenView(csrfToken.getToken(), csrfToken.getHeaderName()));
    }

    @PostMapping("/login")
    public ApiResponse<AuthDtos.LoginResult> login(
            @RequestBody AuthDtos.LoginRequest request, HttpServletRequest http, HttpServletResponse response) {
        return ApiResponse.ok(authService.login(request, http, response));
    }

    @GetMapping("/me")
    public ApiResponse<AuthDtos.AdminUser> me() {
        return ApiResponse.ok(authService.currentUser());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        authService.logout(request);
        return ApiResponse.ok(null);
    }
}
