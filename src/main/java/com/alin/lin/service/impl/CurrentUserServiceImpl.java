package com.alin.lin.service.impl;

import com.alin.lin.config.PosSecurityProperties;
import com.alin.lin.service.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserServiceImpl implements CurrentUserService {
    private static final String LOCAL_USERNAME = "local-development";

    private final PosSecurityProperties securityProperties;

    public CurrentUserServiceImpl(PosSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public String username() {
        if (!securityProperties.isEnabled()) {
            return LOCAL_USERNAME;
        }
        Authentication authentication = currentAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("尚未登入或登入資訊已失效");
        }
        return authentication.getName();
    }

    @Override
    public boolean hasRole(String role) {
        if (!securityProperties.isEnabled()) {
            return true;
        }
        Authentication authentication = currentAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    @Override
    public boolean securityEnabled() {
        return securityProperties.isEnabled();
    }

    private Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
