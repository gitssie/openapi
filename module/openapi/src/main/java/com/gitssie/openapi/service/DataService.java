package com.gitssie.openapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gitssie.openapi.data.Code;
import com.gitssie.openapi.ebean.EbeanApi;
import com.gitssie.openapi.rule.RuleProxyMap;
import com.gitssie.openapi.ebean.TypeQuery;
import com.gitssie.openapi.ebean.TypeQueryRaw;
import com.gitssie.openapi.ebean.repository.SQLParameterFunction;
import com.gitssie.openapi.ebean.repository.SQLRowMapper;
import com.gitssie.openapi.models.BasicDomain;
import com.gitssie.openapi.page.Model;
import com.gitssie.openapi.page.*;
import com.gitssie.openapi.rule.Rules;
import com.gitssie.openapi.web.query.QueryMap;
import com.gitssie.openapi.web.query.QueryPredicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.ebean.*;
import io.ebean.bean.EntityBean;
import io.ebean.plugin.BeanType;
import io.ebean.plugin.Property;
import io.ebeaninternal.server.deploy.BeanPropertyAssoc;
import io.ebeaninternal.server.deploy.BeanPropertyAssocOne;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author: Awesome
 * @create: 2024-02-22 16:56
 */
@Service
public class DataService {
    private EbeanApi ebeanApi;
    private SQLRowMapper rowMapper;
    private Provider provider;
    private ModelNodeConversion mc;
    private PageService pageService;

    public DataService(EbeanApi ebeanApi, SQLRowMapper rowMapper, Provider provider, ModelNodeConversion mc, PageService pageService) {
        this.ebeanApi = ebeanApi;
        this.rowMapper = rowMapper;
        this.provider = provider;
        this.mc = mc;
        this.pageService = pageService;
        this.ebeanApi.setDataService(this);
    }

    /**
     * 根据提交的表单创建实体,使用model进行参数验证
     *
     * @param desc
     * @param body
     * @param model
     * @param <T>
     * @return
     */
    @Transactional
    public <T> Either<Code, Map<String, Object>> create(BeanType<T> desc, ObjectNode body, Model model) {
        Create create = model.createOne(desc, provider);
        Either<Code, ?> beanE = create.create(body, mc);
        if (beanE.isLeft()) {
            return (Either) beanE;
        }
        NeedContext context = create.context();
        Option<Code> code = context.apply(ebeanApi.db());
        if (code.isDefined()) {
            ebeanApi.setRollbackOnly();
            return Either.left(code.get());
        }
        Map<String, Object> map = Maps.newHashMapWithExpectedSize(4);
        map.put(desc.idProperty().name(), desc.id(beanE.get()));
        return Either.right(map);
    }

    /**
     * 批量新增/修改数据,同一事务
     *
     * @param desc
     * @param body
     * @param model
     * @return
     */
    @Transactional
    public Either<Code, List<Map<String, Object>>> batchSave(BeanType<?> desc, ArrayNode body, Model model) {
        Save save = model.saveOne(desc, provider);
        List<EntityBean> beans = new LinkedList<>();
        List<Map<String, Object>> result = new LinkedList<>();
        for (JsonNode node : body) {
            if (!node.isObject()) {
                continue;
            }
            Either<Code, EntityBean> bean = save.save((ObjectNode) node, mc);
            if (bean.isLeft()) {
                return (Either) bean;
            } else {
                beans.add(bean.get());
            }
        }
        //批量保存数据
        Option<Code> code = save.apply(ebeanApi.db());
        if (code.isDefined()) {
            ebeanApi.setRollbackOnly();
            return Either.left(code.get());
        }
        for (EntityBean bean : beans) {
            Map<String, Object> map = Maps.newHashMapWithExpectedSize(4);
            map.put(desc.idProperty().name(), desc.id(bean));
            result.add(map);
        }

        return Either.right(result);
    }

    /**
     * 根据提交的表单创建与更新，实体不存在则新增，存在则更新
     * 使用model进行参数验证
     *
     * @param desc
     * @param body
     * @param model
     * @param <T>
     * @return
     */
    @Transactional
    public <T> Either<Code, Map<String, Object>> restSave(BeanType<?> desc, ObjectNode body, Model model) {
        Save save = model.saveOne(desc, provider);
        Either<Code, ?> beanE = save.save(body, mc);
        return beanE.flatMap(bean -> {
            Option<Code> code = save.apply(ebeanApi.db());
            if (code.isDefined()) {
                ebeanApi.setRollbackOnly();
                return Either.left(code.get());
            }
            Map<String, Object> map = Maps.newHashMapWithExpectedSize(4);
            map.put(desc.idProperty().name(), desc.id(bean));
            return Either.right(map);
        });
    }

    /**
     * 根据提交的表单创建与更新，实体不存在则新增，存在则更新
     * 使用model进行参数验证
     * 批量插入、更新
     *
     * @param desc
     * @param body
     * @param model
     * @param <T>
     * @return
     */
    @Transactional
    public <T> Either<Code, List<Map<String, Object>>> restBatchSave(BeanType<?> desc, ArrayNode body, Model model) {
        Save save = model.saveOne(desc, provider);
        List<Either<Code, io.ebean.bean.EntityBean>> beans = new LinkedList<>();
        List<Map<String, Object>> result = new LinkedList<>();
        for (JsonNode node : body) {
            if (!node.isObject()) {
                continue;
            }
            beans.add(save.save((ObjectNode) node, mc));
        }
        //批量保存数据
        Option<Code> code = save.apply(ebeanApi.db());
        if (code.isDefined()) {
            ebeanApi.setRollbackOnly();
            return Either.left(code.get());
        }
        for (Either<Code, io.ebean.bean.EntityBean> bean : beans) {
            Map<String, Object> map = Maps.newHashMapWithExpectedSize(2);
            if (bean.isRight()) {
                map.put("success", true);
                map.put(desc.idProperty().name(), desc.id(bean.get()));
            } else {
                map.put("success", false);
                map.put("message", bean.getLeft().getMessage());
            }
            result.add(map);
        }

        return Either.right(result);
    }

    /**
     * 获取详情
     * 根据ID获取对象的详细信息
     *
     * @param desc
     * @param id
     * @param model
     * @param <T>
     * @return
     */
    @Transactional(readOnly = true)
    public <T> Either<Code, Map<String, Object>> fetchDetail(BeanType<T> desc, Object id, Model model, Expression dataPermission) {
        Fetch fetch = model.fetchOne(desc, provider);
        fetch.and(Expr.eq(desc.idProperty().name(), id));
        fetch.and(dataPermission);
        Object bean = fetch.fetch();
        if (bean == null) {
            return Either.left(Code.NOT_FOUND);
        } else {
            Object result = fetch.toJSON(bean);
            return Either.right((Map) result);
        }
    }

    /**
     * 只进行更新
     *
     * @param desc
     * @param body
     * @param model
     * @param <T>
     * @return
     */
    @Transactional
    public <T> Either<Code, Map<String, Object>> restPatch(BeanType<?> desc, ObjectNode body, Model model) {
        Save save = model.saveOne(desc, provider, true);
        Either<Code, ?> beanE = save.save(body, mc);
        return beanE.flatMap(bean -> {
            Option<Code> code = save.apply(ebeanApi.db());
            if (code.isDefined()) {
                ebeanApi.setRollbackOnly();
                return Either.left(code.get());
            }
            Map<String, Object> map = Maps.newHashMapWithExpectedSize(4);
            map.put(desc.idProperty().name(), desc.id(bean));
            return Either.right(map);
        });
    }

    /**
     * 根据指定的ID列表查询数据
     *
     * @param desc
     * @param ids
     * @param model
     * @param dataPermission
     * @param <T>
     * @return
     */
    @Transactional(readOnly = true)
    public <T> Either<Code, List<Map<String, Object>>> fetchDetailList(BeanType<T> desc, List<Long> ids, Model model, Expression dataPermission) {
        Fetch fetch = model.fetchList(desc, provider);
        fetch.and(Expr.in(desc.idProperty().name(), ids));
        fetch.and(dataPermission);
        List<Object> bean = (List<Object>) fetch.fetch();
        if (bean == null) {
            return Either.left(Code.NOT_FOUND);
        } else {
            List<Map<String, Object>> result = (List<Map<String, Object>>) fetch.toJSON(bean);
            return Either.right(result);
        }
    }

    /**
     * 通用数据编辑
     * 根据实体的ID查询出数据，根据model的配置对实体进行更新
     *
     * @param desc
     * @param id
     * @param body
     * @param model
     * @param <T>
     * @return
     */
    @Transactional
    public <T> Either<Code, Map<String, Object>> patch(BeanType<?> desc, Object id, ObjectNode body, Model model, Expression dataPermission) {
        Patch patch = model.patchOne(desc, provider);
        patch.and(Expr.eq(desc.idProperty().name(), id));
        patch.and(dataPermission);
        Object bean = patch.fetch();
        if (bean == null) {
            return Either.left(Code.NOT_FOUND);
        } else {
            //更新对象图
            Either<Code, Object> patchE = patch.patch(bean, body, mc);
            if (patchE.isLeft()) {
                return (Either) patchE;
            }
            NeedContext context = patch.context();
            Option<Code> code = context.apply(ebeanApi.db());
            if (code.isDefined()) {
                ebeanApi.setRollbackOnly();
                return Either.left(code.get());
            }
            Map<String, Object> map = Maps.newHashMapWithExpectedSize(4);
            map.put(desc.idProperty().name(), desc.id(bean));
            return Either.right(map);
        }
    }


    @Transactional
    public <T> Either<Code, Map<String, Object>> patchValue(BeanType<?> desc, BeanPropertyAssocOne property, Object id, Expression dataPermission, ObjectNode body) {
        Query<EntityBean> query = (Query<io.ebean.bean.EntityBean>) ebeanApi.createQuery(desc.type());
        query.where().eq(desc.idProperty().name(), id);
        if (dataPermission != null) {
            query.where().add(dataPermission);
        }
        io.ebean.bean.EntityBean bean = query.findOne();
        if (bean == null) {
            return Either.left(Code.NOT_FOUND);
        } else {
            //更新对象图
            NeedContext context = new NeedContext();
            try (context) {
                context.addUpdate(bean);
                //设置关联属性
                Either<Errors, ?> errors = mc.assocValue(context, provider, property, bean, body);
                if (errors.isLeft()) {
                    return Either.left(Code.INVALID_ARGUMENT.withErrors(Option.of(errors.get())));
                }
                Option<Code> code = context.apply(ebeanApi.db());
                if (code.isDefined()) {
                    ebeanApi.setRollbackOnly();
                    return Either.left(code.get());
                }
                Map<String, Object> map = Maps.newHashMapWithExpectedSize(4);
                map.put(desc.idProperty().name(), desc.id(bean));
                return Either.right(map);
            }
        }
    }

    @Transactional
    public <T> Either<Code, Map<String, Object>> delete(BeanType<T> desc, Object id, Model model, Expression dataPermission) {
        Patch patch = model.patchOne(desc, provider);
        patch.and(Expr.eq(desc.idProperty().name(), id));
        patch.and(dataPermission);
        T bean = (T) patch.fetch();
        if (bean == null) {
            return Either.left(Code.NOT_FOUND);
        } else {
            //更新对象,判断数据是否可以删除
            NeedContext context = patch.context();
            context.addDelete(bean);
            model.addPreDelete((ctx, e) -> canBeDelete(e));
            Option<Code> code = context.apply(ebeanApi.db());
            if (code.isDefined()) {
                ebeanApi.setRollbackOnly();
                return Either.left(code.get());
            }
            Map<String, Object> map = Maps.newHashMapWithExpectedSize(4);
            map.put(desc.idProperty().name(), desc.id(bean));
            return Either.right(map);
        }
    }

    /**
     * 判断数据是否可以被删除
     *
     * @param bean
     * @param <T>
     * @return
     */
    private <T> Option<Code> canBeDelete(T bean) {
        //更新对象,判断数据是否可以删除
        if (bean instanceof BasicDomain) {
            if (((BasicDomain) bean).isLockStatus()) {
                return Option.of(Code.FAILED_PRECONDITION.withMessage("数据被锁定,无法删除"));
            }
        }
        return Option.none();
    }


    @Transactional
    public <T> Either<Code, List<Map<String, Object>>> batchDelete(BeanType<T> desc, List<String> idList, Model model, Expression dataPermission) {
        Patch patch = model.patchList(desc, provider); //list
        patch.and(Expr.in(desc.idProperty().name(), idList));
        patch.and(dataPermission);
        List<T> beans = (List) patch.fetch();
        List<T> deleteBeans = Lists.newArrayListWithExpectedSize(beans.size());
        List<Map<String, Object>> result = Lists.newArrayListWithExpectedSize(beans.size());
        Option<Code> code;
        Map<String, Object> item;
        Property idProperty = desc.idProperty();
        for (T bean : beans) {
            code = canBeDelete(bean);
            item = Maps.newHashMapWithExpectedSize(4);
            item.put(idProperty.name(), idProperty.value(bean));
            item.put("success", true);
            if (code.isDefined()) {
                item.put("success", false);
                item.put("message", code.get().getMessage());
            } else {
                deleteBeans.add(bean);
            }
            result.add(item);
        }
        ebeanApi.deleteAll(deleteBeans);
        return Either.right(result);
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, Page<T>> query(String apiKey, QueryPredicate queryPredicate, Pageable page, Sort sort) {
        Either<String, BeanType<T>> entity = provider.getBeanTypeIfPresent(apiKey);
        if (entity.isLeft()) {
            return Either.left(Code.notFound(entity.getLeft()));
        }
        Either<Code, PagedList<T>> either = ebeanApi.query(entity.get(), queryPredicate, page, sort);
        return either.map(pagedList -> {
            PageImpl result = new PageImpl(pagedList.getList(), page, pagedList.getTotalCount());
            return result;
        });
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, Page<Map<String, Object>>> queryMap(BeanType<T> desc, Model model, Expression dataPermission, QueryPredicate queryPredicate, Pageable page) {
        return queryMap(desc, model, dataPermission, queryPredicate, page, page.getSort());
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, Page<Map<String, Object>>> queryMap(BeanType<T> desc, Model model, Expression dataPermission, QueryPredicate queryPredicate, Pageable page, Sort sort) {
        Fetch fetch = model.fetchList(desc, provider);
        Either<Code, PagedList<T>> pagedList = queryMap(desc, fetch, dataPermission, queryPredicate, page, sort);
        return pagedList.map(e -> {
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) fetch.toJSON(e.getList());
            if (page.isPaged() && page.getPageNumber() > 0) {
                return new PageImpl(dataList);
            }
            return new PageImpl(dataList, page, e.getTotalCount());
        });
    }

    protected <T> Either<Code, PagedList<T>> queryMap(BeanType<T> desc, Fetch fetch, Expression dataPermission, QueryPredicate queryPredicate, Pageable page, Sort sort) {
        Expression where = dataPermission;
        if (queryPredicate != null && ObjectUtils.isNotEmpty(queryPredicate.getPredicate())) {
            Either<Code, Expression> predicate = ebeanApi.parsePredicate(desc, queryPredicate.getPredicate()).flatMap(p -> ebeanApi.parseExpression(queryPredicate.getExpression(), p));
            if (predicate.isLeft()) {
                return (Either) predicate;
            }
            Expression queryWhere = predicate.get();
            if (queryWhere != null) {
                if (where == null) {
                    where = queryWhere;
                } else {
                    where = Expr.and(queryWhere, where);
                }
            }
        }
        return queryMap(desc, fetch, where, page, sort);
    }

    protected <T> Either<Code, PagedList<T>> queryMap(BeanType<T> desc, Fetch fetch, Expression where, Pageable page, Sort sort) {
        fetch.and(where);
        PagedList pagedList = fetch.fetch(query -> {
            if (sort != null) {
                if (sort.isUnsorted()) {
                    query.orderBy().desc(desc.idProperty().name());
                } else {
                    for (Sort.Order e : sort) {
                        if (e.isDescending()) {
                            query.orderBy().desc(e.getProperty());
                        } else {
                            query.orderBy().asc(e.getProperty());
                        }
                    }
                }
            }
            if (page != null && page.isPaged()) {
                query.setFirstRow((int) page.getOffset()).setMaxRows(page.getPageSize());
            }
            return query.findPagedList();
        });
        return Either.right(pagedList);
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, List<T>> queryList(BeanType<T> desc, Model model, Expression dataPermission, QueryPredicate queryPredicate) {
        Fetch fetch = model.fetchList(desc, provider);
        fetch.and(dataPermission);
        Either<Code, List<T>> pagedList = queryList(desc, fetch, queryPredicate);
        return pagedList;
    }

    protected <T> Either<Code, List<T>> queryList(BeanType<T> desc, Fetch fetch, QueryPredicate queryPredicate) {
        Expression where = null;
        if (queryPredicate != null && ObjectUtils.isNotEmpty(queryPredicate.getPredicate())) {
            Either<Code, Expression> predicate = ebeanApi.parsePredicate(desc, queryPredicate.getPredicate()).flatMap(p -> ebeanApi.parseExpression(queryPredicate.getExpression(), p));
            if (predicate.isLeft()) {
                return (Either) predicate;
            }
            where = predicate.get();
        }
        return queryList(fetch, where);
    }

    protected <T> Either<Code, List<T>> queryList(Fetch fetch, Expression where) {
        fetch.and(where);
        List list = fetch.fetch(query -> query.findList());
        return Either.right(list);
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, Object> queryMapAggre(BeanType<T> desc, Expression dataPermission, QueryPredicate queryPredicate, Pageable page, AggreModel aggre) {
        return queryMapAggre(desc, dataPermission, queryPredicate, page, page.getSort(), aggre);
    }

    protected <T> Either<Code, Object> queryMapAggre(BeanType<T> desc, Expression filter, QueryPredicate queryPredicate, Pageable page, Sort sort, AggreModel aggre) {
        Either<Code, Tuple2<Optional<Property>, String>> aggreE = ebeanApi.parseAggregationProperties(desc, aggre.getAggre());
        if (aggreE.isLeft()) {
            return (Either) aggreE;
        }
        Tuple2<Optional<Property>, String> aggreProp = aggreE.get();
        Expression where = null;
        if (queryPredicate != null && ObjectUtils.isNotEmpty(queryPredicate.getPredicate())) {
            Either<Code, Expression> predicate = ebeanApi.parsePredicate(desc, queryPredicate.getPredicate()).flatMap(p -> ebeanApi.parseExpression(queryPredicate.getExpression(), p));
            if (predicate.isLeft()) {
                return (Either) predicate;
            }
            where = predicate.get();
        }
        if (where == null) {
            where = filter;
        } else if (filter != null) {
            where = Expr.and(where, filter);
        }
        Query query = ebeanApi.createQuery(desc.type());
        if (aggreProp._1.isPresent()) {
            query.select(desc.idProperty().name());
        } else {
            query.select(aggreProp._2);
        }
        if (where != null) {
            query.where(where);
        }
        if (aggreProp._1.isPresent()) {
            BeanPropertyAssoc aggreP = (BeanPropertyAssoc) aggreProp._1.get();
            Query aggreQuery = ebeanApi.createQuery(aggreP.type());
            aggreQuery.select(aggreProp._2);
            aggreQuery.where().in(aggreP.mappedBy(), query);
            query = aggreQuery;
        }
        if (sort != null && sort.isSorted()) {
            for (Sort.Order e : sort) {
                if (e.isDescending()) {
                    query.orderBy().desc(e.getProperty());
                } else {
                    query.orderBy().asc(e.getProperty());
                }
            }
        }
        if (page != null && page.isPaged()) {
            query.setFirstRow((int) page.getOffset()).setMaxRows(page.getPageSize());
        }
        DtoQuery dtoQuery = query.asDto(aggre.getDtoClass());
        if (aggre.isList()) {
            return Either.right(dtoQuery.findList());
        } else {
            return Either.right(dtoQuery.setMaxRows(1).findOne());
        }
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, Object> queryRawSql(FetchContext context, Map<String, Object> scope, Pageable page, AggreModel aggre, Option<Model> table) {
        return queryRawSql(context, scope, page, page.getSort(), aggre, table);
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, Object> queryRawSql(Map<String, Object> scope, Pageable page, AggreModel aggre, Option<Model> table) {
        FetchContext context = new FetchContext(provider);
        try (context) {
            return queryRawSql(context, scope, page, page.getSort(), aggre, table);
        }
    }

    protected <T> Either<Code, Object> queryRawSql(Map<String, Object> scope, Pageable page, Sort sort, AggreModel aggre, Option<Model> table) {
        FetchContext context = new FetchContext(provider);
        try (context) {
            return queryRawSql(context, scope, page, sort, aggre, table);
        }
    }

    protected Either<Code, Object> queryRawSql(FetchContext context, Map<String, Object> scopeMap, Pageable page, Sort sort, AggreModel aggre, Option<Model> table) {
        Scriptable scope;
        if (scopeMap instanceof Scriptable) {
            scope = (Scriptable) scopeMap;
        } else {
            scope = new RuleProxyMap(scopeMap);
        }
        scope.put("page", scope, page);
        scope.put("sort", scope, sort);
        SQLParameterFunction pf = new SQLParameterFunction();
        scope.put(pf.getParameterFuncName(), scope, pf);

        Object funcResult = Rules.runFunction(context.get(), aggre.getRawSql(), scope);
        if (funcResult instanceof Undefined) {
            return Either.left(Code.FAILED_PRECONDITION.withMessage(""));
        }
        String sql = "";
        String count = "";
        if (aggre.isPage()) {
            boolean excludeCount = true;
            if (funcResult instanceof NativeObject) {
                NativeObject data = (NativeObject) funcResult;
                sql = data.get("sql", data).toString();
                count = data.get("count", data).toString();
            } else if (funcResult instanceof List) {
                List data = (List) funcResult;
                if (data.size() == 1) {
                    sql = data.get(0).toString();
                } else if (data.size() > 1) {
                    sql = data.get(0).toString();
                    count = data.get(1).toString();
                }
            }
            if (page.isPaged() && page.getPageNumber() > 0 && excludeCount) {
                count = "";
            } else if (StringUtils.isEmpty(count)) {
                return Either.left(Code.FAILED_PRECONDITION.withMessage("query need a row count statement"));
            }
        } else {
            sql = funcResult.toString();
        }

        if (StringUtils.isEmpty(sql)) {
            return Either.left(Code.FAILED_PRECONDITION.withMessage("query statement is empty"));
        }

        //sql query
        Object[] args = pf.getArgs().toArray();
        SqlQuery.TypeQuery sqlQuery = ebeanApi.db().sqlQuery(sql)
                .setTimeout(aggre.timeout())
                .setParameters(args)
                .mapTo(rowMapper.createRowMapper(aggre.getDtoClass(), true));

        if (aggre.isMap()) {
            Map<String, Object> tmp = (Map<String, Object>) sqlQuery.findOne();
            if (tmp == null && aggre.isMapOne()) {
                tmp = Maps.newHashMap();
            }
            Map<String, Object> result = tmp;
            //data convert
            table.forEach(model -> ebeanApi.toJSONMap(model, result));
            return Either.right(result);
        } else if (aggre.isList()) {
            List<Map<String, Object>> result = sqlQuery.findList();
            //data convert
            table.forEach(model -> ebeanApi.toJSONMap(model, result));
            return Either.right(result);
        } else {
            //page
            List<Map<String, Object>> list = sqlQuery.findList();
            //data convert
            table.forEach(model -> ebeanApi.toJSONMap(model, list));
            if (StringUtils.isEmpty(count)) {
                return Either.right(new PageImpl(list));
            }
            //count row
            long rowCount = ebeanApi.db().sqlQuery(count)
                    .setTimeout(aggre.timeout())
                    .setParameters(args)
                    .mapToScalar(Long.class)
                    .findOne();

            PageImpl result = new PageImpl(list, page, rowCount);
            return Either.right(result);
        }
    }

    @Transactional(readOnly = true)
    public Either<Code, SqlRow> sqlOne(String apiKey, String funcName, Map<String, Object> queryParams, Pageable page) {
        return sqlQuery(apiKey, funcName, queryParams, page, page.getSort()).map(e -> e.findOne());
    }

    @Transactional(readOnly = true)
    public Either<Code, List<SqlRow>> sqlList(String apiKey, String funcName, Map<String, Object> queryParams, Pageable page) {
        return sqlQuery(apiKey, funcName, queryParams, page, page.getSort()).map(e -> e.findList());
    }

    @Transactional(readOnly = true)
    public Either<Code, Page<SqlRow>> sqlPage(String apiKey, String funcName, Map<String, Object> queryParams, Pageable page) {
        return sqlQuery(apiKey, funcName, queryParams, page, page.getSort()).map(e -> e.findPage());
    }

    @Transactional(readOnly = true)
    public Either<Code, TypeQuery<SqlRow>> sqlQuery(String apiKey, String funcName, Map<String, Object> queryParams, Pageable page) {
        return sqlQuery(apiKey, funcName, queryParams, page, page.getSort());
    }

    @Transactional(readOnly = true)
    public Either<Code, TypeQuery<SqlRow>> sqlQuery(String apiKey, String funcName, Map<String, Object> queryParams, Pageable page, Sort sort) {
        Either<Code, AggreModel> modelE = pageService.getAggre(apiKey, funcName);
        if (modelE.isLeft()) {
            return (Either) modelE;
        }
        //查询条件
        Option<Model> query = pageService.getQuery(apiKey, funcName).toOption();
        //返回数据格式化
        Option<Model> table = pageService.getTable(apiKey, funcName).toOption();
        Map<String, Object> scope;
        if (query.isDefined()) {
            QueryMap queryMap = new QueryMap(queryParams);
            Tuple2<Option<Errors>, Map<String, Object>> errors = queryMap.validateWithScope(query);
            if (errors._1.isDefined()) {
                return Either.left(Code.INVALID_ARGUMENT.withErrors(errors._1));
            }
            scope = errors._2();
        } else {
            scope = new RuleProxyMap(queryParams);
        }
        AggreModel model = modelE.get();
        //查询返回
        return queryRawSql(scope, page, sort, model, table).map(e -> new TypeQueryRaw(e));
    }
}
