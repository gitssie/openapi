package com.gitssie.openapi.converter;

import org.springframework.core.convert.converter.Converter;

public enum NumberToBooleanConverter implements Converter<Number, Boolean> {
    INSTANCE;

    @Override
    public Boolean convert(Number number) {
        return number.intValue() != 0;
    }
}