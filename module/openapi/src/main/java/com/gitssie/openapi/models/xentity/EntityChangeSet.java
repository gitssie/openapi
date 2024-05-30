package com.gitssie.openapi.models.xentity;

import com.gitssie.openapi.ebean.GeneratorType;
import io.ebean.annotation.*;
import io.ebean.bean.StaticEntity;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "entity_change_set")
public class EntityChangeSet implements StaticEntity {
    @Id
    @GeneratedValue(generator = GeneratorType.IdWorker)
    private Long id;
    protected Long createdBy;
    protected Long updatedBy;
    @WhenCreated
    protected Date createdAt;
    @WhenModified
    protected Date updatedAt;
    @NotNull
    private Long entityId;
    @NotNull
    private String changeSet;
    @Version
    private Long version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
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

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getChangeSet() {
        return changeSet;
    }

    public void setChangeSet(String changeSet) {
        this.changeSet = changeSet;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
