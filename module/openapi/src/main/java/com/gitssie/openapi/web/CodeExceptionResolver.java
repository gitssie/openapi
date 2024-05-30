package com.gitssie.openapi.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitssie.openapi.data.Code;
import com.gitssie.openapi.data.CodeException;
import io.vavr.control.Option;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.MessageSource;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class CodeExceptionResolver extends EitherReturnValueHandler implements HandlerExceptionResolver {
    protected final Log logger = LogFactory.getLog(AbstractHandlerExceptionResolver.class);

    public CodeExceptionResolver(ObjectMapper objectMapper, MessageSource messageSource, Tracer tracer) {
        super(objectMapper, null, messageSource, tracer);
    }

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object arg, Exception ex) {
        try {
            Code code = handlerException(response, ex);
            Map<String, Object> node = newResult();
            handleCode(code, node);
            writeReturnValue(node, response);
            return new ModelAndView();
        } catch (Exception handlerEx) {
            if (logger.isWarnEnabled()) {
                logger.warn("Failure while trying to resolve exception [" + ex.getClass().getName() + "]", handlerEx);
            }
            return new ModelAndView();
        }
    }

    private Code handlerException(HttpServletResponse response, Exception ex) {
        Code code = Code.INTERNAL;
        int status = 500;
        boolean log = false;
        if (ex instanceof AccessDeniedException) {
            code = Code.PERMISSION_DENIED;
            status = HttpServletResponse.SC_FORBIDDEN;
        } else if (ex instanceof CodeException) {
            code = Code.INTERNAL.withMessage(ex.getMessage());
        } else if (ex instanceof HttpMessageConversionException) {
            status = HttpServletResponse.SC_BAD_REQUEST;
            code = Code.INVALID_ARGUMENT;
        } else if (ex instanceof HttpRequestMethodNotSupportedException) {
            status = HttpServletResponse.SC_METHOD_NOT_ALLOWED;
            code = Code.INVALID_ARGUMENT.withMessage(ex.getMessage());
        }else if (ex instanceof HttpMediaTypeException) {
            status = HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE;
            code = Code.INVALID_ARGUMENT.withMessage(ex.getMessage());
        } else if (ex instanceof MissingRequestValueException) {
            status = HttpServletResponse.SC_BAD_REQUEST;
            code = Code.INVALID_ARGUMENT.withMessage(ex.getMessage());
        } else if (ex instanceof BindException) {
            status = HttpServletResponse.SC_BAD_REQUEST;
            BindingResult result = ((BindException) ex).getBindingResult();
            code = new Code(400, getBindingResultErrorMessage(result));
            code = code.withErrors(Option.of(result));
        } else if (ex instanceof OAuth2Exception) {
            OAuth2Exception oAuth2Exception = (OAuth2Exception) ex;
            code = new Code(Code.PERMISSION_DENIED.code, oAuth2Exception.getOAuth2ErrorCode(), ex.getMessage());
            code = code.withErrors(Option.of(oAuth2Exception.getAdditionalInformation()));
        } else {
            log = true;
        }

        if (log && logger.isErrorEnabled()) {
            logger.error("Failed to complete request:" + ex, ex);
        }

        response.setStatus(status);
        return code;
    }

    private String getBindingResultErrorMessage(BindingResult result) {
        return "Validation failed for object='" + result.getObjectName() + "'. " + "Error count: " + result.getErrorCount();
    }

}
