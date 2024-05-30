package com.gitssie.openapi.models.functree;


import io.ebean.annotation.WhenCreated;
import io.ebean.bean.DynamicEntity;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "system_func_tree_tenant")
public class FuncTreeTenant implements DynamicEntity {
    @Id
    @GeneratedValue(generator = "IdWorker")
    private Long id;
    private Long tenantId;
    @ManyToOne
    private FuncTree funcTree;
    @WhenCreated
    private Date createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public FuncTree getFuncTree() {
        return funcTree;
    }

    public void setFuncTree(FuncTree funcTree) {
        this.funcTree = funcTree;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public void put(String key, Object value) {

    }

    @Override
    public void set(String key, Object value) {

    }

    @Override
    public Object get(String key) {
        return null;
    }

    @Override
    public boolean has(String key) {
        return false;
    }
}
