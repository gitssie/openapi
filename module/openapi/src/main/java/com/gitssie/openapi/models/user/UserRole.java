package com.gitssie.openapi.models.user;

import com.gitssie.openapi.models.BasicDomain;
import io.ebean.annotation.Index;
import io.ebean.annotation.NotNull;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "user_role")
public class UserRole extends BasicDomain {
    private String name;//角色名称 name

    @Index
    private String code;//角色编码 code

    @Enumerated
    @NotNull
    private DataPermissionEnum dataPermission = DataPermissionEnum.SELF; //数据权限

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "role")
    private List<RolePermission> permissions;

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

    public DataPermissionEnum getDataPermission() {
        return dataPermission;
    }

    public void setDataPermission(DataPermissionEnum dataPermission) {
        this.dataPermission = dataPermission;
    }

    public List<RolePermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<RolePermission> permissions) {
        this.permissions = permissions;
    }
}
