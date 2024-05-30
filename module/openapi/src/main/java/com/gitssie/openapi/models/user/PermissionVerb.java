package com.gitssie.openapi.models.user;

import java.io.Serializable;

public class PermissionVerb implements Serializable {
    public static final String WILD_WORD = "*";
    private String name;
    private String code;
    private Boolean disabled;

    public PermissionVerb() {
    }

    public PermissionVerb(String name, String code) {
        this.name = name;
        this.code = code;
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

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }
}