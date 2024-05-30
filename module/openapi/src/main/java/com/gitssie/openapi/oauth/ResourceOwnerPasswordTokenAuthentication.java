package com.gitssie.openapi.oauth;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;

/**
 * @see org.springframework.security.oauth2.provider.password.ResourceOwnerPasswordTokenGranter
 */
public class ResourceOwnerPasswordTokenAuthentication implements AuthenticationManager {

    private AuthenticationManager authenticationManager;
    private ApplicationEventPublisher eventPublisher;

    public ResourceOwnerPasswordTokenAuthentication(AuthenticationManager authenticationManager, ApplicationEventPublisher eventPublisher) {
        this.authenticationManager = authenticationManager;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Authentication auth = authenticationManager.authenticate(authentication);
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(auth.getPrincipal(), null, auth.getAuthorities());
        if (auth.isAuthenticated() && eventPublisher != null) {
            this.eventPublisher.publishEvent(new InteractiveAuthenticationSuccessEvent(auth, this.getClass()));
        }
        /*
        if (false) {
            OAuth2Exception ex = OAuth2Exception.create("200", "测试错误");
            ex.addAdditionalInformation("code","401"); //401:用户未绑定  402:用户未注册
            throw ex;
        }*/
        return authenticationToken;
    }
}
