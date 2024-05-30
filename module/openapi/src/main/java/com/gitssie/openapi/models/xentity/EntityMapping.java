package com.gitssie.openapi.models.xentity;

import com.gitssie.openapi.ebean.GeneratorType;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.NotNull;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import io.ebean.bean.StaticEntity;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "entity_mapping")
public class EntityMapping implements StaticEntity {
    @Id
    @GeneratedValue(generator = GeneratorType.IdWorker)
    private Long id;//          BIGINT            PRIMARY KEY  AUTO_INCREMENT,
    @NotNull
    private String name;//        VARCHAR(255)      NOT NULL UNIQUE COMMENT '实体API名称',
    @NotNull
    private String label;//       VARCHAR(255)      NOT NULL COMMENT '实体显示名称',
    private String beanType;//   VARCHAR(255)      DEFAULT NULL COMMENT '实体Class类型',
    private String superName;//     VARCHAR(255)      DEFAULT NULL COMMENT '继承的父实体名称',
    private boolean tenant = false; //是为租户实体
    private boolean custom = false;//      boolean           DEFAULT FALSE ,
    private boolean disabled = false;//    boolean           DEFAULT FALSE ,
    private boolean creatable = true;//  boolean           DEFAULT TRUE ,
    private boolean deletable = true;//  boolean           DEFAULT TRUE ,
    private boolean updatable = true;//  boolean           DEFAULT TRUE ,
    private boolean queryable = true;//   boolean           DEFAULT TRUE ,
    private boolean feedable = true;//    boolean           DEFAULT FALSE ,
    private Long createdBy;//  BIGINT            DEFAULT NULL ,
    @WhenCreated
    private Date createdAt;//  DATETIME          NOT NULL ,
    private Long updatedBy;//  BIGINT            DEFAULT NULL ,
    @WhenModified
    private Date updatedAt;//  DATETIME          NOT NULL

    @OneToMany(cascade = CascadeType.ALL,mappedBy="entity")
    private List<EntityAssoc> assocs;
    @OneToMany(cascade = CascadeType.ALL,mappedBy="entity")
    private List<EntityField> fields;

    @DbJson
    private List<Map<String,Object>> annotation; //元标注

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

    public String getBeanType() {
        return beanType;
    }

    public void setBeanType(String beanType) {
        this.beanType = beanType;
    }

    public String getSuperName() {
        return superName;
    }

    public void setSuperName(String superName) {
        this.superName = superName;
    }

    public boolean isTenant() {
        return tenant;
    }

    public void setTenant(boolean tenant) {
        this.tenant = tenant;
    }

    public boolean isCustom() {
        return custom;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isCreatable() {
        return creatable;
    }

    public void setCreatable(boolean creatable) {
        this.creatable = creatable;
    }

    public boolean isDeletable() {
        return deletable;
    }

    public void setDeletable(boolean deletable) {
        this.deletable = deletable;
    }

    public boolean isUpdatable() {
        return updatable;
    }

    public void setUpdatable(boolean updatable) {
        this.updatable = updatable;
    }

    public boolean isQueryable() {
        return queryable;
    }

    public void setQueryable(boolean queryable) {
        this.queryable = queryable;
    }

    public boolean isFeedable() {
        return feedable;
    }

    public void setFeedable(boolean feedable) {
        this.feedable = feedable;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<EntityAssoc> getAssocs() {
        return assocs;
    }

    public void setAssocs(List<EntityAssoc> assocs) {
        this.assocs = assocs;
    }

    public List<EntityField> getFields() {
        return fields;
    }

    public void setFields(List<EntityField> fields) {
        this.fields = fields;
    }

    public List<Map<String, Object>> getAnnotation() {
        return annotation;
    }

    public void setAnnotation(List<Map<String, Object>> annotation) {
        this.annotation = annotation;
    }
}
