package com.campus.activity.security;
import com.campus.activity.common.BusinessException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
/** Accept only phone ownership assertions signed by a configured trusted identity gateway. */
@Component
public class IdentityVerifier {
    @Value("${CAMPUS_IDENTITY_SECRET:}") private String secret;
    @Value("${CAMPUS_IDENTITY_ISSUER:}") private String issuer;
    @Value("${CAMPUS_IDENTITY_AUDIENCE:campus-attendance}") private String audience;
    @Value("${CAMPUS_IDENTITY_AUTHORIZE_URL:}") private String authorizeUrl;
    public java.util.Map<String,Object> config(){boolean enabled=secret.length()>=32&&!issuer.isBlank()&&authorizeUrl.startsWith("https://");return java.util.Map.of("enabled",enabled,"authorizeUrl",enabled?authorizeUrl:"");}
    public void verify(String policy,String phone,String proof){
        if(!java.util.Set.of("STRICT","IDENTITY_REQUIRED").contains(policy))return;
        if(secret.length()<32||issuer.isBlank())throw new BusinessException("尚未配置校园身份验证，请联系活动管理员");
        if(proof==null||proof.isBlank())throw new BusinessException(40002,"请先完成校园身份验证");
        try{var claims=Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).requireIssuer(issuer).build().parseSignedClaims(proof).getPayload();
            if(claims.getExpiration()==null||claims.getSubject()==null||claims.getAudience()==null||!claims.getAudience().contains(audience)||!Boolean.TRUE.equals(claims.get("phone_number_verified"))||!java.util.Objects.equals(phone,claims.get("phone_number")))throw new IllegalArgumentException();
        }catch(Exception e){throw new BusinessException(40002,"校园身份凭据无效或已过期，请重新验证");}
    }
}
