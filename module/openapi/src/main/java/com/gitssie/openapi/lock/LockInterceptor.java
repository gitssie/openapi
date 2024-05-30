package com.gitssie.openapi.lock;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.cache.interceptor.CacheableOperation;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;
import org.springframework.util.Assert;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.OptimisticLockException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockInterceptor implements MethodInterceptor, Serializable {
    private LockOperationExpressionEvaluator evaluator = new LockOperationExpressionEvaluator();
    private ObjectProvider<LockOperationSupport> lockSupportOpt;
    private LockOperationSupport lockSupport;
    private AnnotationLockOperationSource lockOperationSource;
    private int defaultLockTime = 10; //seconds

    @Nullable
    @Override
    public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Object target = invocation.getThis();
        Assert.state(target != null, "Target must not be null");

        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(target);
        Collection<CacheOperation> operations = lockOperationSource.getCacheOperations(method, targetClass);
        if (!operations.isEmpty()) {
            return executeLock(invocation, method, targetClass, (CacheableOperation) operations.iterator().next());
        }
        return invocation.proceed();
    }

    protected Object executeLock(MethodInvocation invocation, Method method, Class<?> targetClass, CacheableOperation operation) throws Throwable {
        AnnotatedElementKey methodKey = new AnnotatedElementKey(method, targetClass);
        EvaluationContext evaluationContext = evaluator.createEvaluationContext(method, invocation.getArguments(), invocation.getThis(), targetClass, invocation.getMethod(), null);
        Object lockId = evaluator.key(operation.getKey(), methodKey, evaluationContext);
        String lockName = operation.getName();
        if (lockSupport == null) {
            lockSupport = lockSupportOpt.getIfAvailable();
        }
        Lock lock = lockSupport.obtainLock(lockName, lockId);
        boolean acquired = false;
        try {
            //fast lock
            acquired = lock.tryLock();
            if (!acquired && operation.isSync()) {
                throw new OptimisticLockException("Lock Sync Fail Fast,lockName:" + lockName + ",lockId:" + lockId);
            } else if (!acquired) {
                //wait lock
                acquired = lock.tryLock(defaultLockTime, TimeUnit.SECONDS);
                if (!acquired) {//maybe deadlocked
                    throw new OptimisticLockException("Invalid state - got lock had failure,lockName:" + lockName + ",lockId:" + lockId);
                }
            }
            return invocation.proceed();
        } finally {
            if (acquired) {
                lock.unlock();
            }
        }
    }

    public void setLockSupportOpt(ObjectProvider<LockOperationSupport> lockSupportOpt) {
        this.lockSupportOpt = lockSupportOpt;
    }

    public void setLockSupport(LockOperationSupport lockSupport) {
        this.lockSupport = lockSupport;
    }

    public void setLockOperationSource(AnnotationLockOperationSource lockOperationSource) {
        this.lockOperationSource = lockOperationSource;
    }
}
