package com.gitssie.openapi.validator;

public interface ValidatorImpl{
        String validate(final String field, final Object value, final Object expected);
    }