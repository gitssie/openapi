package com.gitssie.openapi.web.annotation;


import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.Mapping;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface Action {
    @AliasFor("apiKey")
    String value() default "";

    @AliasFor("value")
    String apiKey() default "";

    String funcName() default "";

    ActionMethod method() default ActionMethod.DELETE;
}
