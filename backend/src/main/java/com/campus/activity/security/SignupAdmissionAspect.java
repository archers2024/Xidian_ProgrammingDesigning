package com.campus.activity.security;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.context.request.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
/** Admission finishes before the idempotency/business transaction can hold a database connection. */
@Aspect @Component @Order(-20)
public class SignupAdmissionAspect {
 private final RateLimitStore rates;
 @Value("${campus.signup.ip-limit-per-minute}") private int ipLimit;
 public SignupAdmissionAspect(RateLimitStore rates){this.rates=rates;}
 @Before("execution(* com.campus.activity.module.signup.controller.SignupController.signup(..))")
 public void admit(JoinPoint call){
  var request=((ServletRequestAttributes)RequestContextHolder.currentRequestAttributes()).getRequest();
  rates.take("signup-ip:"+request.getRemoteAddr(),ipLimit,60);
  var body=(java.util.Map<?,?>)call.getArgs()[0];
  try{String identity=String.valueOf(body.get("guid"))+":"+String.valueOf(body.get("phone"));String key=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8)));rates.take("signup-subject:"+key,10,60);}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
 }
}
