package com.gitssie.openapi.form.entity;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class EntityFieldCreateForm {
    @NotNull
    private Long entity;
    @NotEmpty
    @Size(max = 255)
    private String name;//        VARCHAR(255)      NOT NULL UNIQUE COMMENT '实体API名称',
    @NotEmpty
    @Size(max = 128)
    private String label;//       VARCHAR(255)      NOT NULL COMMENT '实体显示名称',
    @NotEmpty
    private String type;//        VARCHAR(255)      NOT NULL COMMENT '字段类型',
    private String fieldType;//     VARCHAR(255)    NOT NULL COMMENT '字段类型',
    private Boolean custom = true;//      Boolean           DEFAULT FALSE ,
    private Boolean uniquable;//   Boolean           DEFAULT FALSE COMMENT '是否唯一字段',
    private Boolean encrypted;//   Boolean           DEFAULT FALSE COMMENT '是否加密',
    private Boolean disabled;//    Boolean           DEFAULT FALSE ,
    private Boolean nullable = true;//    Boolean           DEFAULT TRUE,
    private Boolean required;//    Boolean           DEFAULT FALSE COMMENT '是否必填',
    private Boolean creatable = true;//  Boolean           DEFAULT TRUE ,
    private Boolean updatable = true;//  Boolean           DEFAULT TRUE ,
    private Boolean sortable = true;//    Boolean           DEFAULT TRUE COMMENT '是否可以进行排序',
    private Integer minLength;//  INT               DEFAULT NULL ,
    private Integer maxLength;//  INT               DEFAULT NULL,
    private BigDecimal minimum;//     DECIMAL           DEFAULT NULL,
    private BigDecimal maximum;//     DECIMAL           DEFAULT NULL,
    private String pattern;//     VARCHAR(255)      DEFAULT NULL COMMENT '正则表达式验证',
    private String defaultVal;// VARCHAR(255)      DEFAULT NULL

    private List<Map<String,Object>> options; //选项集
    private List<Map<String,Object>> annotation;

    public Long getEntity() {
        return entity;
    }

    public void setEntity(Long entity) {
        this.entity = entity;
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

    public Boolean getCustom() {
        return custom;
    }

    public void setCustom(Boolean custom) {
        this.custom = custom;
    }

    public Boolean isUniquable() {
        return uniquable;
    }

    public void setUniquable(Boolean uniquable) {
        this.uniquable = uniquable;
    }

    public Boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(Boolean encrypted) {
        this.encrypted = encrypted;
    }

    public Boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Boolean isNullable() {
        return nullable;
    }

    public void setNullable(Boolean nullable) {
        this.nullable = nullable;
    }

    public Boolean isRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public Boolean isCreatable() {
        return creatable;
    }

    public void setCreatable(Boolean creatable) {
        this.creatable = creatable;
    }

    public Boolean isUpdatable() {
        return updatable;
    }

    public void setUpdatable(Boolean updatable) {
        this.updatable = updatable;
    }

    public Boolean isSortable() {
        return sortable;
    }

    public void setSortable(Boolean sortable) {
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
}
