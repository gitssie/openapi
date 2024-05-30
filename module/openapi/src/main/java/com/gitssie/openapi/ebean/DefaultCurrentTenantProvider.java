package com.gitssie.openapi.ebean;

import com.gitssie.openapi.models.auth.InternalUser;
import com.gitssie.openapi.models.auth.SecurityUser;
import io.ebean.config.CurrentTenantProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Component;

@Component
public class DefaultCurrentTenantProvider implements CurrentTenantProvider {
    @Override
    public Object currentId() {
        Long tenantId = null;
        Authentication token = SecurityContextHolder.getContext().getAuthentication();
        if (token != null) {
            Object user = token.getPrincipal();
            if (user instanceof InternalUser) {
                Long tenantOrigin = ((InternalUser) user).getTenantId();
                if (tenantOrigin != null) {
                    tenantId = tenantOrigin;
                }
            }
        }
        return tenantId;
    }
}
