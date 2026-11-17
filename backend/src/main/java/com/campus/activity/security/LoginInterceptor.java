package com.campus.activity.security;

import com.campus.activity.common.R;
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
    private final com.campus.activity.module.system.mapper.OrganizationMapper organizations;
    private final com.campus.activity.module.activity.mapper.ActivityMapper activities;
    private final com.campus.activity.module.system.mapper.AdminMapper adminMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LoginInterceptor(JwtUtil jwtUtil, com.campus.activity.module.system.mapper.AdminMapper adminMapper, com.campus.activity.module.system.mapper.OrganizationMapper organizations, com.campus.activity.module.activity.mapper.ActivityMapper activities) {
        this.jwtUtil = jwtUtil;
        this.adminMapper = adminMapper;
        this.organizations=organizations;this.activities=activities;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        response.setHeader("X-Content-Type-Options", "nosniff");
        AdminContext.clear();
        if (org.springframework.web.cors.CorsUtils.isPreFlightRequest(request)) return true;
        String uri = normalizePath(request);
        boolean operationsEndpoint = (uri.equals("/actuator") || uri.startsWith("/actuator/"))
                && !uri.equals("/actuator/health") && !uri.startsWith("/actuator/health/");
        Object matched = request.getAttribute(org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (matched instanceof String pattern) uri = pattern;
        boolean needLogin = operationsEndpoint || uri.startsWith("/admin/") || uri.startsWith("/super/")
                || (uri.startsWith("/file-assets/")&&!uri.startsWith("/file-assets/public/")) || uri.startsWith("/file/") || uri.startsWith("/actuator/prometheus") || uri.startsWith("/checkin/qrcode/") || uri.equals("/auth/logout");
        if (!needLogin) {
            return true;
        }

        String token = resolveToken(request);
        if (token == null) {
            writeUnauthorized(response, "未登录或登录已过期");
            return false;
        }
        if ((request.getHeader(HttpHeaders.AUTHORIZATION) == null || !request.getHeader(HttpHeaders.AUTHORIZATION).startsWith("Bearer ")) && !java.util.Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod())) {
            String csrf = null;
            if (request.getCookies() != null) for (var cookie : request.getCookies()) if ("CAMPUS_CSRF".equals(cookie.getName())) csrf = cookie.getValue();
            if (csrf == null || !java.security.MessageDigest.isEqual(csrf.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    request.getHeader("X-CSRF-Token") == null ? new byte[0] : request.getHeader("X-CSRF-Token").getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                response.sendError(403, "CSRF validation failed"); return false;
            }
        }
        Claims claims;
        try {
            claims = jwtUtil.parse(token);
        } catch (Exception e) {
            writeUnauthorized(response, "未登录或登录已过期");
            return false;
        }

        try {
            Long adminId = Long.valueOf(String.valueOf(claims.get("adminId")));
            var admin = adminMapper.selectById(adminId);
            if (admin == null || !Integer.valueOf(1).equals(admin.getStatus())
                    || !admin.getTokenVersion().equals(Long.valueOf(String.valueOf(claims.get("tokenVersion"))))) {
                writeUnauthorized(response, "账号状态或登录信息已变更，请重新登录");
                return false;
            }
            var org=organizations.selectById(admin.getOrgId());
            if(!"SUPER".equals(admin.getRoleCode())&&(org==null||!Integer.valueOf(1).equals(org.getStatus()))){writeUnauthorized(response,"所属组织已停用");return false;}
            AdminContext.set(new AdminContext.CurrentAdmin(adminId, admin.getUsername(), admin.getRoleCode(),admin.getOrgId()));
            if (admin.getMustChangePassword() && !uri.equals("/admin/current") && !uri.equals("/admin/password") && !uri.equals("/auth/logout")) {
                AdminContext.clear();
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(objectMapper.writeValueAsString(R.fail(40002, "请先修改临时密码")));
                return false;
            }
        } catch (RuntimeException e) {
            writeUnauthorized(response, "登录信息无效，请重新登录");
            return false;
        }

        if ((uri.startsWith("/super/") || operationsEndpoint) && !AdminContext.get().isSuper()) {
            AdminContext.clear();
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(R.fail(40002, "无权限访问")));
            return false;
        }
        if(!AdminContext.get().isSuper() && (uri.startsWith("/admin/activity/") || uri.startsWith("/checkin/qrcode/"))) {
            Object vars=request.getAttribute(org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            if(vars instanceof java.util.Map<?,?> values){
                Object id=values.containsKey("id")?values.get("id"):values.get("activityId");
                if(id!=null){
                    var activity=activities.selectById(Long.valueOf(String.valueOf(id)));
                    if(activity==null||!AdminContext.get().getOrgId().equals(activity.getOrgId())){
                        AdminContext.clear();response.setStatus(403);response.setContentType("application/json;charset=UTF-8");response.getWriter().write(objectMapper.writeValueAsString(R.fail(40002,"无权访问该组织的活动")));return false;
                    }
                }
            }
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
        if (request.getCookies() != null) for (var cookie : request.getCookies()) {
            if ("CAMPUS_SESSION".equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(40002, msg)));
    }
}
