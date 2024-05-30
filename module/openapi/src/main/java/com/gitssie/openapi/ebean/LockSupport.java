package com.gitssie.openapi.ebean;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.ebean.Database;
import io.ebean.Transaction;
import io.ebean.TransactionCallbackAdapter;
import io.ebeaninternal.server.deploy.BeanDescriptor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.persistence.EntityNotFoundException;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class LockSupport {
    private static final Logger LOG = LoggerFactory.getLogger(LockSupport.class);
    private static final String LOCK_KEY = "ReentrantLockKey";
    private final Database db;
    private Cache<LockKey,ReentrantLock> lockCache;
    private ObjectPool<ReentrantLock> lockPool;

    public LockSupport(Database db) {
        this.db = db;
        //可以通过设置对象池最大数量控制cache的数量
        this.lockPool = new GenericObjectPool(new BasePooledObjectFactory<ReentrantLock>(){
            @Override
            public ReentrantLock create(){
                return new ReentrantLock();
            }
            @Override
            public PooledObject wrap(ReentrantLock obj) {
                return new DefaultPooledObject(obj);
            }
        });
        this.lockCache = CacheBuilder.newBuilder()
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .removalListener((e) -> {
                    try {
                        this.lockPool.returnObject((ReentrantLock)e.getValue());
                    } catch (Exception e1) {
                        LOG.error("lock object is returned cause",e1);
                    }
                }).build();
    }

    private Object doLock(Object bean,Class<?> clazz,Object id,String ...props){
        final Transaction transaction = db.currentTransaction();
        if(transaction == null){
            throw new PersistenceException("Invalid state - there is needs an Active transaction");
        }
        final BeanDescriptor desc = (BeanDescriptor)db.pluginApi().beanType(clazz);
        final LockKey key = new LockKey(desc,id);
        try {
            final ReentrantLock lock = lockCache.get(key,() -> this.lockPool.borrowObject());
            if(lock.isHeldByCurrentThread()){
                return bean;
            }
            boolean locked = lock.tryLock(10,TimeUnit.SECONDS); //lock
            if(!locked){//maybe deadlocked
                throw new OptimisticLockException("Invalid state - got lock had failure");
            }
            transaction.putUserObject(LOCK_KEY,lock);

            String select = StringUtils.defaultIfEmpty(StringUtils.join(props,","),"*");
            Object dbBean = db.createQuery(clazz).select(select)
                    .setUseCache(false)
                    .forUpdate()
                    .setId(id)
                    .findOne();

            if (dbBean == null) {
                String msg = "Bean not found during lazy load or refresh." + " id[" + id + "] type[" + desc.type() + "]";
                throw new EntityNotFoundException(msg);
            }

            desc.resetManyProperties(dbBean);
            transaction.register(new LockCallable(lock));
            return dbBean;
        }catch (Exception e){
            lockCache.invalidate(key);
            throw new RuntimeException(e);
        }
    }

    public <T> T lock(T bean,String ...props){
        Object id = db.beanId(bean);
        doLock(bean,bean.getClass(),id,props);
        return bean;
    }

    public Optional<LockSupport> toOptional(){
        return Optional.of(this);
    }

    public static boolean isHeldByCurrentThread(Transaction transaction){
        ReentrantLock lock = (ReentrantLock)transaction.getUserObject(LOCK_KEY);
        return lock != null && lock.isHeldByCurrentThread();
    }

    private static class LockKey {
        private BeanDescriptor desc;
        private Object id;

        public LockKey(BeanDescriptor desc, Object id) {
            this.desc = desc;
            this.id = id;
        }

        @Override
        public int hashCode() {
            return this.desc.hashCode() * 37 * id.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if(obj == null || !(obj instanceof LockKey)){
                return false;
            }
            LockKey res = (LockKey)obj;
            return this.desc.equals(res.desc) && this.id.equals(res.id);
        }
    }

    private static class LockCallable extends TransactionCallbackAdapter{
        private final ReentrantLock lock;

        public LockCallable(ReentrantLock lock) {
            this.lock = lock;
        }

        @Override
        public void postCommit() {
            lock.unlock();
        }
        @Override
        public void postRollback() {
            lock.unlock();
        }
    }
}
