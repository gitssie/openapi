package com.gitssie.openapi.form.entity;

import javax.validation.constraints.NotEmpty;

public class EntityReferToForm {
    @NotEmpty
    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
