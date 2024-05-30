package com.gitssie.openapi.ebean;

import com.gitssie.openapi.page.ModelConverter;
import com.gitssie.openapi.xentity.XEntityManager;
import io.ebean.Database;
import io.ebean.Expression;
import io.ebean.bean.EntityBean;
import io.ebean.plugin.BeanType;
import io.ebean.plugin.Property;
import io.ebeaninternal.api.SpiExpression;
import io.ebeaninternal.server.deploy.BeanDescriptor;
import io.ebeaninternal.server.deploy.BeanProperty;
import io.ebeaninternal.server.expression.DocQueryContext;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Deprecated
@Service
public class EbeanService {
    private Database database;
    private XEntityManager entityManager;
    private ConversionService conversionService;

    private ModelConverter modelConverter;

    public EbeanService(Database database, XEntityManager entityManager, FormattingConversionService conversionService, ModelConverter modelConverter) {
        this.database = database;
        this.entityManager = entityManager;
        this.conversionService = conversionService;
        this.modelConverter = modelConverter;
    }

    public <T> void custom(Map<String, Object> source, T bean) {
        Class<T> beanClass = (Class<T>) bean.getClass();
        BeanType<T> beanType = database.pluginApi().beanType(beanClass);
        custom(beanType, source, bean);
    }

    public <T> void custom(BeanType<T> beanType, Map<String, Object> source, T bean) {
        EntityBean target = (EntityBean) bean;
        Collection<? extends Property> properties = beanType.allProperties();
        for (Property p : properties) {
            BeanProperty property = (BeanProperty) p;
            if (!property.isCustom()) {
                continue;
            }
            if (source.containsKey(property.name())) {
                Object value = source.get(property.name());
                if (value != null) {
                    if (property.type().isAssignableFrom(value.getClass())) {
                        property.setValueIntercept(target, value);
                    } else {
                        value = conversionService.convert(value, property.type());
                        property.setValueIntercept(target, value);
                    }
                } else {
                    property.setValueIntercept(target, value);
                }
            }
        }
    }

    public <T> void copy(Map<String, Object> source, T bean) {
        Class<T> beanClass = (Class<T>) bean.getClass();
        BeanType<T> beanType = database.pluginApi().beanType(beanClass);
        copy(beanType, source, bean);
    }

    public <T> void copy(BeanType<T> beanType, Map<String, Object> source, T bean) {
        EntityBean target = (EntityBean) bean;
        Collection<? extends Property> properties = beanType.allProperties();
        for (Property p : properties) {
            BeanProperty property = (BeanProperty) p;
            if (source.containsKey(property.name())) {
                Object value = source.get(property.name());
                if (value != null) {
                    if (property.type().isAssignableFrom(value.getClass())) {
                        value = property.convert(value);
                        property.setValueIntercept(target, value);
                    } else if (property.isAssocId()) { //*ToOne
                        value = convertId(property.type(), value);
                        if (value != null) {
                            property.setValueIntercept(target, database.reference(property.type(), value));
                        } else {
                            property.setValueIntercept(target, null);//@TODO  需要考虑不能为空的问题
                        }
                    } else {
                        value = conversionService.convert(value, property.type());
                        property.setValueIntercept(target, value);
                    }
                } else {
                    //设置其余的属性为null,但是需要考虑基本数据类型不能为null的问题
                    property.setValueIntercept(target, null); //@TODO 需要考虑不能为空的问题
                }
            }
        }
    }

    public String toSQL(Expression expression) {
        SpiExpression spiExpression = (SpiExpression) expression;
        DocQueryContext context = new StringBufferDocQueryContext();
        try {
            spiExpression.writeDocQuery(context);
            return context.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isEntityBean(Class<?> clazz) {
        return EntityBean.class.isAssignableFrom(clazz);
    }

    public <T> T reference(Class<T> clazz, Object id) {
        if (ObjectUtils.isEmpty(id)) {
            return null;
        }
        return database.reference(clazz, id);
    }

    public Page<Map<String, Object>> toMap(Page pagedList) {
        return modelConverter.toJSON(pagedList);
    }

    public List<Map<String, Object>> toMap(List dataList) {
        return modelConverter.toJSON(dataList);
    }

    public Map<String, Object> toMap(Object objectBean) {
        return modelConverter.toJSON((EntityBean) objectBean);
    }

    public Map<String, Object> toMap(EntityBean bean) {
        return modelConverter.toJSON(bean);
    }

    public List<Map<String, Object>> toMap(Collection dataList) {
        return modelConverter.toJSON(dataList);
    }

    public Map<String, Object> toJson(Object bean) {
        return modelConverter.toJSON((EntityBean) bean);
    }

    public <T> T toBean(Class<T> beanType, String json) {
        return database.json().toBean(beanType, json);
    }

    public Class<?> getBeanClass(String apiKey) {
        return entityManager.getBeanClass(apiKey);
    }

    public <T> T createEntityBean(Class<T> type) {
        return database.createEntityBean(type);
    }

    public <T> T createEntityBean(String apiKey) {
        Class<?> beanClass = entityManager.getBeanClass(apiKey);
        return (T) database.createEntityBean(beanClass);
    }

    public <T> BeanType<T> desc(String apiKey) {
        Class<T> beanClass = (Class<T>) entityManager.getBeanClass(apiKey);
        return desc(beanClass);
    }

    public <T> BeanType<T> desc(Class<T> beanClass) {
        return database.pluginApi().beanType(beanClass);
    }

    public Object convertId(Class<?> type, Object id) {
        if (ObjectUtils.isEmpty(id)) {
            return null;
        }
        BeanDescriptor desc = (BeanDescriptor) desc(type);
        id = desc.convertId(id);
        return id;
    }

    public ConversionService getConversionService() {
        return conversionService;
    }
}
