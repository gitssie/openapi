package com.gitssie.openapi.auth;

import com.gitssie.openapi.models.auth.RoleGrantedAuthority;
import com.gitssie.openapi.models.auth.VerbAttribute;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.FilterInvocation;

import java.util.Collection;

public class URLVerbVoter implements AccessDecisionVoter<FilterInvocation> {
    @Override
    public boolean supports(ConfigAttribute attribute) {
        return attribute != null && attribute instanceof VerbAttribute;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return FilterInvocation.class.isAssignableFrom(clazz);
    }

    @Override
    public int vote(Authentication authentication, FilterInvocation object, Collection<ConfigAttribute> attributes) {
        if (authentication == null) {
            return ACCESS_DENIED;
        }
        int result = ACCESS_ABSTAIN;
        Collection<? extends GrantedAuthority> authorities = extractAuthorities(authentication);
        for (ConfigAttribute attribute : attributes) {
            if (this.supports(attribute)) {
                result = doVote((VerbAttribute) attribute, authorities);
                if (result == ACCESS_GRANTED) {
                    break;
                }
            }
        }
        return result;
    }

    public int doVote(VerbAttribute attribute, Collection<? extends GrantedAuthority> authorities) {
        int result = ACCESS_DENIED;
        for (GrantedAuthority authority : authorities) {
            if (!(authority instanceof RoleGrantedAuthority)) {
                continue;
            }
            RoleGrantedAuthority roleGrantedAuthority = (RoleGrantedAuthority) authority;
            if (roleGrantedAuthority.match(attribute)) {
//                SecurityContextHolder.getContext(); @TODO 如何把匹配的角色传递到业务端去
                return ACCESS_GRANTED;
            }
        }
        return result;
    }

    Collection<? extends GrantedAuthority> extractAuthorities(Authentication authentication) {
        return authentication.getAuthorities();
    }

}
