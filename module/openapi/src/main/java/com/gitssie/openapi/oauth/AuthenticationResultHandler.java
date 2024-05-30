package com.gitssie.openapi.oauth;

import com.gitssie.openapi.models.auth.SecurityUser;
import com.gitssie.openapi.models.user.User;
import com.gitssie.openapi.web.EitherReturnValueHandler;
import io.vavr.control.Either;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class AuthenticationResultHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private EitherReturnValueHandler returnValueHandler;

    public AuthenticationResultHandler(EitherReturnValueHandler returnValueHandler) {
        this.returnValueHandler = returnValueHandler;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        if (exception instanceof CodeAuthenticationException) {
            returnValueHandler.handleReturnValue(((CodeAuthenticationException) exception).getCode(), response);
        } else {
            returnValueHandler.handleReturnValue(Either.left("用户名或密码错误"), response);
        }
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        returnValueHandler.handleReturnValue(Either.right(toMap(authentication)), response);
    }

    public Map<String, Object> toMap(Authentication authentication) {
        Map<String, Object> result = new LinkedHashMap<>();
        boolean notLogin = authentication == null || authentication.getPrincipal() == null || !(authentication.getPrincipal() instanceof SecurityUser);
        if (notLogin) {
            result.put("authenticated", false);
            return result;
        }
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        result.put("authorities", authentication.getAuthorities());
        result.put("authenticated", authentication.isAuthenticated());
        User user = securityUser.getUser();
        Map<String, Object> principal = new LinkedHashMap<>();
        principal.put("id", user.getId());
        principal.put("username", user.getUsername());
        principal.put("name", user.getName());
        principal.put("phone", user.getPhone());
        result.put("principal", principal);
        return result;
    }

    public Map<String, Object> toResult(Authentication authentication) {
        return returnValueHandler.toResult(Either.right(toMap(authentication)));
    }
}
