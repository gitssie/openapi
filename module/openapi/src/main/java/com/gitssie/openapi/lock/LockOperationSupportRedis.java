package com.gitssie.openapi.lock;

import org.springframework.integration.redis.util.RedisLockRegistry;

import java.util.concurrent.locks.Lock;

public class LockOperationSupportRedis implements LockOperationSupport {
    private final RedisLockRegistry redisLockRegistry;

    public LockOperationSupportRedis(RedisLockRegistry redisLockRegistry) {
        this.redisLockRegistry = redisLockRegistry;
    }

    @Override
    public Lock obtainLock(String lockName, Object lockId) {
        String lockKey = String.format("%s:%s", lockName, lockId);
        return redisLockRegistry.obtain(lockKey);
    }
}
