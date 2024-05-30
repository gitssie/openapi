package com.gitssie.openapi.models.xentity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gitssie.openapi.ebean.GeneratorType;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import io.ebean.bean.StaticEntity;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "entity_field")
public class EntityField implements StaticEntity {
    @Id
    @GeneratedValue(generator = GeneratorType.IdWorker)
    private Long id;//          BIGINT            PRIMARY KEY  AUTO_INCREMENT,
    private String name;//        VARCHAR(255)      NOT NULL UNIQUE COMMENT '字段API名称',
    private String label;//       VARCHAR(255)      NOT NULL COMMENT '字段显示名称',
    private String type;//        VARCHAR(255)      NOT NULL COMMENT '字段类型',
    private String fieldType;//     VARCHAR(255)    NOT NULL COMMENT '字段类型',
    private boolean custom;//      boolean           DEFAULT FALSE ,
    private boolean uniquable;//   boolean           DEFAULT FALSE COMMENT '是否唯一字段',
    private boolean indexable;
    private boolean encrypted;//   boolean           DEFAULT FALSE COMMENT '是否加密',
    private boolean disabled;//    boolean           DEFAULT FALSE ,
    private boolean nullable = true;//    boolean           DEFAULT TRUE,
    private boolean nameable = false; //  boolean           DEFAULT TRUE,
    private boolean required;//    boolean           DEFAULT FALSE COMMENT '是否必填',
    private boolean creatable = true;//  boolean           DEFAULT TRUE ,
    private boolean updatable = true;//  boolean           DEFAULT TRUE ,
    private boolean sortable = true;//    boolean           DEFAULT TRUE COMMENT '是否可以进行排序',
    private Integer minLength;//  INT               DEFAULT NULL ,
    private Integer maxLength;//  INT               DEFAULT NULL,
    private BigDecimal minimum;//     DECIMAL           DEFAULT NULL,
    private BigDecimal maximum;//     DECIMAL           DEFAULT NULL,
    private String pattern;//     VARCHAR(255)      DEFAULT NULL COMMENT '正则表达式验证',
    private String defaultVal;// VARCHAR(255)      DEFAULT NULL

    @DbJson
    private List<Map<String,Object>> annotation; //元标注
    @DbJson
    private List<Map<String,Object>> options; //选项集
    @ManyToOne
    @JsonIgnore
    private EntityMapping entity;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "field")
    @JsonIgnore
    private EntityAssoc assoc;//    BIGINT            DEFAULT NULL COMMENT ' 关联实体',

    @WhenCreated
    private Date createdAt;//  DATETIME          NOT NULL ,
    @WhenModified
    private Date updatedAt;//  DATETIME          NOT NULL

    public EntityField() {
    }

    public EntityField(String name, String label, String type, String fieldType, EntityMapping entity) {
        this.name = name;
        this.label = label;
        this.type = type;
        this.fieldType = fieldType;
        this.entity = entity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public boolean isCustom() {
        return custom;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }

    public boolean isUniquable() {
        return uniquable;
    }

    public void setUniquable(boolean uniquable) {
        this.uniquable = uniquable;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public boolean isIndexable() {
        return indexable;
    }

    public void setIndexable(boolean indexable) {
        this.indexable = indexable;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isCreatable() {
        return creatable;
    }

    public void setCreatable(boolean creatable) {
        this.creatable = creatable;
    }

    public boolean isUpdatable() {
        return updatable;
    }

    public void setUpdatable(boolean updatable) {
        this.updatable = updatable;
    }

    public boolean isNameable() {
        return nameable;
    }

    public void setNameable(boolean nameable) {
        this.nameable = nameable;
    }

    public boolean isSortable() {
        return sortable;
    }

    public void setSortable(boolean sortable) {
        this.sortable = sortable;
    }

    public Integer getMinLength() {
        return minLength;
    }

    public void setMinLength(Integer minLength) {
        this.minLength = minLength;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public BigDecimal getMinimum() {
        return minimum;
    }

    public void setMinimum(BigDecimal minimum) {
        this.minimum = minimum;
    }

    public BigDecimal getMaximum() {
        return maximum;
    }

    public void setMaximum(BigDecimal maximum) {
        this.maximum = maximum;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getDefaultVal() {
        return defaultVal;
    }

    public void setDefaultVal(String defaultVal) {
        this.defaultVal = defaultVal;
    }

    public EntityMapping getEntity() {
        return entity;
    }

    public void setEntity(EntityMapping entity) {
        this.entity = entity;
    }

    public EntityAssoc getAssoc() {
        return assoc;
    }

    public void setAssoc(EntityAssoc assoc) {
        this.assoc = assoc;
    }

    public List<Map<String, Object>> getAnnotation() {
        return annotation;
    }

    public void setAnnotation(List<Map<String, Object>> annotation) {
        this.annotation = annotation;
    }

    public List<Map<String, Object>> getOptions() {
        return options;
    }

    public void setOptions(List<Map<String, Object>> options) {
        this.options = options;
    }

    public boolean isPrimary(){
        return StringUtils.equalsIgnoreCase(type,"Primary");
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
