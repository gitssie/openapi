package com.gitssie.openapi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.gitssie.openapi.data.Code;
import com.gitssie.openapi.ebean.EbeanApi;
import com.gitssie.openapi.form.entity.*;
import com.gitssie.openapi.models.xentity.EntityField;
import com.gitssie.openapi.models.xentity.EntityMapping;
import com.gitssie.openapi.service.EntityService;
import com.gitssie.openapi.service.XObjectService;
import com.gitssie.openapi.utils.Json;
import com.gitssie.openapi.web.query.QueryForm;
import com.google.common.collect.Lists;
import io.vavr.control.Either;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class EntityAction {
    @Autowired
    private XObjectService objectService;
    @Autowired
    private EntityService entityService;
    @Autowired
    private EbeanApi ebeanApi;

    @GetMapping("/api/object/entity/{id}")
    public Either<Code, EntityMapping> getEntity(@PathVariable Long id) {
        return entityService.getEntity(id);
    }

    @GetMapping("/api/object/desc/{name}")
    public Either<Code, JsonNode> desc(@PathVariable String name) {
        return entityService.getEntity(name).map(bean -> {
            Map<String, Object> map = ebeanApi.toJSON(bean);
            List<Map<String, Object>> fields = ebeanApi.toJSON(bean.getFields());
            map.put("fields", fields);
            map.remove("id");
            for (Map<String, Object> field : fields) {
                field.remove("id");
                field.remove("entity");
                field.remove("assoc");
            }
            return Json.toJson(map);
        });
    }

    @PostMapping("/api/object/deploy")
    public Either<Code, Boolean> deploy(@RequestBody Map<String, String> body) {
        String name = StringUtils.defaultString(body.get("name"), null);
        return entityService.getEntity(name).flatMap(entity -> {
            return entityService.deploy(entity);
        });
    }

    @PostMapping("/api/object/migrant")
    public Either<Code, String> migrant(@RequestBody Map<String, String> body) {
        String name = StringUtils.defaultString(body.get("name"), null);
        return entityService.getEntity(name).flatMap(entity -> {
            return entityService.migrant(entity);
        });
    }

    @PostMapping("/api/object/entity")
    public Either<Code, Page<EntityMapping>> query(@RequestBody @Valid QueryForm queryForm) {
        return objectService.query(EntityMapping.class, queryForm.getQuery(), queryForm.getPageable());
    }

    @PostMapping("/api/object/entity/fields")
    public Either<Code, Page<EntityField>> queryFields(@RequestBody @Valid QueryForm queryForm) {
        return objectService.query(EntityField.class, queryForm.getQuery(), queryForm.getPageable());
    }

    @PatchMapping("/api/object/entity/{id}")
    public Either<Code, EntityMapping> patchEntity(@PathVariable Long id, @Valid @RequestBody EntityEditForm updateForm) {
        return entityService.getEntity(id).flatMap(entity -> {
            return entityService.patchEntity(entity, updateForm);
        });
    }

    @PutMapping("/api/object/entity")
    public Either<Code, EntityMapping> addEntity(@Valid @RequestBody EntityCreateForm entityCreateForm) {
        EntityMapping entity = new EntityMapping();
        BeanUtils.copyProperties(entityCreateForm, entity);
        List<EntityField> fields = Lists.newArrayList();
        if (ObjectUtils.isNotEmpty(entityCreateForm.getFields())) {
            for (EntityFieldForm fieldForm : entityCreateForm.getFields()) {
                EntityField field = new EntityField();
                BeanUtils.copyProperties(fieldForm, field);
                fields.add(field);
            }
        }
        entity.setFields(fields);
        return entityService.addEntity(entity);
    }

    @GetMapping("/api/object/entity/field/{id}")
    public Either<Code, EntityField> getField(@PathVariable Long id) {
        return entityService.getField(id);
    }

    @PatchMapping("/api/object/entity/field/{id}")
    public Either<Code, EntityField> patchSingleField(@PathVariable Long id, @Valid @RequestBody EntityFieldEditForm updateForm) {
        return entityService.getField(id).flatMap(field -> {
            return entityService.patchField(field, updateForm);
        });
    }

    @PutMapping("/api/object/entity/field/{entityId}")
    public Either<Code, EntityField> addField(@PathVariable Long entityId, @Valid @RequestBody EntityFieldCreateForm updateForm) {
        List<EntityField> fields = Lists.newArrayList();
        EntityField field = new EntityField();
        BeanUtils.copyProperties(updateForm, field);
        fields.add(field);
        return entityService.getEntity(updateForm.getEntity()).flatMap(entity -> {
            return entityService.addField(entity, fields).map(list -> list.get(0));
        });
    }

    @PutMapping("/api/object/entity/field")
    public Either<Code, List<EntityField>> addField(@Valid @RequestBody EntityUpdateForm updateForm) {
        List<EntityField> fields = Lists.newArrayList();
        if (ObjectUtils.isNotEmpty(updateForm.getFields())) {
            for (EntityFieldForm fieldForm : updateForm.getFields()) {
                EntityField field = new EntityField();
                BeanUtils.copyProperties(fieldForm, field);
                fields.add(field);
            }
        }
        return entityService.getEntity(updateForm.getEntityId()).flatMap(entity -> {
            return entityService.addField(entity, fields);
        });
    }

    @DeleteMapping("/api/object/entity/fields")
    public Either<Code, Integer> batchDelete(@RequestBody List<Long> ids, Authentication authentication) {
        List<EntityField> fields = entityService.getFields(ids);
        if (fields.isEmpty()) {
            return Either.right(0);
        }
        return entityService.deleteFields(fields);
    }

    @PatchMapping("/api/object/entity/field")
    public Either<Code, List<EntityField>> patchField(@Valid @RequestBody EntityPatchForm updateForm) {
        return entityService.getEntity(updateForm.getEntityId()).flatMap(entity -> {
            return entityService.patchField(entity, updateForm.getFields());
        });
    }

}
