package com.gitssie.openapi.models.auth;

import com.gitssie.openapi.models.BasicDomain;
import com.gitssie.openapi.models.user.Permission;
import io.ebean.annotation.Length;
import io.ebean.annotation.TenantId;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "url_verb")
public class URLVerb extends BasicDomain {
    @ManyToOne
    private Permission permission;
    @Length(64)
    private String name;
    @Length(32)
    private String code; //@TODO 是否支持匹配多个动作?
    @Length(64)
    private String groupName; //动作分组,便于管理
    @Length(16)
    private String method;
    private String antPath;

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }

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

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getAntPath() {
        return antPath;
    }

    public void setAntPath(String antPath) {
        this.antPath = antPath;
    }
}
