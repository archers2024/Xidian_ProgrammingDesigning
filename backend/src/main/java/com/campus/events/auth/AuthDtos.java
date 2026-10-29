package com.campus.events.auth;

import java.time.OffsetDateTime;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(String username, String password) {
    }

    public record AdminUser(String id, String username, String displayName) {
    }

    public record LoginResult(AdminUser user, OffsetDateTime sessionExpiresAt) {
    }

    public record CsrfTokenView(String token, String headerName) {
    }
}
