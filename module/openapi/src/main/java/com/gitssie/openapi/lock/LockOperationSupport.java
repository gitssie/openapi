package com.gitssie.openapi.lock;

import java.util.concurrent.locks.Lock;

public interface LockOperationSupport {
    Lock obtainLock(String lockName, Object lockId);
}
