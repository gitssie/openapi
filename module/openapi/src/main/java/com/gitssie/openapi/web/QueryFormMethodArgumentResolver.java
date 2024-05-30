package com.gitssie.openapi.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gitssie.openapi.data.Code;
import com.gitssie.openapi.page.Model;
import com.gitssie.openapi.service.PageService;
import com.gitssie.openapi.web.annotation.Action;
import com.gitssie.openapi.web.annotation.ActionMethod;
import com.gitssie.openapi.web.query.PredicateField;
import com.gitssie.openapi.web.query.QueryForm;
import com.google.common.collect.Lists;
import io.vavr.control.Either;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.AbstractMessageConverterMethodArgumentResolver;

import javax.servlet.http.HttpServletRequest;
import java.util.Iterator;
import java.util.Map;

/**
 * @author: Awesome
 * @create: 2024-05-15 11:05
 */
public class QueryFormMethodArgumentResolver extends AbstractMessageConverterMethodArgumentResolver implements HandlerMethodArgumentResolver {
    private ObjectMapper objectMapper;
    private ModeArgumentResolver resolver;

    public QueryFormMethodArgumentResolver(ObjectMapper objectMapper,ModeArgumentResolver resolver) {
        super(Lists.newArrayList(new MappingJackson2HttpMessageConverter(objectMapper)));
        this.objectMapper = objectMapper;
        this.resolver = resolver;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(QueryForm.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
        Assert.state(servletRequest != null, "No HttpServletRequest");
        ServletServerHttpRequest inputMessage = new ServletServerHttpRequest(servletRequest);

        ObjectNode node = (ObjectNode) readWithMessageConverters(inputMessage, parameter, ObjectNode.class);
        QueryForm form;
        JsonNode query = node.get("query");
        if (query != null && query.isObject()) {
            JsonNode predicate = query.get("predicate");
            if (predicate != null && predicate.isObject()) {
                ObjectNode oQuery = (ObjectNode) query;
                oQuery.remove("predicate");
                form = objectMapper.treeToValue(node, QueryForm.class);
                Iterator<String> itr = predicate.fieldNames();
                String key;
                while (itr.hasNext()) {
                    key = itr.next();
                    form.addPredicate(new PredicateField(key, objectMapper.treeToValue(predicate.get(key), Object.class)));
                }
            } else {
                form = objectMapper.treeToValue(node, QueryForm.class);
            }
        } else {
            form = objectMapper.treeToValue(node, QueryForm.class);
        }


        if (binderFactory != null) {
            Either<Code, Model> queryModel = resolver.getQuery(parameter, webRequest);
            String name = "";
            WebDataBinder binder = binderFactory.createBinder(webRequest, form, name);
            if (queryModel.isRight()) {
                binder.addValidators(new ModelValidator(queryModel.get()));
            }
            if (form != null) {
                validateIfApplicable(binder, parameter);
                if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
                    throw new MethodArgumentNotValidException(parameter, binder.getBindingResult());
                }
            }
            if (mavContainer != null) {
                mavContainer.addAttribute(BindingResult.MODEL_KEY_PREFIX + name, binder.getBindingResult());
            }
        }

        return form;
    }
}
