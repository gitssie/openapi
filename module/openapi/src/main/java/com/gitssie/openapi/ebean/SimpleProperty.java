package com.gitssie.openapi.ebean;

import io.ebean.plugin.Property;

public class SimpleProperty implements Property {
    private String name;
    private Class<?> type;

    public SimpleProperty(String name) {
        this.name = name;
    }

    public SimpleProperty(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Class<?> type() {
        return type;
    }

    @Override
    public Object value(Object bean) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isMany() {
        return false;
    }
}
