package com.campus.activity.security;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
class IdentityVerifierTest {
 private static final String SECRET="test-only-identity-secret-at-least-32-bytes";
 private IdentityVerifier verifier(){var v=new IdentityVerifier();ReflectionTestUtils.setField(v,"secret",SECRET);ReflectionTestUtils.setField(v,"issuer","urn:test");ReflectionTestUtils.setField(v,"audience","campus-attendance");return v;}
 private String proof(String phone,boolean verified,long expires){return Jwts.builder().issuer("urn:test").subject("student-1").audience().add("campus-attendance").and().claim("phone_number",phone).claim("phone_number_verified",verified).expiration(new Date(expires)).signWith(Keys.hmacShaKeyFor(SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8))).compact();}
 @Test void verifiedOwnershipMatchesRequestedPhone(){assertDoesNotThrow(()->verifier().verify("STRICT","13912345678",proof("13912345678",true,System.currentTimeMillis()+60000)));}
 @Test void anotherPhoneCannotReuseProof(){assertThrows(com.campus.activity.common.BusinessException.class,()->verifier().verify("STRICT","13900000000",proof("13912345678",true,System.currentTimeMillis()+60000)));}
 @Test void expiredProofFailsClosed(){assertThrows(com.campus.activity.common.BusinessException.class,()->verifier().verify("STRICT","13912345678",proof("13912345678",true,System.currentTimeMillis()-60000)));}
 @Test void unverifiedPhoneClaimDoesNotCountAsOwnership(){assertThrows(com.campus.activity.common.BusinessException.class,()->verifier().verify("STRICT","13912345678",proof("13912345678",false,System.currentTimeMillis()+60000)));}
 @Test void malformedProofFailsClosed(){assertThrows(com.campus.activity.common.BusinessException.class,()->verifier().verify("IDENTITY_REQUIRED","13912345678","forged"));}
}
