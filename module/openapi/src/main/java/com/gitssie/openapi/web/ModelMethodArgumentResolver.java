package com.gitssie.openapi.web;

import com.gitssie.openapi.data.Code;
import com.gitssie.openapi.page.Model;
import com.gitssie.openapi.service.PageService;
import com.gitssie.openapi.web.annotation.Action;
import com.gitssie.openapi.web.annotation.ActionMethod;
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

public class ModelMethodArgumentResolver implements HandlerMethodArgumentResolver {
    private ModeArgumentResolver resolver;

    public ModelMethodArgumentResolver(ModeArgumentResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return Model.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        Either<Code, Model> model = resolver.getModel(parameter, webRequest);
        if (model.isLeft()) {
            handleMissingValue(parameter.getParameterName(), parameter);
        }
        return model.get();
    }

    protected void handleMissingValue(String name, MethodParameter parameter) throws ServletException {
        throw new ServletRequestBindingException("Missing argument '" + name +
                "' for method parameter of type " + parameter.getNestedParameterType().getSimpleName());
    }
}
