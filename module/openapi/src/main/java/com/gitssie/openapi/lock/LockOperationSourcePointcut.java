package com.gitssie.openapi.lock;

import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class LockOperationSourcePointcut extends StaticMethodMatcherPointcut implements Serializable {
    private AnnotationLockOperationSource lockOperationSource;

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        if (method.getDeclaringClass() == Object.class) {
            return false;
        }
        // Don't allow non-public methods, as configured.
        if (!Modifier.isPublic(method.getModifiers())) {
            return false;
        }
        AnnotationLockOperationSource cas = getLockOperationSource();
        return (cas != null && !CollectionUtils.isEmpty(cas.getCacheOperations(method, targetClass)));
    }

    public AnnotationLockOperationSource getLockOperationSource() {
        return lockOperationSource;
    }

    public void setLockOperationSource(AnnotationLockOperationSource lockOperationSource) {
        this.lockOperationSource = lockOperationSource;
    }
}
