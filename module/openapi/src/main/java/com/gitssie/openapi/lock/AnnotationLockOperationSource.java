package com.gitssie.openapi.lock;

import com.google.common.collect.Lists;
import org.springframework.cache.interceptor.AbstractFallbackCacheOperationSource;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.cache.interceptor.CacheableOperation;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

public class AnnotationLockOperationSource extends AbstractFallbackCacheOperationSource implements Serializable {
    @Override
    protected Collection<CacheOperation> findCacheOperations(Class<?> clazz) {
        return null;
    }

    @Override
    protected Collection<CacheOperation> findCacheOperations(Method method) {
        Lock lock = method.getAnnotation(Lock.class);
        if (lock == null) {
            return Collections.EMPTY_LIST;
        }
        CacheableOperation.Builder builder = new CacheableOperation.Builder();
        builder.setName(method.toString());
        if (lock.value().length > 0) {
            builder.setName(lock.value()[0]);
        } else if (lock.lockNames().length > 0) {
            builder.setName(lock.lockNames()[0]);
        }
        builder.setKey(lock.key());
        builder.setSync(lock.sync());

        return Lists.newArrayList(builder.build());
    }
}
