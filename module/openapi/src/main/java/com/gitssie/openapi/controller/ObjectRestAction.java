package com.gitssie.openapi.controller;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gitssie.openapi.data.Code;
import com.gitssie.openapi.models.BasicDomain;
import com.gitssie.openapi.page.Model;
import com.gitssie.openapi.service.PageService;
import com.gitssie.openapi.service.XObjectService;
import io.ebean.plugin.BeanType;
import io.vavr.control.Either;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class ObjectRestAction {
    @Autowired
    private XObjectService objectService;
    @Autowired
    private PageService pageService;


    @PostMapping("/api/rest/save/{apiKey}")
    public Either<Code, Map<String, Object>> save(@PathVariable String apiKey, @RequestParam(required = false) Long pageId, @RequestBody ObjectNode body) {
        Either<String, BeanType<BasicDomain>> entity = objectService.getBeanTypeIfPresent(apiKey);
        if (entity.isLeft()) {
            return (Either) entity;
        }
        Model model = new Model(apiKey,true);
        return objectService.restSave(entity.get(), body, model);
    }

    @PostMapping("/api/rest/batch/save/{apiKey}")
    public Either<Code, List<Map<String, Object>>> batchSave(@PathVariable String apiKey, @RequestParam(required = false) Long pageId, @RequestBody ArrayNode body) {
        Either<String, BeanType<BasicDomain>> entity = objectService.getBeanTypeIfPresent(apiKey);
        if (entity.isLeft()) {
            return (Either) entity;
        }
        Model model = new Model(apiKey, true);
        return objectService.restBatchSave(entity.get(), body, model);
    }

    @PostMapping("/api/rest/patch/{apiKey}")
    public Either<Code, Map<String, Object>> patch(@PathVariable String apiKey, @RequestParam(required = false) Long pageId, @RequestBody ObjectNode body) {
        Either<String, BeanType<BasicDomain>> entity = objectService.getBeanTypeIfPresent(apiKey);
        if (entity.isLeft()) {
            return (Either) entity;
        }
        Model model = new Model(apiKey, true);
        return objectService.restPatch(entity.get(), body, model);
    }

    @GetMapping("/api/rest/object/{apiKey}/{id}")
    public Either<Code, Map<String, Object>> getDetail(@PathVariable String apiKey, @PathVariable String id, @RequestParam(required = false) Long pageId) {
        Either<String, BeanType<BasicDomain>> entity = objectService.getBeanTypeIfPresent(apiKey);
        if (entity.isLeft()) {
            return (Either) entity;
        }
        Model model = new Model(apiKey, true);
        return objectService.fetchDetail(entity.get(), id, model, null);
    }
}
