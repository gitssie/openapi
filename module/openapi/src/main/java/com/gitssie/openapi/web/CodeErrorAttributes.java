package com.gitssie.openapi.web;

import com.gitssie.openapi.data.Code;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.validation.ObjectError;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.Map;

/**
 * @author: Awesome
 * @create: 2024-04-26 17:49
 */
public class CodeErrorAttributes extends DefaultErrorAttributes {
    private final EitherReturnValueHandler handler;

    public CodeErrorAttributes(EitherReturnValueHandler handler) {
        this.handler = handler;
    }

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest, options);
        int status = (int) errorAttributes.get("status");
        String error = (String) errorAttributes.get("error");
        String message = (String) errorAttributes.get("message");
        List<ObjectError> errors = (List<ObjectError>) errorAttributes.get("errors");

        Code code = new Code(status, error, message);
        Map<String, Object> node = handler.newResult();
        handler.handleCode(code, node);
        handler.handleErrors(errors, node);
        return node;
    }


}
