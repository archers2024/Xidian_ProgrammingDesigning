package com.campus.activity.config;
import com.campus.activity.security.LoginInterceptor;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
/** Actuator uses its own HandlerMapping and does not inherit application MVC interceptors. */
@Component
@org.springframework.core.annotation.Order(-90)
public class OperationsSecurityFilter extends OncePerRequestFilter {
 private final LoginInterceptor security;
 public OperationsSecurityFilter(LoginInterceptor security){this.security=security;}
 @Override protected boolean shouldNotFilter(HttpServletRequest request){String path=request.getRequestURI().substring(request.getContextPath().length());return !(path.equals("/actuator")||path.startsWith("/actuator/"))||path.equals("/actuator/health")||path.startsWith("/actuator/health/");}
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
  try{if(security.preHandle(request,response,this))chain.doFilter(request,response);}catch(IOException|ServletException e){throw e;}catch(Exception e){throw new ServletException(e);}finally{com.campus.activity.security.AdminContext.clear();}
 }
}
