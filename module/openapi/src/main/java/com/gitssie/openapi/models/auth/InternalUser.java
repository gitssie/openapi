package com.gitssie.openapi.models.auth;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

import java.io.Serializable;

public interface InternalUser extends Serializable {
    Long getId();
    String getPassword();

    String getUsername();

    boolean isEnabled();

    Long getTenantId();

    static Authentication anonymous(Long tenantId) {
        return new AnonymousAuthenticationToken("anonymous", new InternalTenantUser(tenantId), AuthorityUtils.createAuthorityList("ROLE_USER"));
    }
}
