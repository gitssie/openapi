package com.gitssie.openapi.form.entity;

import com.gitssie.openapi.models.xentity.EntityField;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public class EntityPatchForm {
    @NotNull
    private Long entityId;
    @NotNull
    @NotEmpty
    private List<Map<String,Object>> fields;

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public EntityPatchForm(Long entityId, List<Map<String, Object>> fields) {
        this.entityId = entityId;
        this.fields = fields;
    }

    public List<Map<String, Object>> getFields() {
        return fields;
    }

    public void setFields(List<Map<String, Object>> fields) {
        this.fields = fields;
    }
}
