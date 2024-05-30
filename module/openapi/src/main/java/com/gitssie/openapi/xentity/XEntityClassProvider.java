package com.gitssie.openapi.xentity;

import net.bytebuddy.ByteBuddy;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class XEntityClassProvider {
    private ApplicationContext context;
    private Map<String, Class<?>> customClassPool = new ConcurrentHashMap<>();

    public XEntityClassProvider(ApplicationContext context) {
        this.context = context;
    }

    public Class<?> createClass(Class<?> parent, String className) {
        return createClassByName(parent, className);
    }

    protected synchronized Class<?> createClassByName(Class<?> parent, String className) {
        Class<?> clazz = customClassPool.get(className);
        if (clazz != null) {
            return clazz;
        } else {
            Class<?> newClazz = generateCustomEntityClass(context.getClassLoader(), parent, className);
            customClassPool.put(className, newClazz);
            return newClazz;
        }
    }

    protected Class<?> generateCustomEntityClass(ClassLoader parentClassLoader, Class<?> parent, String className) {
        return new ByteBuddy()
                .subclass(parent)
                .name(className)
                .make()
                .load(parentClassLoader)
                .getLoaded();
    }
}
