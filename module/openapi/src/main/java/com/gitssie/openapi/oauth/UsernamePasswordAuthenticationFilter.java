package com.gitssie.openapi.oauth;

import com.gitssie.openapi.data.Code;
import io.vavr.control.Option;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UsernamePasswordAuthenticationFilter extends org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter {
    private CaptchaStore captchaStore;

    public UsernamePasswordAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        if (captchaStore != null) {
            Option<Code> res = captchaStore.verifyCaptcha(request);
            if (res.isDefined()) {
                throw new CodeAuthenticationException(res.get());
            }
        }
        return super.attemptAuthentication(request, response);
    }

    public void setCaptchaStore(CaptchaStore captchaStore) {
        this.captchaStore = captchaStore;
    }
}
