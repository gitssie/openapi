package com.gitssie.openapi.service;

import com.gitssie.openapi.auth.RolePermissionEvaluator;
import com.gitssie.openapi.data.Code;
import com.gitssie.openapi.ebean.EbeanApi;
import com.gitssie.openapi.models.auth.SecurityUser;
import com.gitssie.openapi.models.layout.Component;
import com.gitssie.openapi.models.layout.PageLayout;
import com.gitssie.openapi.models.layout.PageQuery;
import com.gitssie.openapi.models.user.User;
import com.gitssie.openapi.models.xentity.EntityField;
import com.gitssie.openapi.models.xentity.EntityMapping;
import com.gitssie.openapi.page.*;
import com.gitssie.openapi.utils.Json;
import com.gitssie.openapi.rule.Rules;
import com.gitssie.openapi.web.query.QueryForm;
import com.gitssie.openapi.xentity.XEntityManager;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.ebean.Expr;
import io.ebean.Expression;
import io.ebean.ExpressionList;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class PageService {
    private final EbeanApi ebeanApi;
    private final EntityService entityService;
    private final XEntityManager entityManager;
    private final Provider provider;
    private final RolePermissionEvaluator permission;
    private final PageView pageView;

    public PageService(EbeanApi ebeanApi,
                       EntityService entityService,
                       XEntityManager entityManager,
                       Provider provider,
                       RolePermissionEvaluator permission,
                       Map<String, LazyValueResolver> valueResolvers,
                       @Value("${ebean.templatePath:}") String templatePath) {
        this.ebeanApi = ebeanApi;
        this.entityService = entityService;
        this.entityManager = entityManager;
        this.provider = provider;
        this.permission = permission;
        this.pageView = new PageView(valueResolvers, templatePath);
    }

    @Transactional(readOnly = true)
    public Either<Code, PageLayout> getPage(String apiKey, Long id) {
        PageLayout page = ebeanApi.find(PageLayout.class, id);
        if (page != null) {
            page.setApiKey(apiKey);
        }
        return Option.of(page).toEither(Code.NOT_FOUND);
    }


    private PageLayout getPage(EntityMapping entity, int level) {
        Expression e1 = Expr.eq("entity", entity);
        Expression e2 = Expr.eq("level", level);
        return ebeanApi.find(PageLayout.class, Expr.and(e1, e2));
    }

    private List<PageLayout> getPage(Long entityId, String type) {
        Expression e1 = Expr.eq("entity.id", entityId);
        Expression e2 = Expr.eq("type", type);
        return ebeanApi.list(PageLayout.class, Expr.and(e1, e2));
    }

    @Transactional(readOnly = true)
    public Either<Code, EntityMapping> getEntity(Long entityId) {
        return entityService.getEntity(entityId).map(e -> {
            e.getFields();
            e.getAssocs();
            return e;
        });
    }

    @Transactional(readOnly = true)
    public Either<Code, EntityMapping> getEntity(String apiKey) {
        return entityService.getEntity(apiKey).map(e -> {
            e.getFields();
            e.getAssocs();
            return e;
        });
    }

    public Either<Code, Model> toModel(String apiKey, String type, Long pageId) {
        Either<Code, Long> pageIdE = Either.right(pageId);
        return pageIdE.flatMap(e -> {
            if (e == null) {
                return getPage(apiKey, type).map(page -> toModel(page));
            } else {
                return getPage(apiKey, pageId).map(page -> toModel(page));
            }
        });
    }

    public Either<Code, Model> toModel(String apiKey, Long pageId) {
        Either<Code, PageLayout> pageE = getPage(apiKey, pageId);
        return pageE.map(page -> toModel(page));
    }

    public Model toModel(PageLayout page) {
        ComponentRender render = new ComponentRender();
        return render.parse(page, page.getComponents());
    }

    public Either<Code, AggreModel> getAggre(String apiKey, String funcName) {
        return pageView.getAggre(apiKey, funcName).toEither(() -> toErrCode(apiKey, funcName));//统计查询表单
    }

    public Either<Code, Model> getQuery(String apiKey, String funcName) {
        return pageView.getQuery(apiKey, funcName).toEither(() -> toErrCode(apiKey, funcName));//查询表单
    }

    public Either<Code, Model> getTable(String apiKey, String funcName) {
        return pageView.getTable(apiKey, funcName).toEither(() -> toErrCode(apiKey, funcName));//列表返回数据
    }

    public Either<Code, Model> getView(String apiKey, String funcName) {
        return pageView.getView(apiKey, funcName).toEither(() -> toErrCode(apiKey, funcName));//列表返回数据
    }

    public Either<Code, Model> getCreate(String apiKey, String funcName) {
        return pageView.getCreate(apiKey, funcName).toEither(() -> toErrCode(apiKey, funcName));//列表返回数据
    }

    public Either<Code, Model> getEdit(String apiKey, String funcName) {
        return pageView.getEdit(apiKey, funcName).toEither(() -> toErrCode(apiKey, funcName));//列表返回数据
    }

    public Either<Code, Model> getForm(String apiKey, String funcName) {
        return pageView.getForm(apiKey, funcName).toEither(() -> toErrCode(apiKey, funcName));//列表返回数据
    }

    public Either<Code, Function> getFunction(String apiKey, String funcName) {
        return pageView.getFunction(apiKey, funcName).toEither(() -> toErrCode(apiKey, funcName));//获取页面函数
    }

    public Either<Code, Object> toView(String apiKey, String funcName, Object... args) {
        return toView(Option.none(), apiKey, funcName, args);
    }

    public Either<Code, Object> toView(io.vavr.Value<Context> contextOpt, String apiKey, String funcName, Object... args) {
        return getFunction(apiKey, funcName).map(e -> Rules.toValue(contextOpt, e, args));
    }

    private Code toErrCode(String apiKey, String funcName) {
        return Code.NOT_FOUND.withMessage(String.format("JS view not found %s-%s", apiKey, funcName));
    }

    @Transactional(readOnly = true)
    public Either<Code, PageLayout> getPage(String apiKey, String type) {
        return entityManager.getEntityIfPresent(apiKey).mapLeft(e -> Code.INVALID_ARGUMENT.withMessage(e)).flatMap(entity -> {
            List<PageLayout> pageList = getPage(entity.entityId, type);
            if (pageList.isEmpty()) {
                return Either.left(Code.notFound());
            }
            for (PageLayout page : pageList) {
                page.setApiKey(apiKey);
            }
            return Either.right(pageList.get(0));
        });
    }

    @Transactional(readOnly = true)
    public Either<Code, PageLayout> getPageLayout(Authentication authentication, String apiKey, String type, long parent) {
        return entityManager.getEntityIfPresent(apiKey).mapLeft(e -> Code.INVALID_ARGUMENT.withMessage(e)).flatMap(entity -> {
            List<PageLayout> pageList = getPage(entity.entityId, type);
            if (pageList.isEmpty()) {
                return Either.left(Code.notFound());
            }
            PageLayout page = null;
            for (PageLayout layout : pageList) {
                layout.setApiKey(apiKey);
                //case1.如果是为特定的角色分配的页面
                if (StringUtils.isNotEmpty(layout.getRoleCode())) {
                    if (SecurityUser.contains(authentication, layout.getRoleCode())) {
                        page = layout;
                        break;
                    } else {
                        continue;
                    }
                }
                if (page == null || layout.getParent() == 0) {
                    page = layout;
                }
                if (parent == layout.getParent()) {//如果是子页面
                    page = layout;
                    break;
                }
            }
            ComponentRender render = new ComponentRender();
            render.layout(this, provider, entity, page.getComponents());
            return Either.right(page);
        });
    }

    public Map<String, Map<String, Object>> getLayoutFields(String apiKey, List<String> fields) {
        List<EntityField> fieldList = entityService.getFields(apiKey, fields);
        Map<String, Map<String, Object>> result = Maps.newHashMapWithExpectedSize(fieldList.size());
        for (EntityField entityField : fieldList) {
            Map<String, Object> field = new HashMap<>();
            toFieldColumn(field, entityField);
            result.put(entityField.getName(), field);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Either<Code, PageLayout> getPageSelect(String name) {
        return entityService.getEntity(name).map(entityMapping -> {
            entityMapping.getFields(); //load fields
            PageLayout page = new PageLayout();
            page.setEntity(entityMapping);
            generatePageSelectComponent(page, entityMapping);
            return page;
        });
    }

    @Transactional(readOnly = true)
    public Either<Code, PageLayout> getPageDetail(String name) {
        return entityService.getEntity(name).map(entityMapping -> {
            entityMapping.getFields(); //load fields
            PageLayout page = getPage(entityMapping, 1);
            if (page == null) {
                page = new PageLayout();
                page.setEntity(entityMapping);
                generatePageDetailComponent(page, entityMapping);
            }
            return page;
        });
    }

    @Transactional(readOnly = true)
    public Either<Code, PageLayout> getPageCreate(String name) {
        return entityService.getEntity(name).map(entityMapping -> {
            entityMapping.getFields(); //load fields
            PageLayout page = new PageLayout();
            page.setEntity(entityMapping);
            generatePageCreateComponent(page, entityMapping);
            return page;
        });
    }

    @Transactional(readOnly = true)
    public Either<Code, PageLayout> getPageList(String name) {
        return entityService.getEntity(name).map(entityMapping -> {
            entityMapping.getFields(); //load fields
            PageLayout page = new PageLayout();
            page.setEntity(entityMapping);
            generatePageCreateComponent(page, entityMapping);
            return page;
        });
    }

    @Transactional(readOnly = true)
    public Either<Code, PageLayout> getPagePatch(String name) {
        return entityService.getEntity(name).map(entityMapping -> {
            entityMapping.getFields(); //load fields
            PageLayout page = new PageLayout();
            page.setEntity(entityMapping);
            generatePagePatchComponent(page, entityMapping);
            return page;
        });
    }


    private void generatePageSelectComponent(PageLayout page, EntityMapping entityMapping) {
        Component component = new Component();
        component.setType("QueryTable");
        //component.setId(component.getType());


        page.setComponents(component);

        Component query = new Component();
        query.setType("Query");
        //query.setId(query.getType());
        query.put("columns", Lists.newArrayList());

        Component actionButton = new Component();
        actionButton.setType("ActionButton");
        actionButton.put("buttons", Lists.newArrayList());

        query.setComponents(Lists.newArrayList(actionButton));
        component.setComponents(Lists.newArrayList(query));

        Component table = new Component();
        table.setType("Table");
        //table.setId(table.getType());
        table.put("columns", Lists.newArrayList());

        component.getComponents().add(table);
    }

    private void generatePageDetailComponent(PageLayout page, EntityMapping entityMapping) {
        Component component = new Component();
        component.setType("Detail");
        Component view = new Component();
        view.setType("View");

        component.setComponents(Lists.newArrayList(view));
        page.setComponents(component);
    }

    private void generatePageCreateComponent(PageLayout page, EntityMapping entityMapping) {
        Component component = new Component();
        List<Map<String, Object>> columns = Lists.newArrayList();
        component.setType("Form");
        component.put("columns", columns);

        for (EntityField field : entityMapping.getFields()) {
            if (field.isPrimary()) {
                continue;
            } else if (!field.isCreatable()) {
                continue;
            }
            Map<String, Object> column = toFieldColumn(field);
            column.put("required", field.isRequired());
            columns.add(column);
        }

        page.setComponents(component);
    }

    private void generatePagePatchComponent(PageLayout page, EntityMapping entityMapping) {
        Component component = new Component();
        List<Map<String, Object>> columns = Lists.newArrayList();
        component.setType("Form");
        component.put("columns", columns);

        for (EntityField field : entityMapping.getFields()) {
            if (field.isPrimary()) {
                continue;
            } else if (!field.isUpdatable()) {
                continue;
            }
            Map<String, Object> column = toFieldColumn(field);
            column.put("required", field.isRequired());
            columns.add(column);
        }

        page.setComponents(component);
    }

    public void resolvePageComponent(PageLayout page) {
        EntityMapping mapping = page.getEntity();
        if (mapping == null || ObjectUtils.isEmpty(page.getComponents())) {
            return;
        }
        Map<String, EntityField> fieldMap = Maps.newHashMap();
        for (EntityField field : mapping.getFields()) {
            fieldMap.put(field.getName(), field);
        }

        resolvePageComponent(page.getComponents(), mapping, fieldMap);

    }

    private void resolvePageComponent(Component component, EntityMapping mapping, Map<String, EntityField> fieldMap) {
        if (StringUtils.equalsIgnoreCase(component.getType(), "Table") ||
                StringUtils.equalsIgnoreCase(component.getType(), "View")) {
            List<Object> columns = (List<Object>) component.get("columns");
            if (ObjectUtils.isEmpty(columns)) {
                columns = new ArrayList<>();
                component.put("columns", columns);

                ebeanApi.sort(mapping.getFields(), "id asc");

                for (EntityField field : mapping.getFields()) {
                    columns.add(toFieldColumn(field));
                }
            } else {
                for (Object column : columns) {
                    if (column == null) continue;
                    if (column instanceof Map) {
                        Map map = (Map) column;
                        EntityField field = fieldMap.get(String.valueOf(map.get("field")));
                        if (field != null) {
                            toFieldColumn(map, field);
                        }
                    }
                }
            }
        } else if (StringUtils.equalsIgnoreCase(component.getType(), "Query")) {
            List<Object> columns = (List<Object>) component.get("columns");
            if (ObjectUtils.isNotEmpty(columns)) {
                for (Object column : columns) {
                    if (column == null) continue;
                    if (column instanceof Map) {
                        Map map = (Map) column;
                        EntityField field = fieldMap.get(String.valueOf(map.get("field")));
                        if (field != null) {
                            toFieldColumn(map, field);
                        }
                    }
                }
            }
        }

        if (ObjectUtils.isNotEmpty(component.getComponents())) {
            for (Component subComponent : component.getComponents()) {
                resolvePageComponent(subComponent, mapping, fieldMap);
            }
        }
    }

    private void toFieldColumn(Map column, EntityField field) {
        column.put("id", field.getId());
        column.put("field", field.getName());
        if (!column.containsKey("label")) column.put("label", field.getLabel());
        if (!column.containsKey("type")) column.put("type", field.getType());
        column.put("fieldType", field.getFieldType());
        //column.put("format", field.getType().toLowerCase());
        column.put("options", field.getOptions());
        column.put("sortable", field.isSortable());
        column.put("nameable", field.isNameable());
        column.put("updatable", field.isUpdatable());
        column.put("minLength", field.getMinLength());
        column.put("maxLength", field.getMaxLength());
        column.put("minimum", field.getMinimum());
        column.put("maximum", field.getMaximum());
        column.put("pattern", field.getPattern());
        column.put("defaultVal", field.getDefaultVal());
    }

    private Map<String, Object> toFieldColumn(EntityField field) {
        Map<String, Object> column = Maps.newLinkedHashMap();
        toFieldColumn(column, field);
        return column;
    }

    public Map<String, Object> toEntityMap(EntityMapping mapping) {
        Map<String, Object> entityE = Maps.newHashMap();
        entityE.put("name", mapping.getName());
        entityE.put("label", mapping.getLabel());
        entityE.put("id", mapping.getId());
        for (EntityField field : mapping.getFields()) {
            if (field.isNameable()) {
                entityE.put("nameField", field.getName());
                break;
            }
        }
        return entityE;
    }

    public List<PageQuery> listPageQuery(Authentication authentication, Long pageId, int size) {
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        ExpressionList<PageQuery> where = provider.createQuery(PageQuery.class).where();
        if (permission.isAdmin(authentication)) {
            where.eq("createdBy", securityUser.getUser());
        } else {
            where.eq("owner", securityUser.getUser());
        }
        where.eq("pageId", pageId);
        return where.setMaxRows(size).findList();
    }


    @Transactional
    public Either<Code, PageQuery> savePageQuery(Authentication authentication, String name, Long pageId, QueryForm queryMap) {
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        if (StringUtils.isEmpty(name) || ObjectUtils.isEmpty(pageId)) {
            return Either.left(Code.INVALID_ARGUMENT);
        }
        PageQuery query = new PageQuery();
        query.setName(name);
        query.setQueryMap(Json.fromJson(Json.toJson(queryMap), Map.class));
        query.setPageId(pageId);
        query.setStatus(1);
        if (permission.isAdmin(authentication)) {
            query.setOwner(null);
        } else {
            query.setOwner(securityUser.getUser());
        }
        provider.db().save(query);
        return Either.right(query);
    }

    @Transactional
    public Either<Code, PageQuery> deletePageQuery(Authentication authentication, Long id) {
        if (ObjectUtils.isEmpty(id)) {
            return Either.left(Code.INVALID_ARGUMENT);
        }
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        User user = securityUser.getUser();
        PageQuery query = provider.findBean(PageQuery.class, id);
        if (query == null) {
            return Either.left(Code.NOT_FOUND);
        }
        if (permission.isAdmin(authentication) && Objects.equals(user, query.getCreatedBy())) {
            provider.db().delete(query);
            return Either.right(query);
        } else if (Objects.equals(user, query.getOwner())) {
            provider.db().delete(query);
            return Either.right(query);
        } else {
            return Either.left(Code.NOT_FOUND);
        }
    }
}
