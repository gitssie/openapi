package com.gitssie.openapi.web.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;


@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Action(method = ActionMethod.EDIT)
public @interface Edit {
    @AliasFor(annotation = Action.class)
    String value() default "";

    @AliasFor(annotation = Action.class)
    String apiKey() default "";

    @AliasFor(annotation = Action.class)
    String funcName() default "";
}
