package com.gitssie.openapi.service;

import com.gitssie.openapi.data.Code;
import com.gitssie.openapi.xentity.XEntityCache;
import io.ebean.*;
import io.ebean.bean.EntityBean;
import io.ebean.bean.EntityBeanIntercept;
import io.ebean.plugin.BeanType;
import io.ebean.plugin.Property;
import io.ebean.text.json.JsonContext;
import io.ebeaninternal.server.deploy.BeanProperty;
import io.ebeaninternal.server.deploy.parse.tenant.XEntity;
import io.vavr.control.Either;

import java.util.Map;

public interface Provider {

    ExpressionFactory expr();

    <T> Class<T> beanClass(String apiKey);

    <T> BeanType<T> desc(Class<T> beanClass);

    <T> BeanType<T> desc(String apiKey);

    <T> Query<T> createQuery(Class<T> beanClass);

    <T> Filter<T> filter(Class<T> beanType);

    <T extends EntityBean> boolean isLoadedProperty(T bean, BeanProperty prop);

    <T extends EntityBean> boolean isLoadedProperty(T bean, EntityBeanIntercept intercept, BeanProperty prop);

    boolean isEntityBean(Class<?> clazz);

    Object reference(Class<?> beanClass, Object id);

    Object beanId(Object bean);

    Object beanId(Object bean, Object id);

    Property codeProperty(BeanType<?> beanType);

    Object convertId(BeanType<?> beanType, Object id);

    Object convertCode(Property property, Object code);

    <T> T createBean(Class<T> beanClass);

    <T> Either<Code, T> createBean(Class<T> beanClass, final Map source, boolean updateOnly);

    <T> T findBean(Class<?> beanClass, Object id);

    <T> T findBeanByCode(Class<?> type, Property codeProperty, Object code);

    <T> T referenceById(BeanType<T> desc, Object id, String select);

    <T> T referenceByCode(BeanType<T> desc, Property codeProperty, Object code, String select);

    <T> T filter(T bean, Expression expr);

    XEntityCache getEntity(Class<?> type);

    XEntityCache getEntity(String apiKey);

    Database db();

    JsonContext json();

    Either<String, XEntityCache> getEntityIfPresent(String apiKey);

    Either<String, Class<?>> getBeanClassIfPresent(String apiKey);

    <T> Either<String, BeanType<T>> getBeanTypeIfPresent(String apiKey);
}
