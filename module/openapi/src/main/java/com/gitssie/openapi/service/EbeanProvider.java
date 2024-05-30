package com.gitssie.openapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gitssie.openapi.data.Code;
import com.gitssie.openapi.xentity.XEntityCache;
import com.gitssie.openapi.xentity.XEntityManager;
import com.gitssie.openapi.xentity.gen.CodeGenerated;
import io.ebean.*;
import io.ebean.bean.ElementBean;
import io.ebean.bean.EntityBean;
import io.ebean.bean.EntityBeanIntercept;
import io.ebean.plugin.BeanType;
import io.ebean.plugin.Property;
import io.ebean.text.json.JsonContext;
import io.ebeaninternal.server.deploy.BeanDescriptor;
import io.ebeaninternal.server.deploy.BeanProperty;
import io.ebeaninternal.server.deploy.parse.tenant.XEntity;
import io.vavr.Lazy;
import io.vavr.control.Either;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EbeanProvider implements Provider {

    private final XEntityManager entityManager;
    private final Database database;

    public EbeanProvider(XEntityManager entityManager, Database database) {
        this.entityManager = entityManager;
        this.database = database;
    }

    @Override
    public ExpressionFactory expr() {
        return database.expressionFactory();
    }

    @Override
    public <T> Class<T> beanClass(String apiKey) {
        return (Class<T>) entityManager.getBeanClass(apiKey);
    }

    @Override
    public <T> BeanType<T> desc(Class<T> beanClass) {
        return database.pluginApi().beanType(beanClass);
    }

    @Override
    public <T> BeanType<T> desc(String apiKey) {
        Class<T> clazz = beanClass(apiKey);
        return desc(clazz);
    }

    @Override
    public <T> Query<T> createQuery(Class<T> beanClass) {
        return database.createQuery(beanClass);
    }

    @Override
    public <T> Filter<T> filter(Class<T> beanType) {
        return database.filter(beanType);
    }

    @Override
    public Object reference(Class<?> beanClass, Object id) {
        return database.reference(beanClass, id);
    }

    @Override
    public <T extends EntityBean> boolean isLoadedProperty(T bean, BeanProperty prop) {
        return isLoadedProperty(bean, null, prop);
    }

    @Override
    public <T extends EntityBean> boolean isLoadedProperty(T bean, EntityBeanIntercept intercept, BeanProperty prop) {
        if (prop.isCustom()) {
            ElementBean value = (ElementBean) bean._ebean_getField(prop.fieldIndex()[0]);
            if (value == null) {
                return false;
            }
            EntityBeanIntercept ebi = value._ebean_getIntercept();
            if (ebi.getPropertyLength() == 0) {
                return value.containsKey(prop.name());
            } else {
                return ebi.isLoadedProperty(prop.fieldIndex()[1]);
            }
        } else {
            if (intercept == null) {
                intercept = bean._ebean_getIntercept();
            }
            return intercept.isLoadedProperty(prop.propertyIndex());
        }
    }

    public boolean isEntityBean(Class<?> clazz) {
        return EntityBean.class.isAssignableFrom(clazz);
    }

    @Override
    public Object beanId(Object bean) {
        if (bean == null) {
            return null;
        }
        return database.beanId(bean);
    }

    @Override
    public Object beanId(Object bean, Object id) {
        return database.beanId(bean, id);
    }

    @Override
    public Object convertId(BeanType<?> beanType, Object id) {
        if (ObjectUtils.isEmpty(id)) {
            return null;
        } else if (id instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) id;
            id = map.get(beanType.idProperty().name());
            if (ObjectUtils.isEmpty(id)) {
                return null;
            }
        } else if (id instanceof ObjectNode) {
            ObjectNode map = (ObjectNode) id;
            JsonNode idValue = map.get(beanType.idProperty().name());
            if (idValue == null) {
                return null;
            } else {
                id = idValue.asText();
            }
        }
        BeanDescriptor desc = (BeanDescriptor) beanType;
        return desc.idProperty().parse(id.toString());
    }

    public Property codeProperty(BeanType<?> beanType) {
        BeanDescriptor desc = (BeanDescriptor) beanType;
        for (BeanProperty property : desc.propertiesGenInsert()) {
            if (property.generatedProperty() instanceof CodeGenerated) {
                return property;
            }
        }
        for (BeanProperty property : desc.propertiesTransient()) {
            if (property.isGenerated()) {
                return property;
            }
        }
        return null;
    }

    @Override
    public Object convertCode(Property property, Object code) {
        BeanProperty props = (BeanProperty) property;
        if (ObjectUtils.isEmpty(code)) {
            return null;
        } else if (code instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) code;
            code = map.get(props.name());
            if (ObjectUtils.isEmpty(code)) {
                return null;
            }
        } else if (code instanceof ObjectNode) {
            ObjectNode map = (ObjectNode) code;
            JsonNode codeValue = map.get(props.name());
            if (codeValue == null) {
                return null;
            } else {
                code = codeValue.asText();
            }
        }
        return props.parse(code.toString());
    }

    public <T> T createBean(Class<T> beanClass) {
        BeanType<T> desc = this.desc(beanClass);
        return desc.createBean();
    }

    @Override
    public <T> Either<Code, T> createBean(Class<T> beanClass, final Map source, boolean updateOnly) {
        BeanType<T> desc = this.desc(beanClass);
        Object id = convertId(desc, source);
        T bean = null;
        if (id != null) {
            bean = findBean(desc.type(), id);
            if (bean == null) {
                return Either.left(Code.NOT_FOUND.withMessage("not found by id" + id));
            }
        } else {
            BeanProperty codeProperty = (BeanProperty) codeProperty(desc);
            if (codeProperty != null && !codeProperty.isTransient()) {
                Object code = convertCode(codeProperty, source);
                bean = findBeanByCode(desc.type(), codeProperty, code);
                if (bean == null && updateOnly) {
                    return Either.left(Code.NOT_FOUND.withMessage("not found by code" + code));
                }
            }
        }
        if (bean == null && updateOnly) {
            return Either.left(Code.NOT_FOUND);
        } else if (bean == null) {
            bean = desc.createBean();
        }
        return Either.right(bean);
    }

    @Override
    public <T> T findBean(Class<?> beanClass, Object id) {
        return (T) database.find(beanClass, id);
    }

    @Override
    public <T> T findBeanByCode(Class<?> beanClass, Property codeProperty, Object code) {
        return (T) database.createQuery(beanClass).where().eq(codeProperty.name(), code).setMaxRows(1).findOne();
    }

    @Override
    public <T> T referenceById(BeanType<T> desc, Object id, String select) {
        return (T) database.createQuery(desc.type()).select(select).setId(id).findOne();
    }

    @Override
    public <T> T referenceByCode(BeanType<T> desc, Property codeProperty, Object code, String select) {
        return (T) database.createQuery(desc.type()).select(select).where().eq(codeProperty.name(), code).setMaxRows(1).findOne();
    }

    @Override
    public <T> T filter(T bean, Expression expr) {
        throw new UnsupportedOperationException();
    }

    @Override
    public XEntityCache getEntity(Class<?> type) {
        return entityManager.getEntity(type);
    }

    @Override
    public XEntityCache getEntity(String apiKey) {
        return entityManager.getEntity(apiKey);
    }

    @Override
    public Database db() {
        return this.database;
    }

    @Override
    public JsonContext json() {
        return this.database.json();
    }

    @Override
    public Either<String, XEntityCache> getEntityIfPresent(String apiKey) {
        return entityManager.getEntityIfPresent(apiKey);
    }

    @Override
    public Either<String, Class<?>> getBeanClassIfPresent(String apiKey) {
        return entityManager.getEntityIfPresent(apiKey).map(entity -> entity.getBeanType());
    }

    @Override
    public <T> Either<String, BeanType<T>> getBeanTypeIfPresent(String apiKey) {
        return entityManager.getEntityIfPresent(apiKey).map(entity -> {
            Class<T> clazz = (Class<T>) entity.getBeanType();
            return database.pluginApi().beanType(clazz);
        });
    }
}
