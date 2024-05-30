package com.gitssie.openapi.models.user;

import com.gitssie.openapi.models.BasicDomain;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.Index;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.List;

/**
 * 资源之间有父子关系,例如客户->联系人
 * 1、如果分配了父级资源的CRUD权限,那么子级资源的CRUD权限也自动获得
 * 2、如果子级资源有父级资源不具备的权限,则需要明确获得子级资源的权限操作编码
 */
@Entity
@Table(name = "permission")
public class Permission extends BasicDomain {
    private String name; //资源名称

    @Index(unique = true)
    private String code; //资源编码
    @ManyToOne
    private Permission parent; //父级资源
    private Long resource; //资源ID
    private String url; //请求地址

    @DbJson
    private List<PermissionVerb> verbs; //权限操作

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Permission getParent() {
        return parent;
    }

    public void setParent(Permission parent) {
        this.parent = parent;
    }

    public Long getResource() {
        return resource;
    }

    public void setResource(Long resource) {
        this.resource = resource;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<PermissionVerb> getVerbs() {
        return verbs;
    }

    public void setVerbs(List<PermissionVerb> verbs) {
        this.verbs = verbs;
    }
}
