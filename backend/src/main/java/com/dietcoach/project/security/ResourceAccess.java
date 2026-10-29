package com.dietcoach.project.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Checks the JWT user identity at service boundaries, before reading or mutating private data. */
@Component
public class ResourceAccess {
    public Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new AuthenticationCredentialsNotFoundException("로그인이 필요합니다.");
        }
        return userId;
    }

    public void requireOwner(Long ownerId) {
        if (!currentUserId().equals(ownerId)) {
            throw new AccessDeniedException("해당 데이터에 접근할 권한이 없습니다.");
        }
    }
}
