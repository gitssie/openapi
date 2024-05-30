package com.gitssie.openapi.models.functree;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gitssie.openapi.models.BasicDomain;
import com.gitssie.openapi.models.user.Permission;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "system_func_tree")
public class FuncTree extends BasicDomain {
    private String label; //名称
    private Integer seqno; //序号
    @ManyToOne
    @JsonIgnore
    private FuncTree parent;
    @OneToOne
    private Permission permission;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<FuncTree> children;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getSeqno() {
        return seqno;
    }

    public void setSeqno(Integer seqno) {
        this.seqno = seqno;
    }

    public FuncTree getParent() {
        return parent;
    }

    public void setParent(FuncTree parent) {
        this.parent = parent;
    }

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    public List<FuncTree> getChildren() {
        return children;
    }

    public void setChildren(List<FuncTree> children) {
        this.children = children;
    }
}
