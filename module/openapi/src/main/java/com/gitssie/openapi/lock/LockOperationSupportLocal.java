package com.gitssie.openapi.lock;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.vavr.Tuple2;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class LockOperationSupportLocal implements LockOperationSupport {
    private static final Logger LOG = LoggerFactory.getLogger(LockOperationSupport.class);
    private Cache<Tuple2<String, Object>, ReentrantLock> lockCache;
    private ObjectPool<ReentrantLock> lockPool;

    public LockOperationSupportLocal() {
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(Integer.MAX_VALUE);
        this.lockPool = new GenericObjectPool(new BasePooledObjectFactory<ReentrantLock>() {
            @Override
            public ReentrantLock create() {
                return new ReentrantLock();
            }

            @Override
            public PooledObject wrap(ReentrantLock obj) {
                return new DefaultPooledObject(obj);
            }
        }, poolConfig);
        this.lockCache = CacheBuilder.newBuilder()
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .removalListener((e) -> {
                    try {
                        ReentrantLock lock = (ReentrantLock) e.getValue();
                        if (!lock.isLocked()) {
                            this.lockPool.returnObject(lock);
                        } else {
                            LOG.error("ReentrantLock may be leak,lock:{},hold count:{}", lock, lock.getHoldCount());
                        }
                    } catch (Exception e1) {
                        LOG.error("lock object is returned cause", e1);
                    }
                }).build();
    }

    @Override
    public ReentrantLock obtainLock(String lockName, Object lockId) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("obtainLock lockName:{},lockId:{}", lockName, lockId);
        }
        Tuple2<String, Object> key = new Tuple2(lockName, lockId);
        final ReentrantLock lock;
        try {
            lock = lockCache.get(key, () -> this.lockPool.borrowObject());
            if (LOG.isTraceEnabled()) {
                LOG.trace("success obtainLock lockName:{},lockId:{}", lockName, lockId);
            }
        } catch (ExecutionException e) {
            LOG.error("obtainLock lockName:{},lockId:{},error:", lockName, lockId, e);
            throw new RuntimeException(e);
        }
        return lock;
    }
}
