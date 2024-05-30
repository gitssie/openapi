package com.gitssie.openapi.web;

import com.gitssie.openapi.web.annotation.Action;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;


public class WebApiMvcRegistrations implements WebMvcRegistrations {

    @Override
    public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
        return new RequestMappingHandlerMapping() {
            @Override
            protected RequestCondition<?> getCustomMethodCondition(Method method) {
                Action action = AnnotatedElementUtils.findMergedAnnotation(method, Action.class);
                if (action != null) {
                    String apiKey = StringUtils.defaultIfEmpty(action.apiKey(), action.value());
                    if (StringUtils.isEmpty(apiKey)) {
                        return null;
                    }
                    return new ApiKeyRequestCondition(apiKey, action.funcName());
                }
                return null;
            }
        };
    }
}
