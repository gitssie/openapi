package com.gitssie.openapi.page;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gitssie.openapi.service.Provider;
import com.gitssie.openapi.utils.TypeUtils;
import com.gitssie.openapi.rule.Rules;
import com.google.common.collect.Maps;
import io.ebean.bean.EntityBean;
import io.ebean.plugin.BeanType;
import io.ebean.plugin.Property;
import io.ebeaninternal.server.deploy.BeanProperty;
import io.ebeaninternal.server.deploy.BeanPropertyAssoc;
import io.ebeaninternal.server.deploy.BeanPropertyAssocMany;
import io.ebeaninternal.server.deploy.BeanPropertyAssocOne;
import io.vavr.Value;
import io.vavr.control.Either;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ModelNodeConversion {
    protected static final Logger LOGGER = LoggerFactory.getLogger(ModelNodeConversion.class);
    private final Map<Class<?>, java.lang.reflect.Field> javaTypeField = new ConcurrentHashMap<>();

    @Autowired
    private ObjectMapper mapper;

    private FieldError validate(Value<Context> context, Object value, Field field, BeanProperty property) {
        return field.validate(context, value, "", property.name(), null);
    }

    public Either<Errors, EntityBean> copy(NeedContext context, Provider provider, BeanType<?> desc, Model model, final ObjectNode source, final EntityBean bean) throws Exception {
        MapBindingResult errors = new MapBindingResult(Collections.emptyMap(), "");
        return copy(context, provider, desc, model, source, bean, errors);
    }

    public Either<Errors, EntityBean> copy(NeedContext context, Provider provider, BeanType<?> desc, Model model, final ObjectNode source, final EntityBean bean, MapBindingResult errors) throws Exception {
        if (model == null || model.isIncludeAllLoadedProps()) {
            return copy(context, provider, desc, source, bean);
        }
        for (Field field : model.getFields()) {
            String name = field.getName();
            BeanProperty property = (BeanProperty) desc.property(name);
            if (property == null) {
                continue;
            }
            if (property.isScalar()) {
                copyScalar(context, provider, property, source, bean, field, errors);
            } else if (property instanceof BeanPropertyAssoc) {
                JsonNode value = source.get(name);
                copyAssoc(context, provider, (BeanPropertyAssoc) property, null, value, bean, errors);
            }
        }
        if (errors.hasErrors()) {
            return Either.left(errors);
        }
        //关联的属性
        for (Map.Entry<String, Model> entry : model.assoc.entrySet()) {
            String name = entry.getKey();
            BeanProperty property = (BeanProperty) desc.property(name);
            if (property == null) {
                continue;
            } else if (!source.has(name)) {
                continue;
            }
            JsonNode value = source.get(name);
            if (property instanceof BeanPropertyAssoc) {
                copyAssoc(context, provider, (BeanPropertyAssoc) property, entry.getValue(), value, bean, errors);
            }
            if (errors.hasErrors()) {
                return Either.left(errors);
            }
        }
        return Either.right(bean);
    }

    private void copyAssoc(NeedContext context, Provider provider, BeanPropertyAssoc property, Model model, JsonNode value, EntityBean bean, MapBindingResult errors) throws Exception {
        if (property instanceof BeanPropertyAssocOne) {
            Object assocValue = assocOne(context, provider, model, property, bean, value, errors);
            property.setValueIntercept(bean, assocValue); //@TODO 需要考虑不能为空的问题

        } else if (property instanceof BeanPropertyAssocMany) {
            BeanPropertyAssocMany assoc = (BeanPropertyAssocMany) property;
            Object assocValue = assocMany(context, provider, model, assoc, bean, value, errors);
            property.setValueIntercept(bean, assocValue); //@TODO 需要考虑不能为空的问题
        } else if (property.isEmbedded()) {

        }
    }


    private void copyScalar(NeedContext context, Provider provider, BeanProperty property, ObjectNode source, EntityBean bean, Field field, MapBindingResult errors) throws Exception {
        String name = property.name();
        JsonNode value = null;
        Object newValue;
        FieldError fieldError = null;
        try {
            if (source.has(name)) {
                value = source.get(name);
                if (property.isScalar()) {
                    newValue = copyScalar(property, value, bean);
                    fieldError = validate(context.context, newValue, field, property);
                } else if (property instanceof BeanPropertyAssocOne) {
                    BeanPropertyAssoc assoc = (BeanPropertyAssoc) property;
                    Object assocValue = assocRef(provider, assoc, bean, value, errors);
                    assoc.setValueIntercept(bean, assocValue);
                }
            } else if (field.getValue() != null && property.isScalar()) { //设置默认值配置
                newValue = Rules.toValue(context.context, property.type(), field.getValue(), source);
                property.setValueIntercept(bean, newValue);
            } else if (!context.isUpdate) {
                fieldError = validate(context.context, null, field, property); //判断属性是否可以为null
            }
            if (fieldError != null) {
                errors.addError(fieldError);
            }
        } catch (MismatchedInputException ex) {
            LOGGER.error("convert field:{} value:{},to type:{}", name, value, property.type(), ex);
            fieldError = new FieldError(errors.getObjectName(), name, value, true, new String[]{"mismatched.input.type"}, null, "参数类型不匹配");
            errors.addError(fieldError);
        } catch (Exception e) {
            throw e;
        }
    }

    public void setScalar(Property property, Object bean, Object value) {
        BeanProperty b = (BeanProperty) property;
        if (b != null && b.isScalar()) {
            value = propertyConvert(b, value);
            b.setValueIntercept((EntityBean) bean, value);
        }
    }

    /**
     * 设置普通属性
     *
     * @param property
     * @param source
     * @param bean
     */
    protected Object copyScalar(BeanProperty property, final JsonNode source, final EntityBean bean) throws Exception {
        Object value = null;
        if (source != null) {
            //如果是集合类的JSON
            if (Collection.class.isAssignableFrom(property.type())) {
                java.lang.reflect.Field fd = getJavaType(property.scalarType().getClass(), "deserType");
                if (fd != null) {
                    JavaType javaType = (JavaType) fd.get(property.scalarType());
                    value = mapper.treeToValue(source, javaType);
                } else {
                    value = mapper.treeToValue(source, property.type());
                }
            } else if (Date.class.isAssignableFrom(property.type())) {
                value = TypeUtils.castToDate(source.asText());
            } else {
                value = mapper.treeToValue(source, property.type());
            }
            value = propertyConvert(property, value);
        }
        property.setValueIntercept(bean, value);
        return value;
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

    /**
     * 全部属性复制
     * 使用属性类型或者Annotation进行验证
     *
     * @param context
     * @param provider
     * @param desc
     * @param source
     * @param bean
     * @return
     */
    public Either<Errors, EntityBean> copy(NeedContext context, Provider provider, BeanType<?> desc, final ObjectNode source, final EntityBean bean) throws Exception {
        MapBindingResult errors = new MapBindingResult(Collections.emptyMap(), "");
        for (Property prop : desc.allProperties()) {
            if (source.has(prop.name())) {
                copyField(context, provider, source, bean, prop, errors);
            }
        }
        if (errors.hasErrors()) {
            return Either.left(errors);
        }
        return Either.right(bean);
    }

    private void copyField(NeedContext context, Provider provider, ObjectNode source, EntityBean bean, Property prop, MapBindingResult errors) throws Exception {
        String name = prop.name();
        BeanProperty property = (BeanProperty) prop;
        JsonNode value = source.get(prop.name());
        try {
            if (property.isScalar()) {
                copyScalar(property, value, bean);
            } else if (property instanceof BeanPropertyAssocOne) {
                BeanPropertyAssoc assoc = (BeanPropertyAssoc) property;
                Object assocValue = assocOne(context, provider, null, assoc, bean, value, errors);
                property.setValueIntercept(bean, assocValue); //@TODO 需要考虑不能为空的问题
            } else if (property instanceof BeanPropertyAssocMany) {
                BeanPropertyAssocMany assoc = (BeanPropertyAssocMany) property;
                Object assocValue = assocMany(context, provider, null, assoc, bean, value, errors);
                property.setValueIntercept(bean, assocValue); //@TODO 需要考虑不能为空的问题
            } else if (property.isEmbedded()) {

            }
        } catch (MismatchedInputException ex) {
            LOGGER.error("convert field:{} value:{},to type:{}", name, value, property.type(), ex);
            FieldError fieldError = new FieldError(errors.getObjectName(), name, value, true, new String[]{"mismatched.input.type"}, null, "参数类型不匹配");
            errors.addError(fieldError);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * 根据属性的类型转换
     *
     * @param property
     * @param value
     * @return
     */
    private Object propertyConvert(BeanProperty property, Object value) {
        return property.convert(value);
    }

    private Collection<?> assocMany(NeedContext context, Provider provider, Model model, BeanPropertyAssoc assoc, EntityBean rootBean, JsonNode value, MapBindingResult errors) throws Exception {
        Collection beanList = (Collection<EntityBean>) assoc.getValueIntercept(rootBean);
        if (value == null || !(value instanceof ArrayNode)) {
            return beanList;
        }
        ArrayNode sourceList = (ArrayNode) value;
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
        Map<Object, ObjectNode> sourceIdMap = Maps.newLinkedHashMap();
        Map<Object, ObjectNode> sourceCodeMap = Maps.newLinkedHashMap();
        List<ObjectNode> added = new LinkedList<>();
        for (JsonNode map : sourceList) {
            if (!(map instanceof ObjectNode)) {
                continue; //@TODO 是否需要抛出异常处理？
            }
            Object id = provider.convertId(desc, map.get(idProperty.name()));
            if (id != null) {
                sourceIdMap.put(id, (ObjectNode) map);
                continue;
            } else if (codeProperty != null) {
                Object code = provider.convertCode(codeProperty, map.get(codeProperty.name()));
                if (ObjectUtils.isNotEmpty(code)) {
                    sourceCodeMap.put(code, (ObjectNode) map);
                    continue;
                }
            }
            added.add((ObjectNode) map);
        }

        //查找更新、删除的
        Iterator<EntityBean> iterator = beanList.iterator();
        Object id, code;
        while (iterator.hasNext()) {
            EntityBean bean = iterator.next();
            id = idProperty.value(bean);
            ObjectNode source = sourceIdMap.remove(id);
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
                copy(context, provider, desc, model, source, bean, errors);//需要修改的数据，设置值
                if (errors.hasErrors()) {
                    return beanList;
                }
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
            for (Map.Entry<Object, ObjectNode> codeValue : sourceCodeMap.entrySet()) {
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
                    FieldError fieldError = new FieldError(errors.getObjectName(), assoc.name(), codeValue.getKey(), true, new String[]{"mismatched.input.assoc"}, null, String.format("编码%s数据已关联其它对象", codeValue.getKey()));
                    errors.addError(fieldError);
                    return beanList;
                }
            }
        }
        //新增的数据
        if (mappedBy != null && isSave) {
            for (ObjectNode source : added) {
                EntityBean bean = (EntityBean) desc.createBean();
                copy(context, provider, desc, model, source, bean);//设置值
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


    private Object assocOne(NeedContext context, Provider provider, Model model, BeanPropertyAssoc assoc, EntityBean rootBean, JsonNode value, MapBindingResult errors) throws Exception {
        boolean isSave = assoc.cascadeInfo().isSave();
        EntityBean bean = (EntityBean) assoc.getValueIntercept(rootBean);
        if (isSave) {//级联
            if (value == null) {
                if (bean != null && assoc.cascadeInfo().isDelete()) {
                    context.needDelete.add(bean);
                }
                return null;
            } else if (value.isEmpty()) {
                return bean;
            } else if (!(value instanceof ObjectNode)) {
                return bean;
            }
            if (bean == null) {
                bean = (EntityBean) assoc.targetDescriptor().createBean();
            }
            copy(context, provider, assoc.targetDescriptor(), model, (ObjectNode) value, bean, errors);
            return bean;
        } else if (value == null) {
            return null; //取消对象关联
        } else {//关联
            return assocRef(provider, assoc, rootBean, value, errors);
        }
    }

    private Object assocRef(Provider provider, BeanPropertyAssoc assoc, EntityBean rootBean, JsonNode value, MapBindingResult errors) {
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
            Object bean = provider.referenceById(desc, assocId, null); //新关联另外一个对象
            if (bean == null) {
                FieldError fieldError = new FieldError(errors.getObjectName(), assoc.name(), assocId, true, new String[]{"notFound.id"}, null, String.format("ID:%s数据不存在", assocId));
                errors.addError(fieldError);
            }
            return bean;
        } else if (code != null) {
            Object bean = provider.referenceByCode(desc, codeProperty, code, codeProperty.name());
            if (bean == null) {
                FieldError fieldError = new FieldError(errors.getObjectName(), assoc.name(), code, true, new String[]{"notFound.code"}, null, String.format("编码%s数据不存在", code));
                errors.addError(fieldError);
            }
            return bean;
        } else {
            return null;
        }
    }


    public Either<Errors, Object> assocValue(NeedContext context, Provider provider, BeanPropertyAssoc assoc, EntityBean rootBean, JsonNode value) {
        try {
            MapBindingResult errors = new MapBindingResult(Collections.emptyMap(), "");
            Object valueBean = assocOne(context, provider, null, assoc, rootBean, value, errors);
            assoc.setValueIntercept(rootBean, valueBean);
            if (errors.hasErrors()) {
                return Either.left(errors);
            }
            return Either.right(valueBean);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
