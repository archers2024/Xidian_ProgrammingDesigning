package com.campus.activity.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具：签发与解析管理员会话令牌。
 */
@Component
public class JwtUtil {

    @Value("${campus.jwt.secret:}")
    private String secret;

    @Value("${campus.production:false}") private boolean production;

    @jakarta.annotation.PostConstruct
    public void initialize() {
        if(production&&(secret==null||secret.getBytes(StandardCharsets.UTF_8).length<32))throw new IllegalStateException("Production JWT secret must contain at least 32 bytes");
        if (secret == null || secret.isBlank()) secret = java.util.UUID.randomUUID().toString() + java.util.UUID.randomUUID();
    }

    @Value("${campus.jwt.expire-hours}")
    private long expireHours;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(Long adminId, String username, String roleCode, Long tokenVersion) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("adminId", adminId);
        claims.put("username", username);
        claims.put("roleCode", roleCode);
        claims.put("tokenVersion", tokenVersion);
        Date now = new Date();
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireHours * 3600_000L))
                .signWith(key())
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }
}
