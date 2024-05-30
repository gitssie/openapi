package com.gitssie.openapi.xentity;

import com.gitssie.openapi.models.xentity.EntityAssoc;
import com.gitssie.openapi.models.xentity.EntityField;
import com.gitssie.openapi.models.xentity.EntityMapping;
import io.ebean.Database;
import io.ebean.config.dbplatform.DbDefaultValue;
import io.ebean.core.type.ScalarType;
import io.ebeaninternal.server.deploy.parse.tenant.XEntity;
import io.ebeaninternal.server.deploy.parse.tenant.XField;
import io.ebeaninternal.server.deploy.parse.tenant.annotation.XDbDefault;
import io.ebeaninternal.server.deploy.parse.tenant.annotation.XIndex;
import io.ebeaninternal.server.deploy.parse.tenant.annotation.XLob;
import io.ebeaninternal.server.deploy.parse.tenant.annotation.XTable;
import io.vavr.control.Either;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.Map;

@Component
public class XEntityService implements ApplicationContextAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(XEntityService.class);
    private ApplicationContext applicationContext;
    private final TypeManagerProvider typeManager;
    private final Database database;

    public XEntityService(TypeManagerProvider typeManagerProvider, Database database) {
        this.typeManager = typeManagerProvider;
        this.database = database;
    }

    /**
     * 获取XEntity
     * 这部分是否需要加个缓存
     *
     * @param apiKey
     * @return
     */
    public Either<String, XEntityCache> getXEntity(String apiKey) {
        return getEntity(apiKey).flatMap(entity -> {
            return entityToBeanEntity(entity);
        });
    }

    public Either<String, XEntityCache> getXEntity(EntityMapping mapping) {
        return entityToBeanEntity(mapping);
    }

    public Either<String, EntityMapping> getEntity(String name) {
        return getEntityMapping(name);
    }

    public Either<String, EntityMapping> getEntityLazy(String name) {
        return getEntitySingle(name);
    }

    public Either<String, Class<?>> getEntityClass(EntityMapping entity) {
        try {
            Class<?> beanClass = applicationContext.getClassLoader().loadClass(entity.getBeanType());
            return Either.right(beanClass);
        } catch (ClassNotFoundException e) {
            LOGGER.error("load class:{} cause", entity.getBeanType(), e);
            return Either.left(e.getMessage());
        }
    }

    public Either<String, XEntityCache> entityToBeanEntity(EntityMapping mapping) {
        Either<String, Class<?>> beanTypeE = getEntityClass(mapping);
        if (beanTypeE.isLeft()) {
            return (Either) beanTypeE;
        }
        XEntity entity = new XEntity(beanTypeE.get());
        entity.setName(mapping.getName());
        entity.setLabel(mapping.getLabel());
        entity.setTenant(mapping.isTenant());
        entity.setCustom(mapping.isCustom());
        entity.setDisabled(mapping.isDisabled());
        entity.setCreateable(mapping.isCreatable());
        entity.setUpdateable(mapping.isUpdatable());
        entity.setQueryable(mapping.isQueryable());
        entity.setFeedEnabled(mapping.isFeedable());

        addAnnotation(entity, mapping);

        Either<String, XField> fieldE;
        XField xField;
        for (EntityField field : mapping.getFields()) {
            fieldE = fieldToBeanField(entity.getBeanType(), field);
            if (fieldE.isLeft()) {
                return (Either) fieldE;
            }
            xField = fieldE.get();
            addAnnotation(xField, field);
            trimXField(xField);
            entity.addField(xField);
        }
        return Either.right(new XEntityCache(entity, mapping.getId()));
    }

    /**
     * 对Field进行修剪
     *
     * @param field
     */
    private void trimXField(XField field) {
        //Boolean类型不能设置长度
        if (Boolean.class.isAssignableFrom(field.getType())) {
            field.setMaxLength(null);
            field.setMinLength(null);
        } else if (Date.class.isAssignableFrom(field.getType())) {
            field.setMaxLength(null);
            field.setMinLength(null);
        }

        if (field.getMaxLength() != null && field.getMaxLength() >= 65535) {
            field.addAnnotation(new XLob());
        }


    }

    public Either<String, XField> fieldToBeanField(Class<?> beanClass, EntityField field) {
        XField xField = new XField(field.getName());

        Either<String, Boolean> fileTypeE = resolveFieldType(beanClass, field, xField);
        if (fileTypeE.isLeft()) {
            return (Either) fileTypeE;
        }

        xField.setNameable(field.isNameable());
        xField.setNullable(field.isNullable());
        xField.setCreateable(field.isCreatable());
        xField.setUpdateable(field.isUpdatable());
        xField.setRequired(field.isRequired());
        xField.setSortable(field.isSortable());
        xField.setMinLength(field.getMinLength());
        xField.setMaxLength(field.getMaxLength());

        return Either.right(xField);
    }

    private void addAnnotation(XEntity entity, EntityMapping mapping) {
        String tableName = NamingUtils.reCamelCase(mapping.getName());
        entity.addAnnotation(new XTable(tableName));

        if (ObjectUtils.isNotEmpty(mapping.getAnnotation())) {
            for (Map<String, Object> map : mapping.getAnnotation()) {
                Either<String, Annotation> either = typeManager.createEntityAnnotation(map);
                if (either.isLeft()) {
                    throw new IllegalArgumentException(either.getLeft());
                }
                entity.addAnnotation(either.get());
            }
        }
    }

    private void addAnnotation(XField xField, EntityField field) {
        if (ObjectUtils.isNotEmpty(field.getAnnotation())) {
            for (Map<String, Object> map : field.getAnnotation()) {
                Either<String, Annotation> either = typeManager.createAnnotation(map);
                if (either.isLeft()) {
                    throw new IllegalArgumentException(either.getLeft());
                }
                xField.addAnnotation(either.get());
            }
        }
        String defaultVal = field.getDefaultVal();
        Class<?> type = xField.getType();
        if (defaultVal != null) {
            //这里设置的仅仅是建表时的默认值
            if (type == Boolean.class || type == boolean.class) {
                //对Boolean类型进行统一解析
                if (StringUtils.equals(defaultVal, "0")) {
                    xField.addAnnotation(new XDbDefault(DbDefaultValue.FALSE));
                } else if (StringUtils.equals(defaultVal, "1")) {
                    xField.addAnnotation(new XDbDefault(DbDefaultValue.TRUE));
                } else {
                    xField.addAnnotation(new XDbDefault(DbDefaultValue.FALSE));
                }
            } else if (type.isPrimitive()) {
                if (type == byte.class || type == short.class ||
                        type == int.class || type == long.class ||
                        type == float.class || type == double.class) {
                    //可以解析成正常的数字
                    if (NumberUtils.isDigits(defaultVal)) {
                        xField.addAnnotation(new XDbDefault(defaultVal));
                    }
                }
            } else if (Number.class.isAssignableFrom(type) && NumberUtils.isDigits(defaultVal)) {
                //可以解析成正常的数字
                if (NumberUtils.isDigits(defaultVal)) {
                    xField.addAnnotation(new XDbDefault(defaultVal));
                }
            } else {
                xField.addAnnotation(new XDbDefault(defaultVal));
            }
        }
        if (field.isUniquable()) {
            xField.addAnnotation(new XIndex("", true));
        }
        if (field.isIndexable()) {
            xField.addAnnotation(new XIndex("", false));
        }

    }

    /**
     * 获取关联的对象的Class
     *
     * @param field
     * @param assoc
     * @return
     */
    protected Either<String, Class<?>> getReferenceBeanClass(EntityField field, EntityAssoc assoc) {
        EntityMapping referEntity = assoc.getRefer();
        if (referEntity == null) {
            return Either.left("属性字段关联的实体不存在:" + field.getName());
        }
        return getEntityClass(referEntity);
    }

    /**
     * 获取属性的类型
     *
     * @param field
     * @return
     */
    protected Either<String, Boolean> resolveFieldType(Class<?> beanClass, EntityField field, XField xField) {
        Field javaField = ReflectionUtils.findField(beanClass, xField.getName());
        if (javaField != null && !javaField.equals(xField.getType()) && !Modifier.isStatic(javaField.getModifiers())) {
            xField.setType(javaField.getType());
            return Either.right(true);
        }
        //引用类型
        if (typeManager.isReference(field.getType())) {
            EntityAssoc assoc = field.getAssoc();
            Class<?> refClass;
            if (assoc == null) {
                if (StringUtils.isNotEmpty(field.getFieldType())) {
                    Either<String, Class<?>> classE = getEntitySingle(field.getFieldType()).flatMap(e -> getEntityClass(e));
                    if (classE.isLeft()) {
                        return (Either) classE;
                    }
                    refClass = classE.get();
                } else {
                    xField.setType(Long.class);
                    return Either.left(xField.getName() + "属性关联的类型不存在");
                }
            } else {
                Either<String, Class<?>> classE = getReferenceBeanClass(field, assoc);
                if (classE.isLeft()) {
                    return (Either) classE;
                }
                refClass = classE.get();
            }
            if (refClass != null) {
                XAssocType assocType = typeManager.toAssocType(field.getType());
                XAssoc xassoc = new XAssoc(field.getFieldType(), assocType);
                xField.setType(refClass);
                xField.addAnnotation(xassoc); //设置好关联的表信息,否则在这里表信息丢失了
            }
            return Either.right(true);
        } else if (StringUtils.equals("join", field.getType())) {
            //EntityAssoc assoc = field.getAssoc();
            throw new UnsupportedOperationException();
        } else { //其余的基本类型
            ScalarType<?> fileType = typeManager.getScalarType(field.getFieldType());
            if (fileType == null) {
                return Either.left(String.format("错误的属性类型:%s(%s)", field.getName(), field.getFieldType()));
            }
            typeManager.handlerScalarType(fileType, xField);
            return Either.right(true);
        }
    }

    /**
     * 从数据库里面加载实体
     *
     * @param name
     * @return
     */
    private Either<String, EntityMapping> getEntityMapping(String name) {
        EntityMapping entityMapping = database.find(EntityMapping.class).where().eq("name", name).findOne();
        if (entityMapping == null) {
            return Either.left(name + "实体不存在");
        }
        entityMapping.getFields(); //加载全部的属性
        entityMapping.getAssocs(); //加载所有的关联信息
        return Either.right(entityMapping);
    }

    private Either<String, EntityMapping> getEntitySingle(String name) {
        EntityMapping entityMapping = database.find(EntityMapping.class).where().eq("name", name).findOne();
        if (entityMapping == null) {
            return Either.left(name + "实体不存在");
        }
        return Either.right(entityMapping);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
