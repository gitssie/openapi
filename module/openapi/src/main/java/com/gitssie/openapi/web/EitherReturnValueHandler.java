package com.gitssie.openapi.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitssie.openapi.data.Code;
import com.gitssie.openapi.page.ModelConverter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.ebean.bean.EntityBean;
import io.vavr.Tuple;
import io.vavr.Value;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.TraceContext;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.MessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

public class EitherReturnValueHandler implements HandlerMethodReturnValueHandler {
    protected RequestResponseBodyMethodProcessor methodProcessor;
    protected MappingJackson2HttpMessageConverter converter;

    protected final ObjectProvider<ModelConverter> modelConverter;
    protected final MessageSource messageSource;
    protected final Tracer tracer;


    public EitherReturnValueHandler(ObjectMapper objectMapper, ObjectProvider<ModelConverter> modelConverter, MessageSource messageSource, Tracer tracer) {
        this.modelConverter = modelConverter;
        this.converter = new MappingJackson2HttpMessageConverter(objectMapper);
        this.methodProcessor = new RequestResponseBodyMethodProcessor(Lists.newArrayList(converter));
        this.messageSource = messageSource;
        this.tracer = tracer;
    }

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        Class<?> type = returnType.getParameterType();
        return Either.class.isAssignableFrom(type);
    }

    @Override
    public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
        Map<String, Object> json = toResult(returnValue);
        methodProcessor.handleReturnValue(json, returnType, mavContainer, webRequest);
    }

    protected Map<String, Object> eitherToResult(Either either) {
        Map<String, Object> json = newResult();
        if (either.isLeft()) {
            handleLeft(json, either.getLeft());
        } else {
            json.put("code", Code.OK.code);
            json.put("message", "success");
            handleData(json, either.get());
        }
        return json;
    }

    protected Map<String, Object> newResult() {
        Map<String, Object> json = Maps.newLinkedHashMap();
        Span span = tracer.currentSpan();
        if (span != null) {
            TraceContext context = span.context();
            if (context != null) {
                json.put("traceId", context.traceId());
            }
        }
        return json;
    }

    public Map<String, Object> toResult(Object returnValue) {
        Code code = Code.OK;
        if (returnValue instanceof Code) {
            return newCode((Code) returnValue);
        } else if (returnValue instanceof Either) {
            return eitherToResult((Either) returnValue);
        } else if (returnValue instanceof Option) {
            if (((Option<?>) returnValue).isDefined()) {
                Object value = ((Option<?>) returnValue).get();
                if (value instanceof Code) {
                    return newCode((Code) value);
                }
            }
        } else if (returnValue instanceof Optional) {
            if (((Optional<?>) returnValue).isPresent()) {
                Object value = ((Optional<?>) returnValue).get();
                if (value instanceof Code) {
                    return newCode((Code) value);
                }
            }
        }
        //success result
        Map<String, Object> json = newResult();
        json.put("code", code.code);
        json.put("message", "success");
        handleData(json, returnValue);
        return json;
    }

    protected Map<String, Object> newCode(Code code) {
        Map<String, Object> json = newResult();
        json.put("code", code.code);
        json.put("message", code.getMessage());
        return json;
    }

    protected void handOption() {
        return;
    }

    public void handleReturnValue(Object returnValue, HttpServletResponse response) throws IOException {
        Map<String, Object> json = toResult(returnValue);
        writeReturnValue(json, response);
    }

    public void writeReturnValue(Map<String, Object> json, HttpServletResponse response) throws IOException {
        converter.write(json, MediaType.APPLICATION_JSON, new ServletServerHttpResponse(response));
    }

    protected void handleLeft(Map<String, Object> json, Object left) {
        if (left instanceof Code) {
            handleCode((Code) left, json);
        } else if (left instanceof String) {
            json.put("code", Code.FAILED_PRECONDITION.code);
            json.put("message", left);
        } else {
            handleCode(Code.UNKNOWN, json);
        }
    }

    protected void handleCode(Code code, Map<String, Object> json) {
        json.put("code", code.code);
        json.put("message", code.getMessage());
        json.put("errCode", code.getErrCode());
        Value err = code.getErrors();
        if (err != null && !err.isEmpty()) {
            Object value = err.get();
            if (value instanceof Errors) {
                handleErrors((Errors) value, json);
            } else if (value instanceof Map) {
                json.putAll((Map) value);
            }
        }
    }

    protected void handleErrors(Errors errors, Map<String, Object> json) {
        if (!errors.hasErrors()) {
            return;
        }
        handleErrors(errors.getAllErrors(), json);
    }

    protected void handleErrors(List<ObjectError> errors, Map<String, Object> json) {
        if (errors == null) {
            return;
        }
        Map<String, Object> errMap = Maps.newHashMapWithExpectedSize(errors.size());
        json.put("errors", errMap);
        List<String> global = null;
        for (ObjectError err : errors) {
            String message = messageSource.getMessage(err.getCode(), err.getArguments(), err.getDefaultMessage(), Locale.getDefault());
            if (err instanceof FieldError) {
                FieldError ferr = (FieldError) err;
                if (StringUtils.isNotEmpty(ferr.getObjectName())) {
                    errMap.put(StringUtils.joinWith(".", ferr.getObjectName(), ferr.getField()), message);
                } else {
                    errMap.put(ferr.getField(), message);
                }
            } else {
                if (global == null) {
                    global = Lists.newLinkedList();
                    global.add(message);
                }
            }
        }
        if (global != null) {
            errMap.put("global", global);
        }
    }

    protected void handleTuple(Map<String, Object> json, Tuple data) {
        int i = 0;
        for (Object o : data.toSeq()) {
            handleData(json, o, i++);
        }
    }

    protected void handleData(Map<String, Object> json, Object data) {
        handleData(json, data, 0);
    }

    protected void handleData(Map<String, Object> json, Object data, int index) {
        if (json.containsKey("data")) {
            if (data instanceof Map) {
                ((Map<String, ?>) data).forEach((k, v) -> {
                    json.put(k, handleObject(v));
                });
            } else {
                json.put(String.valueOf(index), handleObject(data));
            }
        } else if (data instanceof Tuple) {
            handleTuple(json, (Tuple) data);
        } else if (data instanceof Slice) {
            Slice slice = (Slice) data;
            if (slice.getPageable().isPaged()) { //分页信息
                Map<String, Object> pageInfo = new LinkedHashMap<>();
                pageInfo.put("page", slice.getNumber());
                pageInfo.put("size", slice.getSize());
                pageInfo.put("isFirst", slice.isFirst());
                pageInfo.put("isLast", slice.isLast());
                pageInfo.put("hasNext", slice.hasNext());
                pageInfo.put("hasPrevious", slice.hasPrevious());

                if (slice instanceof Page) {
                    pageInfo.put("rowsNumber", ((Page<?>) slice).getTotalElements());
                }
                json.put("page", pageInfo);
            }
            json.put("data", handleCollection(slice.getContent()));
        } else {
            json.put("data", handleObject(data));
        }
    }

    protected List<Object> handleCollection(Collection content) {
        List list = new LinkedList<>();
        ModelConverter converter = null;
        for (Object o : content) {
            if (o instanceof EntityBean) {
                if (converter == null) {
                    converter = modelConverter.getIfAvailable();
                }
                list = converter.toJSON((Class<EntityBean>) o.getClass(), content);
                break;
            } else {
                list.add(handleObject(o));
            }
        }
        return list;
    }

    protected Map<String, Object> handleEntityBean(EntityBean bean) {
        return modelConverter.getIfAvailable().toJSON(bean);
    }

    protected Object handleObject(Object o) {
        if (o instanceof Optional || o instanceof Option) {
            return handleOptional(o);
        } else if (o instanceof EntityBean) {
            return handleEntityBean((EntityBean) o);
        } else if (o instanceof Collection) {
            return handleCollection((Collection) o);
        } else {
            return o;
        }
    }

    protected Object handleOptional(Object o) {
        if (o instanceof Optional) {
            if (((Optional<?>) o).isPresent()) {
                return handleObject(((Optional<?>) o).get());
            } else {
                return o;
            }
        } else if (o instanceof Option) {
            if (((Option<?>) o).isDefined()) {
                return handleObject(((Option<?>) o).get());
            } else {
                return Optional.empty();
            }
        } else {
            return o;
        }
    }
}
