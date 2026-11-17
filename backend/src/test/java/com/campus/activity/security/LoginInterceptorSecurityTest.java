package com.campus.activity.security;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class LoginInterceptorSecurityTest {
 @AfterEach void cleanup(){AdminContext.clear();}
 private LoginInterceptor interceptor(){return new LoginInterceptor(mock(JwtUtil.class),mock(com.campus.activity.module.system.mapper.AdminMapper.class),mock(com.campus.activity.module.system.mapper.OrganizationMapper.class),mock(com.campus.activity.module.activity.mapper.ActivityMapper.class));}
 @Test void actuatorTemplateCannotHideProtectedMetrics()throws Exception{var request=new MockHttpServletRequest("GET","/actuator/prometheus");request.setAttribute(org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,"/actuator/{*path}");var response=new MockHttpServletResponse();assertFalse(interceptor().preHandle(request,response,new Object()));assertEquals(401,response.getStatus());}
 @Test void queryTokenIsNeverAuthentication()throws Exception{var request=new MockHttpServletRequest("GET","/admin/current");request.setParameter("token","not-accepted");var response=new MockHttpServletResponse();assertFalse(interceptor().preHandle(request,response,new Object()));assertEquals(401,response.getStatus());}
 @Test void actuatorSecurityRunsBeforeItsSeparateHandlerMapping()throws Exception{
  var filter=new com.campus.activity.config.OperationsSecurityFilter(interceptor());var chain=mock(jakarta.servlet.FilterChain.class);var response=new MockHttpServletResponse();
  filter.doFilter(new MockHttpServletRequest("GET","/actuator/prometheus"),response,chain);assertEquals(401,response.getStatus());verifyNoInteractions(chain);
 }
 @Test void publicHealthRemainsAccessible()throws Exception{assertTrue(interceptor().preHandle(new MockHttpServletRequest("GET","/actuator/health/readiness"),new MockHttpServletResponse(),new Object()));}
 @Test void nullOrganizationNeverMeansPlatformScope(){AdminContext.set(new AdminContext.CurrentAdmin(1L,"user","ADMIN",null));assertThrows(com.campus.activity.common.BusinessException.class,()->new OrganizationAccess(mock(com.campus.activity.module.system.mapper.OrganizationMapper.class)).queryScope());}
}
