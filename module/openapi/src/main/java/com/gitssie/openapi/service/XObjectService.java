package com.gitssie.openapi.service;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gitssie.openapi.data.Code;
import com.gitssie.openapi.ebean.EbeanApi;
import com.gitssie.openapi.models.BasicDomain;
import com.gitssie.openapi.models.user.User;
import com.gitssie.openapi.page.Model;
import com.gitssie.openapi.page.*;
import com.gitssie.openapi.web.query.QueryPredicate;
import com.gitssie.openapi.xentity.XEntityCache;
import com.gitssie.openapi.xentity.XEntityManager;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.ebean.*;
import io.ebean.plugin.BeanType;
import io.ebean.plugin.Property;
import io.ebean.text.json.JsonContext;
import io.ebeaninternal.server.deploy.BeanProperty;
import io.ebeaninternal.server.deploy.BeanPropertyAssoc;
import io.ebeaninternal.server.deploy.BeanPropertyAssocOne;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;

import javax.persistence.OptimisticLockException;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.function.BiConsumer;

@Component
public class XObjectService {
    private final EbeanApi ebeanApi;
    private final XEntityManager entityManager;
    private final Database database;
    private final Provider provider;
    private final AuditService auditService;
    private final ModelNodeConversion mc;

    public XObjectService(EbeanApi ebeanApi, XEntityManager entityManager, Database database, Provider provider,
                          AuditService auditService, ModelNodeConversion mc) {
        this.ebeanApi = ebeanApi;
        this.entityManager = entityManager;
        this.database = database;
        this.provider = provider;
        this.auditService = auditService;
        this.mc = mc;
    }

    public Either<String, XEntityCache> getEntityIfPresent(String apiKey) {
        return entityManager.getEntityIfPresent(apiKey);
    }

    public Either<String, Class<?>> getBeanClassIfPresent(String apiKey) {
        return entityManager.getEntityIfPresent(apiKey).map(entity -> entity.getBeanType());
    }

    public <T> Either<String, BeanType<T>> getBeanTypeIfPresent(String apiKey) {
        return entityManager.getEntityIfPresent(apiKey).map(entity -> {
            Class<T> clazz = (Class<T>) entity.getBeanType();
            return database.pluginApi().beanType(clazz);
        });
    }

    public <T extends BasicDomain> T findById(Class<T> beanClass, Long aLong) {
        return ebeanApi.find(beanClass, aLong);
    }

    public <T> Either<Code, T> find(Class<T> beanClass, Object id) {
        return Option.of(ebeanApi.find(beanClass, id)).toEither(Code.NOT_FOUND);
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
     * 通用新增
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
        Option<Code> code = context.apply(database);
        if (code.isDefined()) {
            ebeanApi.setRollbackOnly();
            return Either.left(code.get());
        }
        Map<String, Object> map = Maps.newHashMapWithExpectedSize(4);
        map.put(desc.idProperty().name(), desc.id(beanE.get()));
        return Either.right(map);
    }

    @Transactional
    public <T> Either<Code, Map<String, Object>> restSave(BeanType<?> desc, ObjectNode body, Model model) {
        Save save = model.saveOne(desc, provider);
        Either<Code, ?> beanE = save.save(body, mc);
        return beanE.flatMap(bean -> {
            Option<Code> code = save.apply(database);
            if (code.isDefined()) {
                ebeanApi.setRollbackOnly();
                return Either.left(code.get());
            }
            Map<String, Object> map = Maps.newHashMapWithExpectedSize(4);
            map.put(desc.idProperty().name(), desc.id(bean));
            return Either.right(map);
        });
    }

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
        Option<Code> code = save.apply(database);
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

    @Transactional
    public <T> Either<Code, Map<String, Object>> restPatch(BeanType<?> desc, ObjectNode body, Model model) {
        Save save = model.saveOne(desc, provider, true);
        Either<Code, ?> beanE = save.save(body, mc);
        return beanE.flatMap(bean -> {
            Option<Code> code = save.apply(database);
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
     * 通用编辑
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
            Option<Code> code = context.apply(database);
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
        Query<io.ebean.bean.EntityBean> query = (Query<io.ebean.bean.EntityBean>) database.createQuery(desc.type());
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
                Option<Code> code = context.apply(database);
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
    public <T extends BasicDomain> Either<Code, Object> delete(BeanType<T> desc, Object id, Model model, Expression dataPermission) {
        Patch patch = model.patchOne(desc, provider);
        patch.and(Expr.eq(desc.idProperty().name(), id));
        patch.and(dataPermission);
        T bean = (T) patch.fetch();
        if (bean == null) {
            return Either.left(Code.NOT_FOUND);
        } else {
            //更新对象,判断数据是否可以删除
            Option<Code> code = canBeDelete(bean);
            if (code.isDefined()) {
                return Either.left(code.get());
            }
            Object beanId = desc.idProperty().value(bean);
            delete(bean);
            return Either.right(beanId);
        }
    }

    /**
     * 判断数据是否可以被删除
     *
     * @param bean
     * @param <T>
     * @return
     */
    private <T extends BasicDomain> Option<Code> canBeDelete(T bean) {
        //更新对象,判断数据是否可以删除
        if (bean.isLockStatus()) {
            return Option.of(Code.FAILED_PRECONDITION.withMessage("数据被锁定,无法删除"));
        } else {
            return Option.none();
        }
    }

    @Transactional
    public <T extends BasicDomain> Either<Code, List<Map<String, Object>>> batchDelete(BeanType<T> desc, List<String> idList, Model model, Expression dataPermission) {
        Patch patch = model.patchList(desc, provider); //list
        patch.and(Expr.in(desc.idProperty().name(), idList));
        patch.and(dataPermission);
        List<T> beans = (List) patch.fetch();
        List<T> deleteBeans = Lists.newArrayListWithExpectedSize(beans.size());
        List<Map<String, Object>> result = Lists.newArrayListWithExpectedSize(beans.size());
        Option<Code> code;
        Map<String, Object> item;
        for (T bean : beans) {
            code = canBeDelete(bean);
            item = Maps.newHashMapWithExpectedSize(4);
            item.put("id", bean.getId());
            item.put("success", true);
            if (code.isDefined()) {
                item.put("success", false);
                item.put("message", code.get().getMessage());
            } else {
                deleteBeans.add(bean);
            }
            result.add(item);
        }
        deleteAll(deleteBeans);
        return Either.right(result);
    }

    @Transactional(readOnly = true)
    public Either<Code, Page<BasicDomain>> query(String apiKey, QueryPredicate queryPredicate, Pageable page, Sort sort) {
        XEntityCache entity = entityManager.getEntity(apiKey);
        Class<BasicDomain> beanClass = (Class<BasicDomain>) entity.getBeanType();
        Either<Code, PagedList<BasicDomain>> either = ebeanApi.query(ebeanApi.desc(beanClass), queryPredicate, page, sort);
        return either.map(pagedList -> {
            PageImpl result = new PageImpl(pagedList.getList(), page, pagedList.getTotalCount());
            return result;
        });
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, Page<Map<String, Object>>> queryMap(Class<T> beanClass, QueryPredicate queryPredicate, Pageable page) {
        return query(beanClass, queryPredicate, page, page.getSort()).map(e -> ebeanApi.toJSON(e));
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, Page<T>> query(Class<T> beanClass, QueryPredicate queryPredicate, Pageable page) {
        return query(beanClass, queryPredicate, page, page.getSort());
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, Page<T>> query(BeanType<T> desc, QueryPredicate queryPredicate, Pageable page) {
        return query(desc, queryPredicate, page, page.getSort());
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, Page<Map<String, Object>>> queryMap(BeanType<T> desc, Model model, Expression dataPermission, QueryPredicate queryPredicate, Pageable page) {
        return queryMap(desc, model, dataPermission, queryPredicate, page, page.getSort());
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, List<Map<String, Object>>> queryRefreshList(BeanType<T> desc, Model model, Expression dataPermission, List<String> idArray) {
        Fetch fetch = model.fetchList(desc, provider);
        Expression where = Expr.in(desc.idProperty().name(), idArray);
        if (dataPermission != null) {
            where = Expr.and(where, dataPermission);
        }
        Either<Code, List<T>> pagedList = queryList(fetch, where);
        return pagedList.map(e -> {
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) fetch.toJSON(e);
            return dataList;
        });
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, Object> queryMapAggre(BeanType<T> desc, Expression dataPermission, QueryPredicate queryPredicate, Pageable page, AggreModel aggre) {
        return queryMapAggre(desc, dataPermission, queryPredicate, page, page.getSort(), aggre);
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

    @Transactional(readOnly = true)
    public <T> Either<Code, PagedList<T>> queryPage(BeanType<T> desc, Model model, Expression dataPermission, QueryPredicate queryPredicate, Pageable page) {
        return queryPage(desc, model, dataPermission, queryPredicate, page, page.getSort());
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, PagedList<T>> queryPage(BeanType<T> desc, Model model, Expression dataPermission, QueryPredicate queryPredicate, Pageable page, Sort sort) {
        Fetch fetch = model.fetchList(desc, provider);
        Either<Code, PagedList<T>> pagedList = queryMap(desc, fetch, dataPermission, queryPredicate, page, sort);
        return pagedList;
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
        List list = fetch.fetch(query -> {
            return query.findList();
        });
        return Either.right(list);
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
        Query query = database.createQuery(desc.type());
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
            Query aggreQuery = database.createQuery(aggreP.type());
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
    public <T> List<T> list(Class<T> beanClass) {
        return database.find(beanClass).findList();
    }

    @Transactional(readOnly = true)
    public <T> List<T> list(Class<T> beanClass, Expression expr) {
        return database.find(beanClass).where(expr).findList();
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, Page<T>> query(Class<T> beanClass, QueryPredicate queryPredicate, Pageable page, Sort sort) {
        return query(ebeanApi.desc(beanClass), queryPredicate, page, sort);
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, Page<T>> query(BeanType<T> desc, QueryPredicate queryPredicate, Pageable page, Sort sort) {
        Either<Code, PagedList<T>> either = ebeanApi.query(desc, queryPredicate, page, sort);
        return either.map(pagedList -> {
            PageImpl result = new PageImpl(pagedList.getList(), page, pagedList.getTotalCount());
            return result;
        });
    }

    public <T> T createEntityBean(Class<T> beanClass) {
        return database.createEntityBean(beanClass);
    }

    public <T> void save(T bean) {
        database.save(bean);
    }

    public <T> void update(T bean) {
        database.update(bean);
    }

    public <T> boolean delete(T bean) throws OptimisticLockException {
        return database.delete(bean);
    }

    public int deleteAll(Class<?> beanType, Collection<?> ids) {
        return database.deleteAll(beanType, ids);
    }

    public <T> int deleteAll(Collection<T> beans) {
        return database.deleteAll(beans);
    }

    public <T> List<T> findAll(Class<T> beanType, Collection<?> ids) {
        BeanType<T> desc = provider.desc(beanType);
        return database.find(beanType).where().in(desc.idProperty().name(), ids).findList();
    }

    //
    public void writePropertyToCSV(Writer writer, List<BasicDomain> list, List<String> properties) throws IOException {
        BeanType beanType = null;
        JsonContext json = database.json();
        JsonGenerator generator = json.createGenerator(writer);
        for (BasicDomain entityBean : list) {
            if (beanType == null) {
                beanType = beanType(entityBean.getClass());
            }
            io.ebean.bean.EntityBean entity = (io.ebean.bean.EntityBean) entityBean;
            for (String field : properties) {
                Property property = beanType.property(field);
                if (property == null || property.isMany()) {
                    continue;
                }
                BeanProperty beanProperty = (BeanProperty) property;
                Object value = beanProperty.getValueIntercept(entity);
                if (value != null) {
                    beanProperty.scalarType().jsonWrite(generator, value);
                    generator.flush();
//                    beanProperty.jsonWriteValue(generator,value);
                    //String text = beanProperty.scalarType().formatValue(value);
//                    writer.write(text);
                }
                writer.write(",");
            }
            writer.write("\n");
        }
//        String str = writer.toString();
    }

    public BeanType beanType(Class<?> clazz) {
        return database.pluginApi().beanType(clazz);
    }

    private boolean isInsertable(BeanProperty p) {
        boolean exclude = p.isId() || p.isTenantId() || p.isVersion() || !p.isDbInsertable();
        return !exclude;
    }

    private boolean isUpdatable(BeanProperty p) {
        boolean exclude = p.isId() || p.isTenantId() || p.isVersion() || !p.isDbUpdatable();
        return !exclude;
    }

    @Transactional
    public <T extends BasicDomain> List<Either<Code, T>> lockAll(BeanType<T> desc, Collection<?> idList, Model model, Expression dataPermission) {
        Patch patch = model.patchList(desc, provider); //list
        patch.and(Expr.in(desc.idProperty().name(), idList));
        patch.and(dataPermission);
        List<T> beans = (List) patch.fetch();
        return lockAll(beans);
    }

    @Transactional
    public <T extends BasicDomain> List<Either<Code, T>> unlockAll(BeanType<T> desc, Collection<?> idList, Model model, Expression dataPermission) {
        Patch patch = model.patchList(desc, provider); //list
        patch.and(Expr.in(desc.idProperty().name(), idList));
        patch.and(dataPermission);
        List<T> beans = (List) patch.fetch();
        return unlockAll(beans);
    }

    private <T extends BasicDomain> List<Either<Code, T>> lockAll(List<T> beans) {
        List<Either<Code, T>> result = Lists.newArrayListWithExpectedSize(beans.size());
        for (T bean : beans) {
            if (!bean.isLockStatus()) {
                bean.setLockStatus(true);
            }
            result.add(Either.right(bean));
        }
        database.updateAll(beans);
        return result;
    }

    private <T extends BasicDomain> List<Either<Code, T>> unlockAll(List<T> beans) {
        List<Either<Code, T>> result = Lists.newArrayListWithExpectedSize(beans.size());
        for (T bean : beans) {
            if (bean.isLockStatus()) {
                bean.setLockStatus(false);
            }
            result.add(Either.right(bean));
        }
        database.updateAll(beans);
        return result;
    }

    @Transactional
    public <T extends BasicDomain> Either<Code, List<T>> batchTransfer(BeanType<T> desc, Collection<?> idList, Model model, Expression dataPermission, Long userId) {
        Either<Code, User> userE = find(User.class, userId);
        Patch patch = model.patchList(desc, provider); //list
        patch.and(Expr.in(desc.idProperty().name(), idList));
        patch.and(dataPermission);
        List<T> beans = (List) patch.fetch();
        return userE.map(user -> {
            for (T bean : beans) {
                if (bean.isLockStatus()) {
                    continue;
                }
                bean.setOwner(user);
            }
            database.updateAll(beans);
            return beans;
        });
    }

    @Transactional
    public <T extends BasicDomain> List<Either<Code, T>> batchSubmit(BeanType<T> desc, Collection<?> idList, Model model, Expression dataPermission, User user) {
        Patch patch = model.patchList(desc, provider); //list
        patch.and(Expr.in(desc.idProperty().name(), idList));
        patch.and(dataPermission);
        List<T> beans = (List) patch.fetch();
        return auditService.submit(desc.name(), user, beans);
    }

    @Transactional
    public <T extends BasicDomain> List<Either<Code, T>> batchApproval(BeanType<T> desc, Collection<?> idList, Model model, Expression dataPermission, User user) {
        Patch patch = model.patchList(desc, provider); //list
        patch.and(Expr.in(desc.idProperty().name(), idList));
        patch.and(dataPermission);
        List<T> beans = (List) patch.fetch();
        return auditService.approval(desc.name(), user, beans);
    }

    @Transactional
    public <T extends BasicDomain> List<Either<Code, T>> batchReview(BeanType<T> desc, Collection<?> idList, Model model, Expression dataPermission, User user) {
        Patch patch = model.patchList(desc, provider); //list
        patch.and(Expr.in(desc.idProperty().name(), idList));
        patch.and(dataPermission);
        List<T> beans = (List) patch.fetch();
        return auditService.review(desc.name(), user, beans);
    }

    @Transactional
    public <T> void batchUpdate(BeanType<T> desc, List<Long> ids, BiConsumer<ModelNodeConversion, List<T>> consumer) {
        List<T> beans = findAll(desc.type(), ids);
        consumer.accept(mc, beans);
        database.updateAll(beans);
    }
}
