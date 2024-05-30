package com.gitssie.openapi.models.user;

import com.gitssie.openapi.models.BasicDomain;
import com.gitssie.openapi.models.tree.PathTree;

import javax.persistence.*;

@Entity
@Table(name = "department")
public class Department extends BasicDomain {
    @ManyToOne
    private Department parent;
    private String departName;

    @Embedded
    private PathTree tree;

    public String getDepartName() {
        return departName;
    }

    public void setDepartName(String departName) {
        this.departName = departName;
    }

    public Department getParent() {
        return parent;
    }

    public void setParent(Department parent) {
        this.parent = parent;
    }

    public PathTree getTree() {
        return tree;
    }

    public void setTree(PathTree tree) {
        this.tree = tree;
    }
}
