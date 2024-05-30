package com.gitssie.openapi.ebean;

import com.gitssie.openapi.models.auth.SecurityUser;
import io.ebean.config.CurrentUserProvider;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Component;

@Component
public class DefaultCurrentUserProvider implements CurrentUserProvider {

    @Autowired
    private ObjectProvider<UserDetailsService> userDetailsService;

    @Override
    public Object currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken || !authentication.isAuthenticated()) {
            return null;
        }

        if (authentication instanceof OAuth2Authentication) {
            authentication = ((OAuth2Authentication) authentication).getUserAuthentication();
        }

        if (authentication != null && authentication instanceof UsernamePasswordAuthenticationToken) {
            Object user = authentication.getPrincipal();
            if (ObjectUtils.isEmpty(user)) {
                return null;
            } else if (user instanceof SecurityUser) {
                return ((SecurityUser) user).getUser();
            } else if (user instanceof String) {
                SecurityUser securityUser = (SecurityUser) userDetailsService.getIfAvailable().loadUserByUsername((String) user);
                if (securityUser == null) {
                    return null;
                } else {
                    return securityUser.getUser();
                }
            } else {
                return null;
            }
        }

        return null;
    }
}
