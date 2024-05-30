package com.gitssie.openapi.lock;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Lock {
    @AliasFor("lockNames")
    String[] value() default {};

    @AliasFor("value")
    String[] lockNames() default {};

    String key() default "";

    boolean sync() default false;
}
