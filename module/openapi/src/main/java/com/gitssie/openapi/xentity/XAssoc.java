package com.gitssie.openapi.xentity;

import java.lang.annotation.Annotation;

/**
 * 关联的类型
 */
public class XAssoc implements Annotation {
    private String apiKey;
    private XAssocType assocType;
    private String mappedBy;
    public XAssoc(String apiKey, XAssocType assocType) {
        this.apiKey = apiKey;
        this.assocType = assocType;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return XAssoc.class;
    }

    public String getApiKey() {
        return apiKey;
    }

    public XAssocType getAssocType() {
        return assocType;
    }

    public String getMappedBy() {
        return mappedBy;
    }

    public void setMappedBy(String mappedBy) {
        this.mappedBy = mappedBy;
    }
}
