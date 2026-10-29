package com.campus.events.auth;

import java.io.Serializable;

public record AdminPrincipal(String id, String username, String displayName) implements Serializable {
    private static final long serialVersionUID = 1L;
}
