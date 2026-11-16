package com.campus.activity.security;

import com.campus.activity.common.R;
import com.campus.activity.module.system.entity.Admin;
import com.campus.activity.module.system.mapper.AdminMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器：
 * - /auth/**、/promo/**、/signup、/checkin、/activity/{id}/qrcode 等对外开放；
 * - /admin/** 需登录；
 * - /super/** 需超级管理员。
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final AdminMapper adminMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LoginInterceptor(JwtUtil jwtUtil, AdminMapper adminMapper) {
        this.jwtUtil = jwtUtil;
        this.adminMapper = adminMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = normalizePath(request);
        boolean needLogin = uri.startsWith("/admin/") || uri.startsWith("/super/") || uri.startsWith("/file/");
        if (!needLogin) {
            return true;
        }

        String token = resolveToken(request);
        if (token == null) {
            writeUnauthorized(response, "未登录或登录已过期");
            return false;
        }
        Claims claims;
        try {
            claims = jwtUtil.parse(token);
        } catch (Exception e) {
            writeUnauthorized(response, "未登录或登录已过期");
            return false;
        }

        Long adminId;
        try {
            adminId = Long.valueOf(String.valueOf(claims.get("adminId")));
        } catch (Exception e) {
            writeUnauthorized(response, "未登录或登录已过期");
            return false;
        }

        // 每次请求回查账号：停用立即生效，角色以库中当前值为准，不信任令牌里的旧角色
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null || admin.getStatus() == null || admin.getStatus() != 1) {
            writeUnauthorized(response, "账号已停用，请联系超级管理员");
            return false;
        }
        Object tvClaim = claims.get("tv");
        int tokenVersion = tvClaim == null ? 0 : Integer.parseInt(String.valueOf(tvClaim));
        int currentVersion = admin.getTokenVersion() == null ? 0 : admin.getTokenVersion();
        if (tokenVersion != currentVersion) {
            writeUnauthorized(response, "登录状态已失效，请重新登录");
            return false;
        }

        AdminContext.set(new AdminContext.CurrentAdmin(adminId, admin.getUsername(), admin.getRoleCode()));

        if (uri.startsWith("/super/") && !AdminContext.get().isSuper()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(R.fail(40002, "无权限访问")));
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AdminContext.clear();
    }

    /**
     * 得到不含网关前缀与 context-path 的路径，保证三种部署形态下 /admin/** 都会被拦截：
     * 1) jar 直连、后端根路径；2) Nginx 原样转发 /api/v1；3) 后端自带 context-path=/api/v1。
     * 早期版本直接用 getRequestURI()，在形态 3 下 URI 为 /api/v1/admin/...，
     * startsWith("/admin/") 判断失败，导致所有后台接口免鉴权。
     */
    private String normalizePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            uri = uri.substring(ctx.length());
        }
        if (uri.startsWith("/api/v1")) {
            uri = uri.substring("/api/v1".length());
        }
        return uri.isEmpty() ? "/" : uri;
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return request.getParameter("token");
    }

    private void writeUnauthorized(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(40002, msg)));
    }
}
