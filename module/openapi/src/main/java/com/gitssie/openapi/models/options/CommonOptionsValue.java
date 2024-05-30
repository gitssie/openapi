package com.gitssie.openapi.models.options;

import com.gitssie.openapi.models.BasicDomain;
import io.ebean.annotation.DbDefault;
import io.ebean.annotation.Index;
import io.ebean.annotation.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "common_options_value")
public class CommonOptionsValue extends BasicDomain {
    private String name;
    @Column(length = 100)
    @Index
    @NotNull
    private String path;
    @DbDefault("1")
    private int level;

    @ManyToOne
    private CommonOptions optionType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CommonOptions getOptionType() {
        return optionType;
    }

    public void setOptionType(CommonOptions optionType) {
        this.optionType = optionType;
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
}
