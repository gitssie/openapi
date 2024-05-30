package com.gitssie.openapi.models;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.gitssie.openapi.ebean.GeneratorType;
import com.gitssie.openapi.models.user.Department;
import com.gitssie.openapi.models.user.User;
import io.ebean.annotation.*;
import io.ebean.bean.DynamicEntity;
import io.ebean.bean.ElementBean;
import io.ebeaninternal.server.deploy.parse.tenant.annotation.XTenantId;

import javax.persistence.*;
import java.util.Date;
import java.util.Map;

@MappedSuperclass
public abstract class BasicDomain implements TenantSupport, DynamicEntity {
    @Id
    @GeneratedValue(generator = GeneratorType.IdWorker)
    private Long id;
    protected Long entityType;
    @ManyToOne
    @JoinColumn(name = "owner_id")
    protected User owner;
    @ManyToOne
    @JoinColumn(name = "dim_depart")
    protected Department dimDepart;
    @ManyToOne
    @JoinColumn(name = "created_by")
    @WhoCreated
    protected User createdBy;
    @WhenCreated
    protected Date createdAt;
    @WhoModified
    @ManyToOne
    @JoinColumn(name = "updated_by")
    protected User updatedBy;
    @WhenModified
    protected Date updatedAt;
    @DbDefault(value = "0")
    protected int status; //通用状态字段
    @DbDefault(value = "false")
    protected boolean lockStatus;
    @Embedded
    @DbJson
    private ElementBean custom = new ElementBean();
    @Transient
    private final int __slot__ = 0;

    public void put(String key, Object value) {
        custom.put(key, value);
    }

    public void set(String key, Object value) {
        custom.put(key, value);
    }

    public Object get(String key) {
        return custom.get(key);
    }

    public boolean has(String key) {
        return custom.containsKey(key);
    }

    @JsonAnySetter
    public void setProperties(String key, Object value) {
        custom.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getProperties() {
        return custom;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEntityType() {
        return entityType;
    }

    public void setEntityType(Long entityType) {
        this.entityType = entityType;
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

    public boolean isLockStatus() {
        return lockStatus;
    }

    public void setLockStatus(boolean lockStatus) {
        this.lockStatus = lockStatus;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public User getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(User updatedBy) {
        this.updatedBy = updatedBy;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public Department getDimDepart() {
        return dimDepart;
    }

    public void setDimDepart(Department dimDepart) {
        this.dimDepart = dimDepart;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Map<String, Object> getCustom() {
        return custom;
    }

    public Long getTenantId() {
        return (Long) get(XTenantId.NAME);
    }

    public void setTenantId(Long tenantId) {
        set(XTenantId.NAME, tenantId);
    }
}
