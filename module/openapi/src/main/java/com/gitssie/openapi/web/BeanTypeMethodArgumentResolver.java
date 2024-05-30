package com.gitssie.openapi.web;

import com.gitssie.openapi.service.Provider;
import com.gitssie.openapi.web.annotation.ActionMethod;
import io.ebean.plugin.BeanType;
import io.vavr.control.Either;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.ServletException;
import java.util.Map;

public class BeanTypeMethodArgumentResolver implements HandlerMethodArgumentResolver {
    private Provider provider;

    public BeanTypeMethodArgumentResolver(Provider provider) {
        this.provider = provider;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return BeanType.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {
        Map<String, String> uriTemplateVars = (Map<String, String>) request.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

        String apiKey = uriTemplateVars.get(ActionMethod.API_KAY);
        if (StringUtils.isEmpty(apiKey)) {
            handleMissingValue(parameter.getParameterName(), parameter);
        }
        Either<String, BeanType<Object>> desc = provider.getBeanTypeIfPresent(apiKey);
        if (desc.isLeft()) {
            handleMissingValue(parameter.getParameterName(), parameter);
        }
        return desc.get();
    }

    protected void handleMissingValue(String name, MethodParameter parameter) throws ServletException {
        throw new ServletRequestBindingException("Missing argument '" + name +
                "' for method parameter of type " + parameter.getNestedParameterType().getSimpleName());
    }
}
