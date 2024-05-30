package com.gitssie.openapi.oauth;

import org.springframework.http.HttpEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.error.DefaultOAuth2ExceptionRenderer;
import org.springframework.security.oauth2.provider.error.OAuth2AuthenticationEntryPoint;
import org.springframework.security.oauth2.provider.error.OAuth2ExceptionRenderer;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.context.request.ServletWebRequest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class StandardOAuth2ExceptionRenderer implements OAuth2ExceptionRenderer, AuthenticationEntryPoint {
    private OAuth2ExceptionRenderer exceptionRenderer = new DefaultOAuth2ExceptionRenderer();
    private OauthExceptionHandler exceptionHandler = new OauthExceptionHandler();
    private OAuth2AuthenticationEntryPoint entryPoint = new OAuth2AuthenticationEntryPoint();

    @Override
    public void handleHttpEntityResponse(HttpEntity<?> responseEntity, ServletWebRequest webRequest) throws Exception {
        if (responseEntity == null) {
            return;
        }
        Object body = responseEntity.getBody();
        if (body != null && body instanceof OAuth2Exception) {
            exceptionHandler.doResolveException(webRequest.getRequest(), webRequest.getResponse(), null, (OAuth2Exception) body);
            return;
        }
        exceptionRenderer.handleHttpEntityResponse(responseEntity, webRequest);
    }

    public OAuth2ExceptionRenderer getExceptionRenderer() {
        return exceptionRenderer;
    }

    public void setExceptionRenderer(OAuth2ExceptionRenderer exceptionRenderer) {
        this.exceptionRenderer = exceptionRenderer;
    }

    public OauthExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    public void setExceptionHandler(OauthExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        Throwable cause = authException.getCause();
        if (cause != null && cause instanceof OAuth2Exception) {
            exceptionHandler.doResolveException(request, response, null, (OAuth2Exception) cause);
        } else {
            entryPoint.commence(request, response, authException);
        }
    }
}
