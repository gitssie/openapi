package com.gitssie.openapi.form.entity;


import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

public class EntityCreateForm {
    @NotEmpty
    @Size(max = 255)
    private String name;//        VARCHAR(255)      NOT NULL UNIQUE COMMENT '实体API名称',
    @NotEmpty
    @Size(max = 128)
    private String label;//       VARCHAR(255)      NOT NULL COMMENT '实体显示名称',
    private Boolean custom = true;//      BOOLEAN           DEFAULT FALSE ,
    private Boolean disabled = false;//    BOOLEAN           DEFAULT FALSE ,
    private Boolean creatable = true;//  BOOLEAN           DEFAULT TRUE ,
    private Boolean deletable = true;//  BOOLEAN           DEFAULT TRUE ,
    private Boolean updatable = true;//  BOOLEAN           DEFAULT TRUE ,
    private Boolean queryable = true;//   BOOLEAN           DEFAULT TRUE ,
    private Boolean feedable = true;//    BOOLEAN           DEFAULT FALSE ,

    @Valid
    private List<EntityFieldForm> fields;

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

    public Boolean getCustom() {
        return custom;
    }

    public void setCustom(Boolean custom) {
        this.custom = custom;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Boolean getCreatable() {
        return creatable;
    }

    public void setCreatable(Boolean creatable) {
        this.creatable = creatable;
    }

    public Boolean getDeletable() {
        return deletable;
    }

    public void setDeletable(Boolean deletable) {
        this.deletable = deletable;
    }

    public Boolean getUpdatable() {
        return updatable;
    }

    public void setUpdatable(Boolean updatable) {
        this.updatable = updatable;
    }

    public Boolean getQueryable() {
        return queryable;
    }

    public void setQueryable(Boolean queryable) {
        this.queryable = queryable;
    }

    public Boolean getFeedable() {
        return feedable;
    }

    public void setFeedable(Boolean feedable) {
        this.feedable = feedable;
    }

    public List<EntityFieldForm> getFields() {
        return fields;
    }

    public void setFields(List<EntityFieldForm> fields) {
        this.fields = fields;
    }
}
