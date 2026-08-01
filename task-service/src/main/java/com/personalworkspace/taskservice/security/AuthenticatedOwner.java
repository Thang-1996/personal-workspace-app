package com.personalworkspace.taskservice.security;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Chuyển identity đã được Resource Server xác thực thành khóa owner của domain.
 * Header do client gửi không được dùng vì có thể giả mạo ownership.
 */
public final class AuthenticatedOwner {

    private AuthenticatedOwner() {}

    public static UUID from(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
