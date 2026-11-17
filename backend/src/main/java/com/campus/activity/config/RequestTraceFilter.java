package com.campus.activity.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;

@Component
@org.springframework.core.annotation.Order(-100)
public class RequestTraceFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = UUID.randomUUID().toString();
        MDC.put("requestId", id);
        response.setHeader("X-Request-ID", id);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("X-Frame-Options", "DENY");
        if (!request.getRequestURI().startsWith("/files/")) response.setHeader("Cache-Control", "no-store");
        var wrapped=new org.springframework.web.util.ContentCachingRequestWrapper(request,1024*1024){@Override protected void handleContentOverflow(int limit){throw new IllegalArgumentException("JSON请求过大");}};
        try { chain.doFilter(request.getContentType()!=null&&request.getContentType().contains("application/json")?wrapped:request, response); } finally { MDC.remove("requestId"); }
    }
}
