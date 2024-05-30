package com.gitssie.openapi.page;


import com.gitssie.openapi.service.Provider;
import io.ebean.bean.EntityBean;
import io.ebean.plugin.BeanType;
import io.ebeaninternal.server.deploy.BeanProperty;
import io.ebeaninternal.server.deploy.BeanPropertyAssoc;

import java.util.Collection;

public interface NodeConversion<T> {
    EntityBean copy(NeedContext context, Provider provider, BeanType<?> desc, final T source, final EntityBean bean) throws Exception;

    void copyScalar(BeanProperty property,final T source, final EntityBean bean) throws Exception;

    Object assocOne(NeedContext context, Provider provider,BeanPropertyAssoc assoc, EntityBean rootBean, T value) throws Exception;

    Collection<?> assocMany(NeedContext context, Provider provider,BeanPropertyAssoc assoc, EntityBean rootBean, T value) throws Exception;
}