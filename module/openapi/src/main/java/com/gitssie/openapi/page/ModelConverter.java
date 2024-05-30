package com.gitssie.openapi.page;

import com.gitssie.openapi.rule.Rules;
import com.gitssie.openapi.service.Provider;
import io.ebean.bean.EntityBean;
import io.ebean.bean.EntityBeanIntercept;
import io.ebean.plugin.BeanType;
import io.ebean.plugin.Property;
import io.ebeaninternal.server.deploy.BeanProperty;
import io.ebeaninternal.server.deploy.BeanPropertyAssoc;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class ModelConverter {
    private Provider provider;

    public ModelConverter(Provider provider) {
        this.provider = provider;
    }

    public Object toMap(FetchContext context, Model model, BeanType<?> desc, Object bean) {
        if (bean instanceof EntityBean) {
            return toMap(context, model, desc, (EntityBean) bean);
        } else if (bean instanceof Collection) {
            return toMapArray(context, model, desc, (Collection) bean);
        } else {
            return null;
        }
    }

    public <T extends EntityBean> Map<String, Object> toJSON(T bean) {
        return toJSON((Model) null, bean);
    }

    public <T extends EntityBean> Map<String, Object> toJSON(Model model, T bean) {
        FetchContext context = new FetchContext(provider);
        try(context){
            return toJSON(context, model, bean);
        }
    }

    public <T extends EntityBean> Map<String, Object> toJSON(FetchContext context, Model model, T bean) {
        BeanType<?> desc = provider.desc(bean.getClass());
        Map<String, Object> result = toMap(context, model, desc, bean);
        return result;
    }

    public <T extends EntityBean> List<Map<String, Object>> toJSON(Class<T> clazz, Collection<T> beanList) {
        return toJSON((Model) null, clazz, beanList);
    }

    public <T extends EntityBean> List<Map<String, Object>> toJSON(Model model, Class<T> clazz, Collection<T> beanList) {
        FetchContext context = new FetchContext(provider);
        try(context){
            return toJSON(context, model, clazz, beanList);
        }
    }

    public <T extends EntityBean> List<Map<String, Object>> toJSON(FetchContext context, Model model, Class<T> clazz, Collection<T> beanList) {
        BeanType<?> desc = provider.desc(clazz);
        List<Map<String, Object>> result = toMapArray(context, model, desc, beanList);
        return result;
    }

    public <T extends EntityBean> Page<Map<String, Object>> toJSON(Page<T> pagedList) {
        List<Map<String, Object>> content = toJSON(pagedList.getContent());
        PageImpl result = new PageImpl(content, pagedList.getPageable(), pagedList.getTotalElements());
        return result;
    }

    public <T extends EntityBean> List<Map<String, Object>> toJSON(Collection<T> beanList) {
        return toJSON((Model) null, beanList);
    }

    public <T extends EntityBean> List<Map<String, Object>> toJSON(Model model, Collection<T> beanList) {
        Class<T> clazz = null;
        for (T bean : beanList) {
            clazz = (Class<T>) bean.getClass();
            break;
        }
        return toJSON(model, clazz, beanList);
    }

    public <T extends EntityBean> Map<String, Object> toJSON(BeanType<?> desc, T bean) {
        FetchContext context = new FetchContext(provider);
        try (context) {
            Map<String, Object> result = toMap(context, null, desc, bean);
            return result;
        }
    }

    public <T extends EntityBean> List<Map<String, Object>> toJSONArray(BeanType<?> desc, Collection<T> bean) {
        FetchContext context = new FetchContext(provider);
        try (context) {
            List<Map<String, Object>> result = toMapArray(context, null, desc, bean);
            return result;
        }
    }

    public <T extends EntityBean> List<Map<String, Object>> toMapArray(FetchContext context, Model model, BeanType<?> desc, Collection<T> arr) {
        return toListMap(context, model, desc, arr);
    }

    private <T extends EntityBean> List<Map<String, Object>> toListMap(FetchContext context, Model model, BeanType desc, Collection<T> arr) {
        if (ObjectUtils.isEmpty(arr)) {
            return context.newArrayList(0);
        }
        List<Map<String, Object>> content = new ArrayList<>();
        for (EntityBean bean : arr) {
            content.add(toMap(context, model, desc, bean));
        }
        return content;
    }

    private Map<String, Object> toMap(FetchContext context, Model model, BeanType<?> desc, EntityBean bean) {
        if (model != null) {
            return modelToMap(context, model, desc, bean);
        } else {
            return beanToMap(false, desc, bean, context.newLinkedHashMap());
        }
    }

    private Map<String, Object> beanToMap(boolean lazyLoad, BeanType<?> desc, EntityBean bean, Map<String, Object> res) {
        EntityBeanIntercept intercept = bean._ebean_getIntercept();
        for (Property p : desc.allProperties()) {
            if (res.containsKey(p.name())) {
                continue;
            }
            BeanProperty property = (BeanProperty) p;
            if (property.isTransient()) {
                continue;
            }
            if (property.isAssocProperty()) {
                continue;
            }
            Object value = null;
            if (lazyLoad) {
                value = property.getValueIntercept(bean);
            } else if (provider.isLoadedProperty(bean, intercept, property)) {
                value = property.value(bean);
            }
            if (value != null) {
                res.put(property.name(), value);
            }
        }
        return res;
    }

    private Map<String, Object> modelToMap(FetchContext context, Model model, BeanType desc, EntityBean bean) {
        Map<String, Object> res;
        if (model.isIncludeAllLoadedProps()) {
            res = context.newLinkedHashMap();
        } else {
            res = context.newHashMap(model.fields.size() + model.assoc.size() + 4);
        }
        //必须具备的属性
        res.put(desc.idProperty().name(), desc.idProperty().value(bean));

        for (Field field : model.fields) {
            String name = field.getName();
            String key = StringUtils.defaultIfBlank(field.getField(), name);
            if (res.containsKey(key)) {
                continue;
            }
            //step1.可能是其余属性需要加载,直接跟bean关联,延迟加载分离查询的数据
            if (field.getLazy() != null) {
                res.put(key, assocLazyValue(context, model, field, bean));
                continue;
            }

            BeanProperty property = (BeanProperty) desc.property(name);
            if (property == null) {
                res.put(key, scalarToValue(context, property, field, null, bean));
                continue;
            }
            //step2.取出值进行正常的数据处理,这里为了兼容custom属性,需要使用provider.isLoadedProperty判断是否加载
            Object value = context.enableLazyFetch() ? property.getValueIntercept(bean) : property.getValue(bean);
            //step3.数据过滤
            if (value != null && Rules.isFunction(field.getFilter())) {
                value = Rules.toValue(context.context, field.getFilter(), value, bean);
            }
            //step4.数据格式化
            if (property.isScalar()) {
                res.put(key, scalarToValue(context, property, field, value, bean));
                if (field.getOptionsMap().isDefined()) {
                    res.put(context.getLabelKey(key), scalarToLabel(context, field, value));
                }
            } else if (value != null && property.isAssocProperty()) {
                BeanPropertyAssoc assocProp = (BeanPropertyAssoc) property;
                if (field.getFormat() != null) {
                    res.put(key, scalarToValue(context, property, field, value, bean));
                } else if (property.isAssocMany()) {
                    res.put(key, assocManyToMap(context, model, assocProp, value));
                } else {
                    res.put(key, assocOneToMap(context, model, assocProp, field, value));
                }
            } else {
                res.put(key, value);
            }
        }

        if (model.isIncludeAllLoadedProps()) {
            beanToMap(true, desc, bean, res);
        }

        return res;
    }

    private Object scalarToValue(FetchContext context, BeanProperty property, Field field, Object value, Object bean) {
        if (property != null && property.isDbEncrypted()) {
            return "***"; //mask
        }
        if (field.getFormat() != null) { //格式化
            return context.addLazyValue(field.getName(), value, field, bean);
        } else if (value == null && field.getValue() != null) { //默认值
            return context.addLazyValue(field.getName(), null, field, bean);
        } else {
            return value;
        }
    }

    private Object scalarToLabel(FetchContext context, Field field, Object value) {
        if (ObjectUtils.isEmpty(field.getOptions())) { //格式化
            return null;
        } else {
            return context.addLazyLabel(field.getName(), value, field);
        }
    }

    private Collection<?> assocManyToMap(FetchContext context, Model parent, BeanPropertyAssoc property, Object value) {
        Model model = parent.assoc.get(property.name());
        Collection<EntityBean> list = (Collection<EntityBean>) value;
        list.hashCode(); //load from db
        return toListMap(context, model, property.targetDescriptor(), list);
    }

    private Object assocOneToMap(FetchContext context, Model parent, BeanPropertyAssoc property, Field field, Object value) {
        if (value == null) {
            return null;
        }
        EntityBean ebean = (EntityBean) value;
        Model model = parent.assoc.get(property.name());
        BeanType<?> desc = property.targetDescriptor();
        if (ebean._ebean_getIntercept().isLoaded()) {
            if (model == null) {
                return desc.idProperty().value(ebean);
            }
            return modelToMap(context, model, desc, ebean);
        } else if (model != null && !model.isEmpty()) {
            return context.addPath(property, desc, ebean, model.getQueryFields(), (stack, refBean) -> {
                return modelToMap(stack, model, desc, (EntityBean) refBean);
            });
        } else {
            return context.addPath(property, desc, ebean, field);
        }
    }

    private LazyValue assocLazyValue(FetchContext context, Model parent, Field field, EntityBean ebean) {
        Model model = parent.assoc.get(field.getName());
        if (model != null && !model.isEmpty()) {
            return context.addLazyPath(field.getName(), ebean, field, (stack, refBean) -> {
                if (refBean instanceof Map) {
                    return modelToMap(stack, model, (Map<String, Object>) refBean);
                } else {
                    return modelToMap(stack, model, stack.desc(refBean), (EntityBean) refBean);
                }
            });
        } else {
            return context.addLazyPath(field.getName(), ebean, field);
        }
    }

    public Map<String, Object> toJSONMap(Model model, Map<String, Object> map) {
        FetchContext context = new FetchContext(provider);
        try(context){
            return toJSONMap(context, model, map);
        }
    }

    public Map<String, Object> toJSONMap(FetchContext context, Model model, Map<String, Object> map) {
        Map<String, Object> result = modelToMap(context, model, map);
        return result;
    }

    public Page<Map<String, Object>> toJSONMap(Model model, Page<Map<String, Object>> pagedList) {
        List<Map<String, Object>> content = toJSONMap(model, pagedList.getContent());
        PageImpl result = new PageImpl(content, pagedList.getPageable(), pagedList.getTotalElements());
        return result;
    }

    public List<Map<String, Object>> toJSONMap(Model model, Collection<Map<String, Object>> arr) {
        FetchContext context = new FetchContext(provider);
        try(context){
            return toJSONMap(context, model, arr);
        }
    }

    public List<Map<String, Object>> toJSONMap(FetchContext context, Model model, Collection<Map<String, Object>> arr) {
        List<Map<String, Object>> result = context.newArrayList();
        for (Map<String, Object> map : arr) {
            result.add(modelToMap(context, model, map));
        }
        return result;
    }

    private LazyValue assocLazyValue(FetchContext context, Field field, Map<String, Object> bean) {
        return context.addLazyPathValue(field.getName(), bean, field);
    }

    private Object assocOneToMap(FetchContext context, Model model, ModelAssoc annotation, String name, Map<String, Object> bean) {
        Object value = annotation == null ? bean.get(name) : bean.get(StringUtils.defaultString(annotation.name, name));
        if (value == null) {
            return null;
        }
        if (model.fields.isEmpty()) {
            return value;
        } else {
            String apiKey = annotation == null ? model.apiKey : StringUtils.defaultString(annotation.apiKey, model.apiKey);
            if (StringUtils.isEmpty(apiKey)) {
                throw new IllegalArgumentException(String.format("property %s,apiKey is empty", name));
            }
            BeanType<?> desc = provider.desc(apiKey);
            BeanProperty mappedBy = null;
            if (annotation != null && StringUtils.isNotEmpty(annotation.mappedBy)) {
                mappedBy = (BeanProperty) desc.property(annotation.mappedBy);
                if (mappedBy == null) {
                    throw new IllegalArgumentException(String.format("mappedBy property %s at apiKey is empty", annotation.mappedBy));
                }
            }

            if (annotation != null && annotation.isMany()) { //to many
                if (mappedBy == null) {
                    throw new IllegalArgumentException(String.format("assoc many mappedBy property %s at apiKey is empty", annotation.mappedBy));
                }
                return context.addPathMany(name, value, mappedBy, model.fields, (stack, beanList) -> {
                    return toListMap(stack, model, desc, (Collection) beanList);
                });
            } else {// to one
                if (mappedBy == null) {
                    return context.addPath(name, value, desc, model.fields, (stack, refBean) -> {
                        return modelToMap(stack, model, desc, (EntityBean) refBean);
                    });
                } else {
                    return context.addPath(name, value, mappedBy, model.fields, (stack, refBean) -> {
                        return modelToMap(stack, model, desc, (EntityBean) refBean);
                    });
                }
            }
        }
    }

    private Map<String, Object> modelToMap(FetchContext context, Model model, Map<String, Object> bean) {
        for (Field field : model.fields) {
            String name = field.getName();
            String key = StringUtils.defaultIfBlank(field.getField(), name);
            if (field.getLazy() != null) { //虚拟字段,延迟加载关联使用
                bean.put(key, assocLazyValue(context, field, bean));
            } else if (field.getFormat() != null || field.getValue() != null) {//对数据进行格式化,默认值赋值
                Object value = bean.get(name);
                bean.put(key, scalarToValue(context, null, field, value, bean));
            }
            if (field.getOptionsMap().isDefined()) {
                bean.put(context.getLabelKey(key), scalarToLabel(context, field, field.getValue()));
            }
        }

        for (Map.Entry<String, Model> entry : model.assoc.entrySet()) {
            String name = entry.getKey();
            Model assoc = entry.getValue();
            ModelAssoc annotation = assoc.getAssocAnnotation();
            //需要进行关联查询,assocOne,assocMany
            bean.put(name, assocOneToMap(context, assoc, annotation, name, bean));
        }
        return bean;
    }


}
