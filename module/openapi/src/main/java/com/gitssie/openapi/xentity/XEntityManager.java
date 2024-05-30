package com.gitssie.openapi.xentity;

import com.gitssie.openapi.models.xentity.EntityMapping;
import io.ebean.config.CurrentTenantProvider;
import io.vavr.control.Either;
import org.springframework.beans.factory.InitializingBean;

public interface XEntityManager extends InitializingBean {
    XEntityCache getEntity(Class<?> beanClass);

    XEntityCache getEntity(String apiKey);

    boolean isExistsEntity(String apiKey);

    Either<String, XEntityCache> getEntityIfPresent(String apiKey);

    XEntityCache loadXEntity(EntityMapping mapping);

    void updateXEntityCache(Object tenantId, XEntityCache entity);

    Class<?> getBeanClass(String apiKey);

    CurrentTenantProvider getTenantProvider();

    void setTenantProvider(CurrentTenantProvider tenantProvider);

}
