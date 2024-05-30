package com.gitssie.openapi.page;

import com.fasterxml.jackson.annotation.JsonValue;
import io.vavr.Function2;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

public class LazyValue implements Function2<FetchContext, Object, Object>, Supplier<Object> {
    private Function2<FetchContext, Object, Object> callback;
    private Function<Object, Object> format;
    private AtomicBoolean formatEvaluate;
    private String name;
    private String originName;
    private Object value;

    public LazyValue(String name) {
        this.name = name;
    }

    public LazyValue(String name, String originName) {
        this.name = name;
        this.originName = originName;
    }

    @Override
    @JsonValue
    public Object get() {
        value = applyFormat(value);
        return value;
    }

    @Override
    public Object apply(FetchContext context, Object bean) {
        value = applyFormat(callback.apply(context, bean));
        return value;
    }

    private Object applyFormat(Object value) {
        boolean doFormat = formatEvaluate != null && formatEvaluate.compareAndSet(false, true);
        if (doFormat && format != null) {
            value = format.apply(value);
        }
        return value;
    }

    public boolean isEmpty() {
        return callback == null;
    }

    public void setCallback(Function2<FetchContext, Object, Object> callback) {
        this.callback = callback;
    }

    public void setValue(Object value) {
        this.value = applyFormat(value);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOriginName() {
        return originName;
    }

    public void setFormat(Function<Object, Object> format) {
        this.formatEvaluate = new AtomicBoolean(false);
        this.format = format;
    }
}
