package com.gitssie.openapi.models.layout;

import com.gitssie.openapi.ebean.GeneratorType;
import com.gitssie.openapi.models.user.DataPermissionEnum;
import com.gitssie.openapi.models.xentity.EntityMapping;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.JsonIgnore;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Table(name = "page_layout")
@Entity
public class PageLayout {
    @Id
    @GeneratedValue(generator = GeneratorType.IdWorker)
    private Long id;//                 BIGINT            PRIMARY KEY  AUTO_INCREMENT,
    private String title;
    private String type;
    private String classes;
    private int level;
    private long parent; //父级页面
    private String roleCode; //角色编码,对特定的角色使用角色布局
    @ManyToOne
    private EntityMapping entity;
    @DbJson
    private Component components;
    @Enumerated
    private DataPermissionEnum dataPermission; //数据权限
    @WhenCreated
    private Date createdAt;//  DATETIME          NOT NULL ,
    @WhenModified
    private Date updatedAt;//  DATETIME          NOT NULL

    @Transient
    private String apiKey;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getClasses() {
        return classes;
    }

    public void setClasses(String classes) {
        this.classes = classes;
    }

    public EntityMapping getEntity() {
        return entity;
    }

    public void setEntity(EntityMapping entity) {
        this.entity = entity;
    }

    public Component getComponents() {
        return components;
    }

    public void setComponents(Component components) {
        this.components = components;
    }

    public long getParent() {
        return parent;
    }

    public void setParent(long parent) {
        this.parent = parent;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public DataPermissionEnum getDataPermission() {
        return dataPermission;
    }

    public void setDataPermission(DataPermissionEnum dataPermission) {
        this.dataPermission = dataPermission;
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

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
