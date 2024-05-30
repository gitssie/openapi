package com.gitssie.openapi.form.entity;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

public class EntityUpdateForm {
    @NotNull
    private Long entityId;
    @Valid
    @NotNull
    @NotEmpty
    private List<EntityFieldForm> fields;

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public List<EntityFieldForm> getFields() {
        return fields;
    }

    public void setFields(List<EntityFieldForm> fields) {
        this.fields = fields;
    }
}
