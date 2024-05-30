package com.gitssie.openapi.models.user;

import com.gitssie.openapi.models.BasicDomain;
import com.gitssie.openapi.models.functree.FuncTree;
import io.ebean.annotation.DbJson;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "role_permission")
public class RolePermission extends BasicDomain {
    @OneToOne
    private Permission permission;
    @OneToOne
    private FuncTree funcTree; //功能菜单
    @ManyToOne
    private UserRole role;
    @DbJson
    private List<PermissionVerb> verbs; //操作


    public FuncTree getFuncTree() {
        return funcTree;
    }

    public void setFuncTree(FuncTree funcTree) {
        this.funcTree = funcTree;
    }

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public List<PermissionVerb> getVerbs() {
        return verbs;
    }

    public void setVerbs(List<PermissionVerb> verbs) {
        this.verbs = verbs;
    }
}
