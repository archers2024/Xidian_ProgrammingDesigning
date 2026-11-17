package com.campus.activity.common;
import com.campus.activity.security.AdminContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.context.request.*;
import org.springframework.web.util.ContentCachingRequestWrapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
@Aspect @Component @org.springframework.core.annotation.Order(0)
public class IdempotencyAspect {
    private final JdbcTemplate jdbc;private final ObjectMapper json;private final TransactionTemplate tx;
    public IdempotencyAspect(JdbcTemplate jdbc,ObjectMapper json,PlatformTransactionManager manager){this.jdbc=jdbc;this.json=json;tx=new TransactionTemplate(manager);}
    private static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    @Around("@annotation(com.campus.activity.common.Idempotent)")
    public Object execute(ProceedingJoinPoint call)throws Throwable{
        var request=((ServletRequestAttributes)RequestContextHolder.currentRequestAttributes()).getRequest();
        String key=request.getHeader("Idempotency-Key");if(key==null)return call.proceed();
        if(!key.matches("[A-Za-z0-9_-]{8,128}"))throw new BusinessException("幂等键格式错误");
        if(!(request instanceof ContentCachingRequestWrapper wrapped))throw new IllegalStateException("Request cache unavailable");
        String scope=String.valueOf(AdminContext.adminId()==null?request.getRemoteAddr():AdminContext.adminId());
        String id=hash(scope+":"+request.getRequestURI()+":"+key),content=hash(new String(wrapped.getContentAsByteArray(),StandardCharsets.UTF_8));
        try{return tx.execute(status->{
            jdbc.update("INSERT INTO t_idempotency(request_key,content_hash,expires_at) VALUES (?,?,TIMESTAMPADD(DAY,1,NOW())) ON DUPLICATE KEY UPDATE request_key=VALUES(request_key)",id,content);
            var row=jdbc.queryForMap("SELECT content_hash,response_json FROM t_idempotency WHERE request_key=? FOR UPDATE",id);
            if(!content.equals(row.get("content_hash")))throw new BusinessException(40009,"幂等键已用于不同内容");
            try{
                if(row.get("response_json")!=null)return json.readValue(String.valueOf(row.get("response_json")),json.constructType(((MethodSignature)call.getSignature()).getMethod().getGenericReturnType()));
                Object result=call.proceed();
                if(result instanceof R<?> r&&r.getCode()==0)jdbc.update("UPDATE t_idempotency SET response_json=? WHERE request_key=?",json.writeValueAsString(result),id);
                else jdbc.update("DELETE FROM t_idempotency WHERE request_key=?",id);
                return result;
            }catch(RuntimeException e){throw e;}catch(Throwable e){throw new Wrapped(e);}
        });}catch(Wrapped e){throw e.getCause();}
    }
    private static class Wrapped extends RuntimeException{Wrapped(Throwable e){super(e);}}
}
