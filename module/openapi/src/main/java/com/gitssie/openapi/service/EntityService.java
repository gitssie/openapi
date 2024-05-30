package com.gitssie.openapi.service;

import com.gitssie.openapi.data.Code;
import com.gitssie.openapi.ebean.EbeanApi;
import com.gitssie.openapi.form.entity.EntityEditForm;
import com.gitssie.openapi.form.entity.EntityFieldEditForm;
import com.gitssie.openapi.models.xentity.EntityAssoc;
import com.gitssie.openapi.models.xentity.EntityField;
import com.gitssie.openapi.models.xentity.EntityMapping;
import com.gitssie.openapi.utils.Json;
import com.gitssie.openapi.utils.Libs;
import com.gitssie.openapi.xentity.TypeManagerProvider;
import io.ebean.Database;
import io.ebean.bean.EntityBean;
import io.ebean.core.type.ScalarType;
import io.ebean.plugin.BeanType;
import io.vavr.Lazy;
import io.vavr.control.Either;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EntityService {
    private static String[] ENTITY_FIELD_CHANGE_PROPS = new String[]{"name", "maxLength", "uniquable", "indexable", "fieldType"};
    @Autowired
    private Database database;
    @Autowired
    private TypeManagerProvider typeManagerProvider;
    @Autowired
    private EntityRunner entityRunner;
    @Autowired
    private EbeanApi ebeanApi;
    @Qualifier("objectBeanClass")
    @Autowired
    private Lazy<Class<?>> objectBeanClass;

    public Either<Code, EntityMapping> getEntity(Long entityId) {
        EntityMapping entityMapping = database.find(EntityMapping.class, entityId);
        if (entityMapping == null) {
            return Either.left(Code.NOT_FOUND.withMessage("实体不存在"));
        }
        return Either.right(entityMapping);
    }

    public Either<Code, EntityMapping> getEntity(String name) {
        EntityMapping entityMapping = database.find(EntityMapping.class).where().eq("name", name).findOne();
        if (entityMapping == null) {
            return Either.left(Code.NOT_FOUND.withMessage("实体不存在"));
        }
        return Either.right(entityMapping);
    }

    @Transactional
    public Either<Code, EntityMapping> addEntity(EntityMapping entity) {
        int count = database.find(EntityMapping.class).where().eq("name", entity.getName()).findCount();
        if (count > 0) {
            return Either.left(Code.FAILED_PRECONDITION.withMessage("实体已经存在"));
        }
        if (entity.isCustom()) {
            entity.setBeanType(objectBeanClass.get().getName());
        }
        putDefaultField(entity);
        Either<Code, ?> either = verifyEntity(entity);
        if (either.isLeft()) {
            return (Either) either;
        }
        database.save(entity);
        return Either.right(entity);
    }

    /**
     * 创建/保存实体，同时刷新到数据库
     * @param entity
     * @return
     */
    @Transactional
    public Either<Code, EntityMapping> createEntity(EntityMapping entity) {
        if (database.beanState(entity).isNew()) {
            int count = database.find(EntityMapping.class).where().eq("name", entity.getName()).findCount();
            if (count > 0) {
                return Either.left(Code.FAILED_PRECONDITION.withMessage("实体已经存在"));
            }
        }
        if (entity.isCustom()) {
            entity.setBeanType(objectBeanClass.get().getName());
        }
        Either<Code, ?> either = verifyEntity(entity);
        if (either.isLeft()) {
            return (Either) either;
        }
        database.save(entity);
        entityRunner.createTable(entity);
        return Either.right(entity);
    }

    @Transactional
    public Either<Code, Boolean> deploy(EntityMapping entity) {
        return entityRunner.deploy(entity);
    }

    @Transactional
    public Either<Code, String> migrant(EntityMapping entity) {
        return entityRunner.createTable(entity);
    }

    public Either<Code, EntityField> getField(EntityMapping entity, String fieldName) {
        EntityField field = database.find(EntityField.class).where().eq("entity", entity).eq("name", fieldName).findOne();
        if (field == null) {
            return Either.left(Code.NOT_FOUND.withMessage("属性不存在"));
        }
        return Either.right(field);
    }

    public Either<Code, EntityField> getField(Long id) {
        EntityField field = database.find(EntityField.class, id);
        if (field == null) {
            return Either.left(Code.NOT_FOUND.withMessage("属性不存在"));
        }
        return Either.right(field);
    }

    public List<EntityField> getFields(String apiKey, List<String> fields) {
        List<EntityField> result = database.createQuery(EntityField.class)
                .where().eq("entity.name", apiKey)
                .in("name", fields)
                .findList();
        return result;
    }

    @Transactional
    public Either<Code, List<EntityField>> addField(EntityMapping entity, List<EntityField> fields) {
        List<String> newFields = fields.stream().map(e -> e.getName()).collect(Collectors.toList());
        int count = database.find(EntityField.class).where().eq("entity", entity).in("name", newFields).findCount();
        if (count > 0) {
            return Either.left(Code.FAILED_PRECONDITION.withMessage("实体属性已经存在"));
        }
        for (EntityField field : fields) {
            Either<Code, Boolean> fieldE = verifyField(entity, field);
            if (fieldE.isLeft()) {
                return (Either) fieldE;
            }
            field.setEntity(entity);
        }
        entity.getFields().addAll(fields);
        database.saveAll(fields);
        return Either.right(fields);
    }

    private Either<Code, Boolean> verifyEntity(EntityMapping entity) {
        if (ObjectUtils.isNotEmpty(entity.getAnnotation())) {
            for (Map<String, Object> map : entity.getAnnotation()) {
                Either<String, ?> either = typeManagerProvider.createEntityAnnotation(map);
                if (either.isLeft()) {
                    return (Either) either;
                }
            }
        }
        List<EntityField> fields = entity.getFields();
        if (ObjectUtils.isNotEmpty(fields)) {
            for (EntityField field : fields) {
                Either<Code, Boolean> either = verifyField(entity, field);
                if (either.isLeft()) {
                    return either;
                } else {
                    field.setEntity(entity);
                }
            }
        }
        return Either.right(true);
    }

    private Either<Code, Boolean> verifyField(EntityMapping entity, EntityField field) {
        if (typeManagerProvider.isReference(field.getType())) {//关联类型,默认为多对一
            Either<Code, EntityAssoc> either = createEntityAssoc(entity, field, field.getFieldType());
            if (either.isLeft()) {
                return (Either) either;
            } else {
                field.setAssoc(either.get());
            }
        } else {
            //判断实体类型,基本数据类型
            ScalarType<?> fileType = typeManagerProvider.getScalarType(field.getFieldType());
            if (fileType == null) {
                return Either.left(Code.INVALID_ARGUMENT.withMessage("属性类型错误,不存在类型:" + field.getFieldType()));
            }
        }
        if (ObjectUtils.isNotEmpty(field.getAnnotation())) {
            for (Map<String, Object> map : field.getAnnotation()) {
                Either<String, ?> either = typeManagerProvider.createAnnotation(map);
                if (either.isLeft()) {
                    return (Either) either;
                }
            }
        }
        return Either.right(true);
    }

    @Transactional
    public Either<String, Boolean> addFieldIfNotExists(EntityMapping entity, EntityField field) {
        int count = database.find(EntityMapping.class).where().eq("entity", entity.getId()).eq("name", field.getName()).findCount();
        if (count > 0) {
            return Either.right(false);
        }
        //判断实体类型
        field.setEntity(entity);
        database.save(field);
        return Either.right(true);
    }

    private void putDefaultField(EntityMapping entity) {
        List<EntityField> fieldList = entity.getFields();
        EntityField field = new EntityField("id", "主键", "OPrimary", "Long", entity);
        field.setAnnotation(Libs.toMapList("@", "GeneratedValue", "generator", "IdWorker"));
        fieldList.add(field);
        fieldList.add(new EntityField("entityType", "业务类型", "OSelect", "Long", entity));
        fieldList.add(new EntityField("lockStatus", "锁定状态", "OCheckbox", "Integer", entity));

        //审批状态
        field = new EntityField("status", "审批状态", "OSelect", "Integer", entity);
        field.setOptions(Libs.toMapList("code", "approvalStatus"));
        fieldList.add(field);

        fieldList.add(new EntityField("owner", "所有人", "ORef", "user", entity));
        fieldList.add(new EntityField("dimDepart", "部门", "ORef", "department", entity));
        fieldList.add(new EntityField("createdBy", "创建人", "ORef", "user", entity));
        fieldList.add(new EntityField("updatedBy", "修改人", "ORef", "user", entity));
        fieldList.add(new EntityField("createdAt", "创建日期", "ODatetime", "DateTime", entity));
        fieldList.add(new EntityField("updatedAt", "修改日期", "ODatetime", "DateTime", entity));

    }

    public Class<?> getTypeClass(String type) {
        ScalarType<?> scalarType = typeManagerProvider.getScalarType(type);
        if (scalarType != null) {
            return scalarType.getType();
        }
        return null;
    }

    /**
     * 修改实体属性
     *
     * @param field
     * @param updateForm
     * @return
     * @TODO 这里仅仅是修改了属性, 未修改表结构
     */
    @Transactional
    public Either<Code, EntityField> patchField(EntityField field, EntityFieldEditForm updateForm) {
        Map<String, Object> fieldMap = Json.fromJson(Json.toJson(updateForm), Map.class);
        fieldMap.put("id", field.getId());
        fieldMap.put("name", field.getName());
        BeanType<EntityField> beanType = database.pluginApi().beanType(EntityField.class);
        changeField(beanType, field, fieldMap);
        applyChangeToDB(field.getEntity(), field);
        return Either.right(field);
    }

    /**
     * 修改实体属性
     *
     * @param entity
     * @param fields
     * @return
     */
    @Transactional
    public Either<Code, List<EntityField>> patchField(EntityMapping entity, List<Map<String, Object>> fields) {
        List<EntityField> entityFields = entity.getFields();
        Map<String, EntityField> propsMap = entityFields.stream().collect(Collectors.toMap(e -> e.getName(), e -> e));
        BeanType<EntityField> beanType = database.pluginApi().beanType(EntityField.class);
        List<EntityField> patchFields = new ArrayList<>();
        for (Map<String, Object> fieldMap : fields) {
            EntityField oldField = propsMap.get(fieldMap.get("name"));
            EntityField field;
            if (oldField != null) {
                field = changeField(beanType, oldField, fieldMap);
            } else {
                field = changeField(beanType, new EntityField(), fieldMap);
            }
            Either<Code, Boolean> fieldE = verifyField(entity, field);
            if (fieldE.isLeft()) {
                return (Either) fieldE;
            }
            //new Field
            if (oldField == null) {
                field.setEntity(entity);
                entityFields.add(field);
            }
            patchFields.add(field);
        }
        entity.setFields(entityFields);
        database.update(entity);
        return Either.right(patchFields);
    }

    private void applyChangeToDB(EntityMapping entity, EntityField field) {
        EntityBean bean = (EntityBean) field;
        Set<String> props = bean._ebean_getIntercept().getDirtyPropertyNames();
        boolean changeKeyProps = false;
        for (String prop : ENTITY_FIELD_CHANGE_PROPS) {
            if (props.contains(prop)) {
                changeKeyProps = true;
                break;
            }
        }
        database.update(field);
        if (changeKeyProps) {
            //entityRunner.alterTable(entity);
        }
    }

    private EntityField changeField(BeanType<EntityField> beanType, EntityField oldField, Map<String, Object> field) {
        ebeanApi.copy(beanType, field, oldField);
        return oldField;
    }


    public List<EntityField> getFields(List<Long> ids) {
        return database.find(EntityField.class).where().in("id", ids).findList();
    }

    @Transactional
    public Either<Code, Integer> deleteFields(List<EntityField> fields) {
        EntityMapping entity = null;
        for (EntityField field : fields) {
            if (entity == null) {
                entity = field.getEntity();
            } else if (!entity.equals(field.getEntity())) {
                return Either.left(Code.INVALID_ARGUMENT.withMessage("实体属性错误"));
            }
        }
        int rowCount = database.deleteAll(fields);
//        entityRunner.alterTable(entity);
        return Either.right(rowCount);
    }

    public Either<Code, EntityAssoc> createEntityAssoc(EntityMapping entity, EntityField field, String apiKey) {
        return getEntity(apiKey).map(refer -> {
            EntityAssoc assoc = new EntityAssoc();
            assoc.setEntity(entity);
            assoc.setField(field);
            assoc.setRefer(refer);
            return assoc;
        });
    }

    public Either<Code, EntityMapping> patchEntity(EntityMapping entity, EntityEditForm updateForm) {
        Map<String, Object> source = Json.fromJson(Json.toJson(updateForm), Map.class);
        ebeanApi.copy(source, entity);
        database.update(entity);
        return Either.right(entity);
    }


}
