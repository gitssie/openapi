package com.gitssie.openapi.models.auth;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.ConfigAttribute;

public class VerbAttribute implements ConfigAttribute {
    private String apiKey;
    private String code;

    private String attribute;

    public VerbAttribute(String apiKey, String code) {
        this.apiKey = apiKey;
        this.code = code;
        this.attribute = StringUtils.join(new String[]{apiKey, code}, ":");
    }

    public VerbAttribute clone(String apiKey) {
        return new VerbAttribute(apiKey, this.code);
    }

    @Override
    public String getAttribute() {
        return attribute;
    }

    public String getApiKey() {
        return apiKey;
    }


    public String getCode() {
        return code;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        } else if (this == other) {
            return true;
        } else if (other instanceof String) {
            return toString().equals(other);
        } else {
            return toString().equals(other.toString());
        }
    }

    @Override
    public int hashCode() {
        return this.apiKey.hashCode() + this.code.hashCode();
    }

    @Override
    public String toString() {
        return attribute;
    }
}
