package com.campus.events.checkin;

import com.campus.events.common.ApiException;
import com.campus.events.common.ErrorCode;
import com.campus.events.config.AppProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Component
public class ChallengeTokens {

    private final byte[] secret;

    public ChallengeTokens(AppProperties properties) {
        String value = properties.checkinSecret() == null ? "" : properties.checkinSecret();
        if (value.length() < 16) {
            throw new IllegalStateException("app.checkin-secret 至少需要 16 个字符");
        }
        this.secret = value.getBytes(StandardCharsets.UTF_8);
    }

    public String issue(String sessionId, String activityId, int version, Instant now) {
        long expiresAt = ((now.getEpochSecond() / 30) + 1) * 30;
        String payload = sessionId + "|" + activityId + "|" + version + "|" + expiresAt;
        return encode(payload) + "." + encode(hmac(payload));
    }

    public Decoded verify(String token, Instant now) {
        if (token == null || token.isBlank() || !token.contains(".")) {
            throw new ApiException(ErrorCode.CHECKIN_TOKEN_INVALID);
        }
        int split = token.lastIndexOf('.');
        String payloadPart = token.substring(0, split);
        String signaturePart = token.substring(split + 1);
        String payload;
        byte[] signature;
        try {
            payload = new String(Base64.getUrlDecoder().decode(payloadPart), StandardCharsets.UTF_8);
            signature = Base64.getUrlDecoder().decode(signaturePart);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.CHECKIN_TOKEN_INVALID);
        }
        if (!MessageDigest.isEqual(signature, hmac(payload))) {
            throw new ApiException(ErrorCode.CHECKIN_TOKEN_INVALID);
        }
        String[] parts = payload.split("\\|");
        if (parts.length != 4) {
            throw new ApiException(ErrorCode.CHECKIN_TOKEN_INVALID);
        }
        long expiresAt;
        int version;
        try {
            version = Integer.parseInt(parts[2]);
            expiresAt = Long.parseLong(parts[3]);
        } catch (NumberFormatException exception) {
            throw new ApiException(ErrorCode.CHECKIN_TOKEN_INVALID);
        }
        if (expiresAt <= now.getEpochSecond()) {
            throw new ApiException(ErrorCode.CHECKIN_TOKEN_EXPIRED);
        }
        return new Decoded(parts[0], parts[1], version, Instant.ofEpochSecond(expiresAt));
    }

    private byte[] hmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("无法签发签到码", exception);
        }
    }

    private static String encode(String value) {
        return encode(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    public record Decoded(String sessionId, String activityId, int version, Instant expiresAt) {
    }
}
