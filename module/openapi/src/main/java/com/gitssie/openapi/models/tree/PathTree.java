package com.gitssie.openapi.models.tree;

import io.ebean.annotation.DbDefault;
import io.ebean.annotation.Index;
import io.ebean.annotation.NotNull;

import javax.persistence.*;

@Embeddable
public class PathTree {
    @Column(length = 100)
    @Index
    @NotNull
    private String path;
    @DbDefault("0")
    private int level;

    public PathTree() {
    }

    public PathTree(String path, int level) {
        this.path = path;
        this.level = level;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String parentPath() {
        if (path == null) {
            return null;
        }
        int i = path.lastIndexOf('.');
        if (i > 0) {
            return path.substring(0, i);
        } else {
            return null;
        }
    }
}
