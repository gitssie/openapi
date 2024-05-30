package com.gitssie.openapi.xentity;

import com.gitssie.openapi.models.xentity.EntityMapping;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import io.ebean.annotation.View;
import io.ebean.bean.DynamicEntity;
import io.ebean.config.CurrentTenantProvider;
import io.ebeaninternal.server.deploy.parse.tenant.XEntity;
import io.ebeaninternal.server.deploy.parse.tenant.XField;
import io.ebeaninternal.server.deploy.parse.tenant.annotation.XManyToOne;
import io.ebeaninternal.server.deploy.parse.tenant.annotation.XOneToMany;
import io.ebeaninternal.server.deploy.parse.tenant.annotation.XOneToOne;
import io.vavr.Lazy;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.persistence.*;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Component
public class XEntityManagerImpl implements XEntityManager {
    private final static String API_PREFIX = "api_";
    private int entityCacheTime = 120;//120 MINUTES
    private final ReentrantLock lock = new ReentrantLock();
    private final ConcurrentMap<Object, Cache<String, XEntityCache>> tenantEntityMap;
    private final XEntityClassProvider classProvider;
    private final ObjectProvider<XEntityService> entityService;
    private final Lazy<Class<?>> objectBeanClass;
    private CurrentTenantProvider tenantProvider;

    public XEntityManagerImpl(XEntityClassProvider classProvider,
                              ObjectProvider<XEntityService> entityService,
                              @Qualifier("objectBeanClass") Lazy<Class<?>> objectBeanClass) {
        this.tenantEntityMap = Maps.newConcurrentMap();
        this.classProvider = classProvider;
        this.entityService = entityService;
        this.objectBeanClass = objectBeanClass;
    }

    @Override
    public XEntityCache getEntity(String apiKey) {
        return loadEntityClass(tenantProvider.currentId(), apiKey);
    }

    @Override
    public XEntityCache getEntity(Class<?> beanClass) {
        Object tenantId = tenantProvider.currentId();
        if (isApiClass(beanClass)) {//case 1.自定义API对象，有专门的表进行API映射关系存储，支持关联关系
            String apiKey = getApiKey(tenantId, beanClass);
            return loadEntityClass(tenantId, apiKey);
        } else {//case 2.标准对象,不支持自定义属性
            return new XEntityCache(new XEntity(beanClass), 0L);
        }
    }

    @Override
    public Class<?> getBeanClass(String apiKey) {
        XEntityCache item = getEntity(apiKey);
        return item.getBeanType();
    }

    /**
     * 加载实体
     *
     * @param apiKey
     * @return
     */
    private XEntityCache loadEntityClass(Object tenantId, String apiKey) {
        Either<String, XEntityCache> entityEither = loadEntity(tenantId, apiKey);
        if (entityEither.isLeft()) {
            throw new IllegalStateException(entityEither.getLeft());
        }
        return entityEither.get();
    }

    private Either<String, XEntityCache> loadEntity(Object tenantId, String apiKey) {
        return loadEntity(tenantId, apiKey, () -> this.computeEntityByApiKey(apiKey));
    }

    @Override
    public Either<String, XEntityCache> getEntityIfPresent(String apiKey) {
        return loadEntity(tenantProvider.currentId(), apiKey, () -> this.computeEntityByApiKey(apiKey));
    }

    @Override
    public boolean isExistsEntity(String apiKey) {
        if (getFromCached(apiKey).isDefined()) {
            return true;
        }
        return entityService.getIfAvailable().getEntityLazy(apiKey).isRight();
    }

    private Either<String, XEntityCache> computeEntityByApiKey(String apiKey) {
        return entityService.getIfAvailable().getXEntity(apiKey);
    }

    @Override
    public XEntityCache loadXEntity(EntityMapping mapping) {
        Either<String, XEntityCache> entityE = entityService.getIfAvailable().getXEntity(mapping);
        if (entityE.isLeft()) {
            throw new IllegalStateException(entityE.getLeft());
        }
        return setLoader(tenantProvider.currentId(), entityE.get());
    }

    private void loadClass(Object tenantId, XEntityCache item) {
        doLock((e) -> resolveEntityBeanClass(tenantId, item.desc), "entity manager acquire load class lock is failure");
    }

    private void loadAssoc(Object tenantId, XEntityCache item) {
        doLock((e) -> resolveEntityFieldAssoc(item.desc), "entity manager acquire load assoc lock is failure");
    }

    private void doLock(Consumer<Boolean> callback, String failure) {
        boolean locked = false;
        try {
            locked = lock.tryLock(1, TimeUnit.MINUTES);
            if (!locked) {
                throw new IllegalStateException(failure);
            }
            callback.accept(true);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    @Override
    public void updateXEntityCache(Object tenantId, XEntityCache entity) {
        getEntityCache(tenantId).forEach(e -> {
            String apiKey = entity.getName().toLowerCase();
            e.put(apiKey, entity);
        });
    }

    /**
     * 解决Bean关联的Class
     *
     * @param entity
     */
    private void resolveEntityFieldAssoc(XEntity entity) {
        for (XField field : entity.getFields()) {
            if (field.has(XAssoc.class)) {
                XAssoc assoc = field.getAnnotation(XAssoc.class);
                Class<?> beanClass = getBeanClass(assoc.getApiKey());
                field.setType(beanClass);
                //设置关联元标注信息
                assocTypeToAnnotation(field, beanClass, assoc, assoc.getAssocType()).forEach(field::addAnnotation);
            }
        }
    }

    private boolean hasToOne(XField field) {
        return field.has(OneToOne.class) || field.has(ManyToOne.class);
    }

    private boolean hasToMany(XField field) {
        return field.has(ManyToMany.class);
    }

    private Option<Annotation> assocTypeToAnnotation(XField field, Class<?> beanClass, XAssoc assoc, XAssocType assocType) {
        switch (assocType) {
            case ORef:
                if (hasToOne(field)) {
                    return Option.none();
                }
                return Option.of(new XManyToOne(beanClass, new CascadeType[]{}));
            case DetailRef:
                if (hasToOne(field)) {
                    return Option.none();
                }
                return Option.of(new XManyToOne(beanClass, new CascadeType[]{CascadeType.PERSIST}));
            case ValRef:
                if (hasToOne(field)) {
                    return Option.none();
                }
                return Option.of(new XOneToOne(beanClass, new CascadeType[]{CascadeType.ALL}));
            case ListValRef:
                field.setType(List.class);
                field.setTargetType(beanClass);
                if (hasToMany(field)) {
                    return Option.none();
                }
                return Option.of(new XOneToMany(beanClass, new CascadeType[]{CascadeType.ALL}, FetchType.EAGER, assoc.getMappedBy()));
            default:
                throw new IllegalStateException("invalid assoc type " + assocType.name());
        }
    }

    private Option<Cache<String, XEntityCache>> getEntityCache(Object tenantId) {
        return Option.of(tenantEntityMap.get(tenantId));
    }

    /**
     * 先要获取到Lock才能调用该方法
     *
     * @param tenantId
     * @return
     */
    private Cache<String, XEntityCache> getEntityCacheLocked(Object tenantId) {
        return tenantEntityMap.computeIfAbsent(tenantId, (key) -> {
            return CacheBuilder.newBuilder().expireAfterAccess(entityCacheTime, TimeUnit.MINUTES).build();
        });
    }

    /**
     * 加载XEntity,生成动态class以及属性关联信息
     *
     * @param tenantId
     * @param apiKey
     * @param provider
     * @return
     */
    private Either<String, XEntityCache> loadEntity(Object tenantId, String apiKey, Supplier<Either<String, XEntityCache>> provider) {
        if (tenantId == null) {
            return Either.left("tenantId must not be null");
        }
        apiKey = apiKey.toLowerCase();
        Cache<String, XEntityCache> entityCache = getEntityCacheLocked(tenantId);
        XEntityCache item = entityCache.getIfPresent(apiKey);
        if (item != null) {
            return Either.right(item);
        }
        Either<String, XEntityCache> itemEither = provider.get(); //load entity from database
        if (itemEither.isLeft()) {
            return itemEither;
        }
        try {
            item = entityCache.get(apiKey, () -> setLoader(tenantId, itemEither.get()));
            return Either.right(item);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private XEntityCache setLoader(Object tenantId, XEntityCache item) {
        item.setLoadClass(e -> this.loadClass(tenantId, e));
        item.setLoadAssoc(e -> this.loadAssoc(tenantId, e));
        return item;
    }

    private Class<?> resolveEntityBeanClass(Object tenantId, XEntity entity) {
        Class<?> clazz = entity.getBeanType();
        Class<?> beanClass = objectBeanClass.get();
        if (beanClass.equals(clazz)) {//完全自定义的Bean
            String className = generateApiClassName(tenantId, entity.getName());
            clazz = classProvider.createClass(beanClass, className);
            entity.setBeanType(clazz);
        }
        return entity.getBeanType();
    }

    private Option<XEntityCache> getFromCached(String apiKey) {
        Object tenantId = tenantProvider.currentId();
        if (tenantId == null) {
            return Option.none();
        }
        return getEntityCache(tenantId).flatMap(e -> Option.of(e.getIfPresent(apiKey.toLowerCase())));
    }

    public boolean isApiClass(Class<?> clazz) {
        boolean notView = !clazz.isAnnotationPresent(View.class);
        return notView && DynamicEntity.class.isAssignableFrom(clazz);
    }

    private String getApiKey(Object tenantId, Class<?> beanClass) {
        String apiKey;
        if (isDynamicClass(beanClass.getSimpleName())) {
            String[] parts = splitClassPart(beanClass.getSimpleName());
            if (!StringUtils.equals(parts[1], String.valueOf(tenantId))) {
                throw new IllegalArgumentException(String.format("tenantId:%s,not equal to original tenant:%s", tenantId, parts[1]));
            }
            apiKey = parts[2];
        } else {
            Table table = beanClass.getAnnotation(Table.class);
            if (table == null) {
                throw new IllegalArgumentException(String.format("class:%s,was not contains a @Table annotation", beanClass.getName()));
            }
            apiKey = NamingUtils.toCamelCase(table.name());
        }
        return apiKey;
    }

    private String generateApiClassName(Object tenantId, String apiName) {
        String simpleName = API_PREFIX + tenantId + "_" + apiName;
        return getCustomEntityClassName(simpleName);
    }

    private boolean isDynamicClass(String simpleName) {
        return simpleName.startsWith(API_PREFIX);
    }


    private String[] splitClassPart(String className) {
        String[] parts = new String[3];
        int i = className.indexOf('_');
        parts[0] = className.substring(0, i);
        int j = className.indexOf('_', i + 1);
        parts[1] = className.substring(i + 1, j);
        parts[2] = className.substring(j + 1);
        return parts;
    }

    protected String getCustomEntityClassName(String simpleName) {
        String packageName = objectBeanClass.get().getPackageName();
        String className = packageName + '.' + simpleName;
        return className;
    }

    @Override
    public void setTenantProvider(CurrentTenantProvider tenantProvider) {
        this.tenantProvider = tenantProvider;
    }

    @Override
    public CurrentTenantProvider getTenantProvider() {
        return tenantProvider;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }
}
