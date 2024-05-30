package com.gitssie.openapi.models.layout;

import com.gitssie.openapi.ebean.GeneratorType;
import com.gitssie.openapi.models.user.User;
import io.ebean.annotation.*;

import javax.persistence.*;
import java.util.Date;
import java.util.Map;

@Table(name = "page_query")
@Entity
public class PageQuery {
    @Id
    @GeneratedValue(generator = GeneratorType.IdWorker)
    private Long id;
    private String name;
    private Long pageId;
    @DbJson
    private Map<String, Object> queryMap;

    @DbDefault(value = "1")
    protected int status; //0禁用,1启用,2默认
    @ManyToOne
    @JoinColumn(name = "owner_id")
    protected User owner;
    @ManyToOne
    @JoinColumn(name = "created_by")
    @WhoCreated
    protected User createdBy;
    @WhenCreated
    private Date createdAt;//  DATETIME          NOT NULL ,
    @WhenModified
    private Date updatedAt;//  DATETIME          NOT NULL

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

    public Long getPageId() {
        return pageId;
    }

    public void setPageId(Long pageId) {
        this.pageId = pageId;
    }

    public Map<String, Object> getQueryMap() {
        return queryMap;
    }

    public void setQueryMap(Map<String, Object> queryMap) {
        this.queryMap = queryMap;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
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
