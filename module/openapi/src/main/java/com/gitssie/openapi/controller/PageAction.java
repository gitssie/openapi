package com.gitssie.openapi.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gitssie.openapi.data.Code;
import com.gitssie.openapi.ebean.EbeanApi;
import com.gitssie.openapi.models.layout.PageLayout;
import com.gitssie.openapi.models.layout.PageQuery;
import com.gitssie.openapi.models.xentity.EntityAssoc;
import com.gitssie.openapi.models.xentity.EntityMapping;
import com.gitssie.openapi.page.Model;
import com.gitssie.openapi.service.PageService;
import com.gitssie.openapi.service.XObjectService;
import com.gitssie.openapi.utils.TypeUtils;
import com.gitssie.openapi.web.query.QueryForm;
import com.google.common.collect.Maps;
import io.ebean.plugin.BeanType;
import io.vavr.control.Either;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class PageAction {
    @Autowired
    private PageService pageService;
    @Autowired
    private EbeanApi ebeanApi;
    @Autowired
    private XObjectService objectService;

    @GetMapping("/api/layout/select/{apiKey}")
    public Either<Code, Map<String, Object>> getPageSelect(@PathVariable String apiKey) {
        return pageService.getPageSelect(apiKey).map(e -> {
            pageService.resolvePageComponent(e);
            Map<String, Object> page = ebeanApi.toJSON(e);
            EntityMapping mapping = e.getEntity();
            if (mapping != null) {
                page.put("entity", pageService.toEntityMap(mapping));
            }
            return page;
        });
    }

    @GetMapping("/api/layout/page")
    public Either<Code, Map<String, Object>> getPageLayout(Authentication authentication, @RequestParam String apiKey,
                                                           @RequestParam String type,
                                                           @RequestParam(required = false, defaultValue = "0") long p) {
        return pageService.getPageLayout(authentication, apiKey, type, p).map(e -> {
            Map<String, Object> page = ebeanApi.toJSON(e);
            return page;
        });
    }

    @GetMapping("/api/layout/entity/{id}")
    public Either<Code, Map<String, Object>> getEntity(@PathVariable String id) {
        Either<Code, EntityMapping> mE;
        if (StringUtils.isNumeric(id)) {
            mE = pageService.getEntity(Long.valueOf(id));
        } else {
            String apiKey = id;
            mE = pageService.getEntity(apiKey);
        }
        return mE.map(e -> {
            Map<String, Object> entity = ebeanApi.toJSON(e);
            entity.put("fields", e.getFields());
            if (e.getAssocs() != null) {
                Map<String, Object> assocsMap = Maps.newHashMap();
                for (EntityAssoc assoc : e.getAssocs()) {
                    Map<String, Object> assocs = Maps.newHashMap();
                    assocs.put("id", assoc.getId());
                    assocs.put("entityId", e.getId());
                    assocs.put("entityApiKey", e.getName());
                    assocs.put("referId", assoc.getRefer().getId());
                    assocs.put("referApiKey", assoc.getRefer().getName());
                    assocs.put("fieldId", assoc.getField().getId());
                    assocs.put("name", assoc.getField().getName());
                    assocsMap.put(assoc.getField().getName(), assocs);
                }
                entity.put("relations", assocsMap);
            }
            return entity;
        });
    }

    @GetMapping("/api/layout/detail/{apiKey}")
    public Either<Code, Map<String, Object>> getPageDetail(@PathVariable String apiKey) {
        return pageService.getPageDetail(apiKey).map(e -> {
            pageService.resolvePageComponent(e);
            Map<String, Object> page = ebeanApi.toJSON(e);
            EntityMapping mapping = e.getEntity();
            if (mapping != null) {
                page.put("entity", pageService.toEntityMap(mapping));
            }
            return page;
        });
    }

    @GetMapping("/api/layout/create/{apiKey}")
    public Either<Code, Map<String, Object>> getPageCreate(@PathVariable String apiKey) {
        return pageService.getPageCreate(apiKey).map(e -> {
            pageService.resolvePageComponent(e);
            Map<String, Object> page = ebeanApi.toJSON(e);
            EntityMapping mapping = e.getEntity();
            if (mapping != null) {
                page.put("entity", pageService.toEntityMap(mapping));
            }
            return page;
        });
    }

    @GetMapping("/api/layout/patch/{apiKey}")
    public Either<Code, Map<String, Object>> getPagePatch(@PathVariable String apiKey) {
        return pageService.getPagePatch(apiKey).map(e -> {
            pageService.resolvePageComponent(e);
            Map<String, Object> page = ebeanApi.toJSON(e);
            EntityMapping mapping = e.getEntity();
            if (mapping != null) {
                page.put("entity", pageService.toEntityMap(mapping));
            }
            return page;
        });
    }

    /**
     * 查询页面布局信息
     *
     * @param queryMap
     * @return
     */
    @PostMapping("/api/layout/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Either<Code, Page<Map<String, Object>>> query(@RequestBody @Valid QueryForm queryMap) {
        Model model = new Model("pageLayout", "title", "type", "level", "parent", "roleCode", "dataPermission", "entity.label");
        return objectService.queryMap(ebeanApi.desc(PageLayout.class), model, null, queryMap.getQuery(), queryMap.getPageable());
    }

    /**
     * 获取详情
     *
     * @param id
     * @return
     */
    @GetMapping("/api/layout/view/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Either<Code, Map<String, Object>> getDetail(@PathVariable String id) {
        BeanType<PageLayout> desc = ebeanApi.desc(PageLayout.class);
        Model model = new Model("pageLayout", "title", "type", "level", "parent", "roleCode", "dataPermission", "components", "entity.label");
        return objectService.fetchDetail(desc, id, model, null);
    }

    @PutMapping("/api/layout/create")
    @PreAuthorize("hasRole('ADMIN')")
    public Either<Code, Map<String, Object>> create(@RequestBody ObjectNode body) {
        BeanType<PageLayout> desc = ebeanApi.desc(PageLayout.class);
        Model model = new Model("pageLayout", "title", "type", "level", "parent", "roleCode", "dataPermission", "components", "entity");
        return objectService.create(desc, body, model);
    }

    /**
     * 数据修改
     *
     * @param id
     * @param body
     * @return
     */
    @PatchMapping("/api/layout/patch/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Either<Code, Map<String, Object>> patch(@PathVariable String id, @RequestBody ObjectNode body) {
        BeanType<PageLayout> desc = ebeanApi.desc(PageLayout.class);
        Model model = new Model("pageLayout", "title", "type", "level", "parent", "roleCode", "dataPermission", "components", "entity");
        return objectService.patch(desc, id, body, model, null);
    }

    /**
     * 数据删除
     *
     * @return
     */
    @DeleteMapping("/api/layout/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public Either<Code, Integer> batchDelete(@RequestBody List<String> ids) {
        List<PageLayout> beans = objectService.findAll(PageLayout.class, ids);
        int rowCount = objectService.deleteAll(beans);
        return Either.right(rowCount);
    }

    @GetMapping("/api/layout/list/{apiKey}")
    public Either<Code, Map<String, Object>> getListPage(@PathVariable String apiKey) {
        return pageService.getPageList(apiKey).map(e -> {
            pageService.resolvePageComponent(e);
            Map<String, Object> page = ebeanApi.toJSON(e);
            EntityMapping mapping = e.getEntity();
            if (mapping != null) {
                page.put("entity", pageService.toEntityMap(mapping));
            }
            return page;
        });
    }

    @GetMapping("/api/layout/page/{id}")
    public Either<Code, Map<String, Object>> getPage(@PathVariable Long id) {
        return pageService.getPage(null, id).map(e -> {
            pageService.resolvePageComponent(e);
            Map<String, Object> page = ebeanApi.toJSON(e);
            /*
            EntityMapping mapping = e.getEntity();
            if (mapping != null) {
                page.put("entity", pageService.toEntityMap(mapping));
            }*/
            return page;
        });
    }

    @PostMapping("/api/layout/query")
    public Either<Code, List<Map<String, Object>>> getPageQuery(Authentication authentication, @RequestBody Map<String, Object> queryMap) {
        int maxSize = 100;
        Integer size = TypeUtils.castToInt(queryMap.get("size"), maxSize);
        Long pageId = TypeUtils.castToLong(queryMap.get("pageId"));
        if (pageId == null) {
            return Either.right(new LinkedList<>());
        }
        List<PageQuery> data = pageService.listPageQuery(authentication, pageId, Math.min(size, maxSize));
        List<Map<String, Object>> result = data.stream().map(p -> {
            Map<String, Object> res = p.getQueryMap();
            if (res == null) {
                res = new HashMap<>();
            }
            res.put("id", p.getId());
            res.put("name", p.getName());
            res.put("pageId", p.getPageId());
            res.put("status", p.getStatus());
            if (p.getOwner() != null) {
                res.put("owner", p.getOwner().getId());
            }
            return res;
        }).collect(Collectors.toList());
        return Either.right(result);
    }

    @PutMapping("/api/layout/query")
    public Either<Code, Map<String, Object>> savePageQuery(Authentication authentication, @Valid @RequestBody QueryForm queryMap) {
        String name = TypeUtils.castToString(queryMap.get("name"));
        Long pageId = TypeUtils.castToLong(queryMap.get("pageId"));
        queryMap.clearMap();
        Either<Code, PageQuery> pageQuery = pageService.savePageQuery(authentication, name, pageId, queryMap);
        return pageQuery.map(e -> {
            Map<String, Object> map = Maps.newHashMapWithExpectedSize(2);
            map.put("id", e.getId());
            return map;
        });
    }

    @DeleteMapping("/api/layout/query")
    public Either<Code, Long> deletePageQuery(Authentication authentication, @RequestBody Map<String, Object> data) {
        Long id = TypeUtils.castToLong(data.get("id"));
        return pageService.deletePageQuery(authentication, id).map(e -> e.getId());
    }
}
