package com.gitssie.openapi.oauth;

import com.gitssie.openapi.data.Code;
import org.springframework.security.core.AuthenticationException;

public class CodeAuthenticationException extends AuthenticationException {
    private Code code;
    public CodeAuthenticationException(Code code) {
        super(code.getMessage());
        this.code = code;
    }

    public Code getCode() {
        return code;
    }
}
