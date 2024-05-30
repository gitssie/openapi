package com.gitssie.openapi.page;

import com.alibaba.fastjson.util.TypeUtils;
import com.fasterxml.jackson.databind.JavaType;
import com.gitssie.openapi.service.Provider;
import com.google.common.collect.Maps;
import io.ebean.bean.EntityBean;
import io.ebean.plugin.BeanType;
import io.ebean.plugin.Property;
import io.ebeaninternal.server.deploy.BeanProperty;
import io.ebeaninternal.server.deploy.BeanPropertyAssoc;
import io.ebeaninternal.server.deploy.BeanPropertyAssocMany;
import io.ebeaninternal.server.deploy.BeanPropertyAssocOne;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NodeConversionMap implements NodeConversion<Map> {
    private static final Logger LOGGER = LoggerFactory.getLogger(NodeConversionMap.class);
    private final Map<Class<?>, java.lang.reflect.Field> javaTypeField = new ConcurrentHashMap<>();

    @Autowired
    private Provider provider;


    /**
     * 从源对象复制属性到目标对象
     *
     * @param source
     * @param target
     */
    public Object copyTo(Object source, Object target) throws Exception {
        if (source instanceof EntityBean) {
            copyTo((EntityBean) source, target);
        } else if (source instanceof Map) {
            copyTo((Map) source, target);
        } else {
            BeanUtils.copyProperties(source, target);
        }
        return target;
    }

    public Object copyTo(Object source, Class<?> clazz) throws Exception {
        if (EntityBean.class.isAssignableFrom(clazz)) {
            return copyTo(source, provider.createBean(clazz));
        } else if (clazz.equals(Map.class)) {
            return copyTo(source, Maps.newHashMap());
        } else {
            return copyTo(source, BeanUtils.instantiateClass(clazz));
        }
    }

    private void copyTo(EntityBean source, Object target) throws Exception {
        if (target instanceof EntityBean) {
            BeanType<?> desc = provider.desc(source.getClass());
            copyTo(desc, source, (EntityBean) target);
        } else if (target instanceof Map) {
            BeanType<?> desc = provider.desc(source.getClass());
            copyTo(desc, source, (Map) target);
        } else {
            BeanUtils.copyProperties(source, target);
        }
    }

    private void copyTo(Map source, Object target) throws Exception {
        if (target instanceof EntityBean) {
            NeedContext context = new NeedContext();
            try (context) {
                BeanType<?> desc = provider.desc(target.getClass());
                copy(context, desc, source, (EntityBean) target);
            }
        } else if (target instanceof Map) {
            ((Map<?, ?>) target).putAll(source); //copy all
        } else {
            BeanUtils.copyProperties(source, target);
        }
    }

    private void copyTo(BeanType<?> desc, EntityBean source, Map target) {
        for (Property prop : desc.allProperties()) {
            BeanProperty property = (BeanProperty) prop;
            if (property.isScalar()) {
                target.put(property.name(), property.getValueIntercept(source));
            }
        }
    }

    private void copyTo(BeanType<?> desc, EntityBean source, EntityBean target) {
        BeanType<?> targetDesc = provider.desc(source.getClass());
        for (Property prop : targetDesc.allProperties()) {
            BeanProperty trg = (BeanProperty) prop;
            BeanProperty src = (BeanProperty) desc.property(prop.name());
            if (src == null || trg.isId()) {
                continue;
            }
            //复制基本属性
            if (trg.isScalar() && src.isScalar()) {
                trg.setValueIntercept(target, src.getValueIntercept(source)); //copy some name scalar value
            } else if (trg.isAssocId() && src.isAssocId()) {
                trg.setValueIntercept(target, src.getValueIntercept(source)); //copy some name assoc by id entity
            }
        }
    }

    public EntityBean copy(NeedContext context, BeanType<?> desc, final Map source, final EntityBean bean) throws Exception {
        return copy(context, provider, desc, source, bean);
    }

    @Override
    public EntityBean copy(NeedContext context, Provider provider, BeanType<?> desc, final Map source, final EntityBean bean) throws Exception {
        for (Property prop : desc.allProperties()) {
            if (source.containsKey(prop.name())) {
                BeanProperty property = (BeanProperty) prop;
                if (property.isScalar()) {
                    copyScalar(property, source, bean);
                } else if (property instanceof BeanPropertyAssocOne) {
                    BeanPropertyAssoc assoc = (BeanPropertyAssoc) property;
                    Object assocValue = assocOne(context, provider, assoc, bean, source);
                    property.setValueIntercept(bean, assocValue); //@TODO 需要考虑不能为空的问题
                } else if (property instanceof BeanPropertyAssocMany) {
                    BeanPropertyAssocMany assoc = (BeanPropertyAssocMany) property;
                    Object assocValue = assocMany(context, provider, assoc, bean, source);
                    property.setValueIntercept(bean, assocValue); //@TODO 需要考虑不能为空的问题
                } else if (property.isEmbedded()) {

                }
            }
        }
        return bean;
    }

    @Override
    public void copyScalar(BeanProperty property, Map node, EntityBean bean) throws Exception {
        Object source = node.get(property.name());
        Object value;
        if (source != null) {
            //如果是集合类的JSON
            if (Collection.class.isAssignableFrom(property.type())) {
                if (!(source instanceof Collection)) {
                    return;
                }
                java.lang.reflect.Field fd = getJavaType(property.scalarType().getClass(), "deserType");
                if (fd != null) {
                    JavaType javaType = (JavaType) fd.get(property.scalarType());
                    if (javaType.isCollectionLikeType()) {
                        value = source;
                    } else {
                        value = TypeUtils.cast(source, javaType, null);
                    }
                } else {
                    value = TypeUtils.castToJavaBean(source, property.type());
                }
            } else {
                value = TypeUtils.castToJavaBean(source, property.type());
            }
            value = property.convert(value);
            property.setValueIntercept(bean, value);
        } else {
            property.setValueIntercept(bean, null);
        }
    }

    @Override
    public Object assocOne(NeedContext context, Provider provider, BeanPropertyAssoc assoc, EntityBean rootBean, Map node) throws Exception {
        boolean isSave = assoc.cascadeInfo().isSave();
        Object value = node.get(assoc.name());
        EntityBean bean = (EntityBean) assoc.getValueIntercept(rootBean);
        if (isSave) {//级联
            if (value == null) {
                if (bean != null && assoc.cascadeInfo().isDelete()) {
                    context.needDelete.add(bean);
                }
                return null;
            } else if (!(value instanceof Map)) {
                return bean;
            } else if (((Map) value).isEmpty()) {
                return bean;
            }
            if (bean == null) {
                bean = (EntityBean) assoc.targetDescriptor().createBean();
            }
            copy(context, provider, assoc.targetDescriptor(), (Map) value, bean);
            return bean;
        } else if (value == null) {
            return null; //取消对象关联
        } else if (!(value instanceof Map)) {
            return assocRefById(provider, assoc, rootBean, value);
        } else {//关联
            return assocRef(provider, assoc, rootBean, (Map) value);
        }
    }

    @Override
    public Collection<?> assocMany(NeedContext context, Provider provider, BeanPropertyAssoc assoc, EntityBean rootBean, Map node) throws Exception {
        Object value = node.get(assoc.name());
        Collection beanList = (Collection<EntityBean>) assoc.getValueIntercept(rootBean);
        if (value == null || !(value instanceof Collection)) {
            return beanList;
        }
        Collection sourceList = (Collection) value;
        if (sourceList.isEmpty() && beanList != null) {
            if (assoc.cascadeInfo().isDelete()) {
                context.needDelete.addAll(beanList);
            }
            beanList.clear();
            return beanList;
        }
        if (beanList == null) {
            beanList = new LinkedList<>();
        }
        boolean isSave = assoc.cascadeInfo().isSave();
        BeanType<?> desc = assoc.targetDescriptor();
        Property idProperty = desc.idProperty();
        BeanProperty codeProperty = (BeanProperty) provider.codeProperty(desc);
        Property mappedBy = null;
        if (StringUtils.isNotEmpty(assoc.mappedBy())) {
            mappedBy = desc.property(assoc.mappedBy());
        }
        //ID映射关系
        Map<Object, Map> sourceIdMap = Maps.newLinkedHashMap();
        Map<Object, Map> sourceCodeMap = Maps.newLinkedHashMap();
        List<Map> added = new LinkedList<>();
        Map item;
        for (Object map : sourceList) {
            if (!(map instanceof Map)) {
                continue; //@TODO 是否需要抛出异常处理？
            }
            item = (Map) map;
            Object id = provider.convertId(desc, item.get(idProperty.name()));
            if (id != null) {
                sourceIdMap.put(id, item);
                continue;
            } else if (codeProperty != null) {
                Object code = provider.convertCode(codeProperty, item.get(codeProperty.name()));
                if (ObjectUtils.isNotEmpty(code)) {
                    sourceCodeMap.put(code, item);
                    continue;
                }
            }
            added.add(item);
        }

        //查找更新、删除的
        Iterator<EntityBean> iterator = beanList.iterator();
        Object id, code;
        while (iterator.hasNext()) {
            EntityBean bean = iterator.next();
            id = idProperty.value(bean);
            Map source = sourceIdMap.remove(id);
            if (source == null) {
                if (codeProperty != null) {
                    code = codeProperty.getValueIntercept(bean);
                    source = sourceCodeMap.remove(code);
                }
            }
            if (source == null) {
                if (assoc.cascadeInfo().isDelete()) {
                    context.needDelete.add(bean);
                }
                iterator.remove(); //需要删除的数据
            } else if (source != null && isSave) {
                copy(context, provider, desc, source, bean);//需要修改的数据，设置值
            }
        }
        //@TODO 指定ID、编码关联数据，需要判断是否能进行关联，不能关联错误对象,比如关联到其它用户的数据
        for (Object idValue : sourceIdMap.keySet()) {
            Object refValue = getRefById(provider, desc, idValue, mappedBy);
            if (refValue != null && !isMappedByValueChanged(provider, rootBean, refValue, mappedBy)) {
                beanList.add(refValue);
            }
        }
        //指定编码更新数据
        if (codeProperty != null) {
            for (Map.Entry<Object, Map> codeValue : sourceCodeMap.entrySet()) {
                if (isSave) {
                    EntityBean bean = (EntityBean) desc.createBean();
                    copy(context, provider, desc, codeValue.getValue(), bean);//设置值
                    beanList.add(bean);
                    continue;
                }
                Object refValue = getRefByCode(provider, desc, codeProperty, codeValue.getKey(), mappedBy);
                if (refValue != null && !isMappedByValueChanged(provider, rootBean, refValue, mappedBy)) {
                    beanList.add(refValue);
                } else if (refValue != null) {
                    LOGGER.error("数据编码:{},类型:{},关联实体错误", codeValue.getKey(), desc.name());
                }
            }
        }
        //新增的数据
        if (mappedBy != null && isSave) {
            for (Map source : added) {
                EntityBean bean = (EntityBean) desc.createBean();
                copy(context, provider, desc, source, bean);//设置值
                beanList.add(bean);
            }
        }
        return beanList;
    }

    public boolean isMappedByValueChanged(Provider provider, EntityBean bean, Object refValue, Property mappedBy) {
        if (mappedBy == null || refValue == null) {
            return false;
        }
        Object mappedId = mappedBy.value(refValue);
        if (mappedId == null) {
            return false;
        }
        Object id = provider.beanId(bean);
        return ObjectUtils.notEqual(id, mappedId);
    }

    private Object getRefById(Provider provider, BeanType<?> desc, Object value, Property mappedBy) {
        Object assocId = provider.convertId(desc, value);
        if (assocId != null) {
            String select = mappedBy == null ? null : mappedBy.name();
            return provider.referenceById(desc, assocId, select); //新关联另外一个对象
        } else {
            return null;
        }
    }

    private Object getRefByCode(Provider provider, BeanType<?> desc, Property codeProperty, Object value, Property mappedBy) {
        BeanProperty bp = (BeanProperty) codeProperty;
        if (bp.isTransient()) {
            return null;
        }
        Object code = provider.convertCode(codeProperty, value);
        if (code != null) {
            String select = codeProperty.name();
            if (mappedBy != null) {
                select = "," + mappedBy.name();
            }
            return provider.referenceByCode(desc, codeProperty, code, select); //新关联另外一个对象
        } else {
            return null;
        }
    }

    private Object assocRefById(Provider provider, BeanPropertyAssoc assoc, EntityBean rootBean, Object value) {
        BeanType<?> desc = provider.desc(assoc.type());
        Object originRefValue = assoc.getValueIntercept(rootBean);
        Object assocId = provider.convertId(desc, value);
        if (assocId != null) {
            if (originRefValue != null && provider.beanId(originRefValue).equals(assocId)) {
                return originRefValue; //没有变更
            }
            return provider.referenceById(desc, assocId, null); //新关联另外一个对象
        } else {
            return null;
        }
    }

    private Object assocRef(Provider provider, BeanPropertyAssoc assoc, EntityBean rootBean, Map value) {
        BeanType<?> desc = provider.desc(assoc.type());
        Object originRefValue = assoc.getValueIntercept(rootBean);
        Object assocId = provider.convertId(desc, value);
        Property codeProperty = provider.codeProperty(desc);
        Object code = null;
        if (assocId == null && codeProperty != null) {
            code = provider.convertCode(codeProperty, value);
        }
        if (assocId != null) {
            if (originRefValue != null && provider.beanId(originRefValue).equals(assocId)) {
                return originRefValue; //没有变更
            }
            return provider.referenceById(desc, assocId, null); //新关联另外一个对象
        } else if (code != null) {
            Object bean = provider.referenceByCode(desc, codeProperty, code, codeProperty.name());
            //@TODO  是否需要根据编码查询不到而报错
            return bean;
        } else {
            return null;
        }
    }

    private java.lang.reflect.Field getJavaType(Class<?> clazz, String fieldName) {
        if (javaTypeField.containsKey(clazz)) {
            return javaTypeField.get(clazz);
        }
        java.lang.reflect.Field fd = ReflectionUtils.findField(clazz, fieldName);
        javaTypeField.put(clazz, fd);
        if (fd != null) {
            fd.setAccessible(true);
        }
        return fd;
    }
}
