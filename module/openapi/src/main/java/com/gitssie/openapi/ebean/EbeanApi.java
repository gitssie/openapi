package com.gitssie.openapi.ebean;

import com.gitssie.openapi.data.Code;
import com.gitssie.openapi.page.Model;
import com.gitssie.openapi.page.*;
import com.gitssie.openapi.service.DataService;
import com.gitssie.openapi.service.Provider;
import com.gitssie.openapi.web.query.AbstractQuery;
import com.gitssie.openapi.web.query.PredicateField;
import com.gitssie.openapi.web.query.QueryPredicate;
import com.gitssie.openapi.xentity.XEntityCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.ebean.*;
import io.ebean.bean.EntityBean;
import io.ebean.bean.EntityBeanIntercept;
import io.ebean.plugin.BeanType;
import io.ebean.plugin.Property;
import io.ebean.plugin.SpiServer;
import io.ebean.text.json.JsonContext;
import io.ebeaninternal.server.deploy.BeanDescriptor;
import io.ebeaninternal.server.deploy.BeanPropertyAssoc;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import javax.persistence.OptimisticLockException;
import java.util.*;

@Service(value = "com.gitssie.openapi.ebean.EbeanApi")
public class EbeanApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(EbeanApi.class);
    private final Database db;
    private final EbeanPredicateService predicateService;
    private final Provider provider;
    private final NodeConversionMap nodeConversionMap;
    private final ModelConverter modelConverter;
    private final QueryExpression queryExpression;

    private transient DataService dataService;

    public EbeanApi(Database db, EbeanPredicateService predicateService, Provider provider, NodeConversionMap nodeConversionMap, ModelConverter modelConverter) {
        this.db = db;
        this.predicateService = predicateService;
        this.provider = provider;
        this.nodeConversionMap = nodeConversionMap;
        this.modelConverter = modelConverter;
        this.queryExpression = new QueryExpression(db.expressionFactory());
    }

    public Database db() {
        return db;
    }

    public void setRollbackOnly() {
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
    }

    public ExpressionFactory expr() {
        return db.expressionFactory();
    }

    public ExpressionList where() {
        return new EbeanExpressionList(queryExpression, db.expressionFactory(), null);
    }

    public ExpressionList or() {
        Junction junction = db.expressionFactory().junction(Junction.Type.OR, queryExpression, null);
        return junction;
    }

    public SpiServer pluginApi() {
        return db.pluginApi();
    }

    public <T> BeanType<T> beanType(Class<T> beanClass) {
        return db.pluginApi().beanType(beanClass);
    }

    public <T> BeanType<T> desc(Class<T> beanClass) {
        return db.pluginApi().beanType(beanClass);
    }

    public <T> Class<T> beanClass(String apiKey) {
        return provider.beanClass(apiKey);
    }

    public <T> BeanType<T> desc(String apiKey) {
        return provider.desc(apiKey);
    }

    public Either<String, XEntityCache> getEntityIfPresent(String apiKey) {
        return provider.getEntityIfPresent(apiKey);
    }

    public Either<String, Class<?>> getBeanClassIfPresent(String apiKey) {
        return provider.getBeanClassIfPresent(apiKey);
    }

    public <T> Either<String, BeanType<T>> getBeanTypeIfPresent(String apiKey) {
        return provider.getBeanTypeIfPresent(apiKey);
    }

    public BeanState beanState(Object bean) {
        return db.beanState(bean);
    }

    public boolean isNotEmpty(Object value) {
        return !isEmpty(value);
    }

    public boolean isEmpty(Object value) {
        if (value != null && value instanceof EntityBean) {
            EntityBean bean = (EntityBean) value;
            EntityBeanIntercept ebi = bean._ebean_getIntercept();
            if (ebi.isReference()) {
                try {
                    db.pluginApi().loadBeanRef(bean._ebean_getIntercept());
                } catch (javax.persistence.EntityNotFoundException e) {
                    //Bean not found during lazy load or refresh
                    return true;
                }
            }
        }
        return ObjectUtils.isEmpty(value);
    }

    public Object beanId(Object bean) {
        return db.beanId(bean);
    }

    public Object beanId(Object bean, Object id) {
        return db.beanId(bean, id);
    }

    public <T> T createEntityBean(Class<T> type) {
        return db.createEntityBean(type);
    }

    public <T> UpdateQuery<T> update(Class<T> beanType) {
        return db.update(beanType);
    }

    public <T> Query<T> createQuery(Class<T> beanType) {
        return db.createQuery(beanType);
    }

    public <T> Query<T> createQuery(Class<T> beanType, String ormQuery) {
        return db.createQuery(beanType, ormQuery);
    }

    public <T> Query<T> find(Class<T> beanType) {
        return db.find(beanType);
    }

    public <T> Query<T> findNative(Class<T> beanType, String nativeSql) {
        return db.findNative(beanType, nativeSql);
    }

    public Object nextId(Class<?> beanType) {
        return db.nextId(beanType);
    }

    public <T> Filter<T> filter(Class<T> beanType) {
        return db.filter(beanType);
    }

    public <T> void sort(List<T> list, String sortByClause) {
        db.sort(list, sortByClause);
    }

    public <T> DtoQuery<T> findDto(Class<T> dtoType, String sql) {
        return db.findDto(dtoType, sql);
    }

    public void refresh(Object bean) {
        db.refresh(bean);
    }

    public void refreshMany(Object bean, String propertyName) {
        db.refreshMany(bean, propertyName);
    }

    public <T> T find(Class<T> beanType, Object id) {
        return db.find(beanType, id);
    }

    public <T> T findOne(Class<T> beanType, Object id) {
        return db.find(beanType, id);
    }

    public <T> T find(Class<T> beanType, Expression where) {
        return db.createQuery(beanType).where(where).setMaxRows(1).findOne();
    }

    public <T> T findOne(Class<T> beanType, Expression where) {
        return find(beanType, where);
    }

    public <T> T findOne(String apiKey, Expression where) {
        return find(beanClass(apiKey), where);
    }

    public <T> T findOne(String apiKey, ExpressionList<T> where) {
        return findOne(beanClass(apiKey), where);
    }

    public <T> T find(Class<T> beanType, ExpressionList<T> where) {
        return db.createQuery(beanType).where().addAll(where).setMaxRows(1).findOne();
    }

    public <T> T findOne(Class<T> beanType, ExpressionList<T> where) {
        return find(beanType, where);
    }


    public <T> T ref(Class<T> beanType, Object id) {
        return ref(beanType, id);
    }

    public <T> T reference(Class<T> beanType, Object id) {
        return db.reference(beanType, id);
    }

    public void save(Object bean) throws OptimisticLockException {
        db.save(bean);
    }

    public int saveAll(Collection<?> beans) throws OptimisticLockException {
        return db.saveAll(beans);
    }

    public int saveAll(Object... beans) throws OptimisticLockException {
        return db.saveAll(beans);
    }

    public boolean delete(Object bean) throws OptimisticLockException {
        return db.delete(bean);
    }

    public int delete(Class<?> beanType, Expression where) {
        return db.createQuery(beanType).where(where).delete();
    }

    public int delete(Class<?> beanType, ExpressionList where) {
        return db.createQuery(beanType).where().addAll(where).delete();
    }

    public int delete(Class<?> beanType, Object id) {
        return db.delete(beanType, id);
    }

    public int deleteAll(Collection<?> beans) throws OptimisticLockException {
        return db.deleteAll(beans);
    }

    public int deleteAll(Class<?> beanType, Collection<?> ids) {
        return db.deleteAll(beanType, ids);
    }

    public void insert(Object bean) {
        db.insert(bean);
    }

    public void insertAll(Collection<?> beans) {
        db.insertAll(beans);
    }

    public int execute(SqlUpdate sqlUpdate) {
        return db.execute(sqlUpdate);
    }

    public int execute(Update<?> update) {
        return db.execute(update);
    }

    public int execute(CallableSql callableSql) {
        return db.execute(callableSql);
    }

    public Set<Property> checkUniqueness(Object bean) {
        return db.checkUniqueness(bean);
    }

    public void markAsDirty(Object bean) {
        db.markAsDirty(bean);
    }

    public void update(Object bean) throws OptimisticLockException {
        db.update(bean);
    }

    public void update(Object bean, Transaction transaction) throws OptimisticLockException {
        db.update(bean, transaction);
    }

    public void updateAll(Collection<?> beans) throws OptimisticLockException {
        db.updateAll(beans);
    }

    public JsonContext json() {
        return db.json();
    }

    public <T> Set<String> validateQuery(Query<T> query) {
        return db.validateQuery(query);
    }

    public void lock(Object bean) {
        db.lock(bean);
    }

    public Property idProperty(BeanType<?> beanType) {
        return beanType.idProperty();
    }

    public Property codeProperty(BeanType<?> beanType) {
        return provider.codeProperty(beanType);
    }

    public Property tenantProperty(BeanType<?> beanType) {
        BeanDescriptor<?> desc = (BeanDescriptor) beanType;
        return desc.tenantProperty();
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, PagedList<T>> query(Class<T> beanClass, QueryPredicate queryPredicate, Pageable page) {
        return query(beanClass, queryPredicate, page, page.getSort());
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, PagedList<T>> query(Class<T> beanClass, QueryPredicate queryPredicate, Pageable page, Sort sort) {
        BeanType<T> desc = db.pluginApi().beanType(beanClass);
        return query(desc, queryPredicate, page, sort);
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, Page<T>> queryPage(Class<T> beanClass, QueryPredicate queryPredicate, Pageable page) {
        return queryPage(beanClass, queryPredicate, page, page.getSort());
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, Slice<T>> querySlice(Class<T> beanClass, QueryPredicate queryPredicate, Pageable page) {
        return querySlice(beanClass, queryPredicate, page, page.getSort());
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, Slice<T>> querySlice(Class<T> beanClass, QueryPredicate queryPredicate, Pageable page, Sort sort) {
        return query(beanClass, queryPredicate, page, sort).map(e -> toSlice(e, page));
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, Page<T>> queryPage(Class<T> beanClass, QueryPredicate queryPredicate, Pageable page, Sort sort) {
        return query(beanClass, queryPredicate, page, sort).map(e -> toPage(e, page));
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, List<T>> list(Class<T> beanClass, QueryPredicate queryPredicate) {
        return list(beanClass, queryPredicate, Pageable.unpaged(), Sort.unsorted());
    }

    @Transactional(readOnly = true)
    public <T> List<T> listById(Class<T> beanClass, Collection<?> id) {
        BeanType<T> desc = this.desc(beanClass);
        return list(beanClass, expr().in(desc.idProperty().name(), id), Pageable.unpaged(), Sort.unsorted());
    }

    @Transactional(readOnly = true)
    public <T> List<T> listByCode(Class<T> beanClass, Collection<?> codes) {
        Property codeProperty = provider.codeProperty(desc(beanClass));
        if (codeProperty == null) {
            throw new IllegalStateException("codeProperty is unset in class " + beanClass.getName());
        }
        return list(beanClass, expr().in(codeProperty.name(), codes), Pageable.unpaged(), Sort.unsorted());
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, List<T>> list(Class<T> beanClass, QueryPredicate queryPredicate, Pageable page) {
        return list(beanClass, queryPredicate, page, page.getSort());
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, List<T>> list(Class<T> beanClass, QueryPredicate queryPredicate, Pageable page, Sort sort) {
        return query(beanClass, queryPredicate, page, sort).map(e -> e.getList());
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, List<T>> list(BeanType<T> desc, QueryPredicate queryPredicate, Pageable page, Sort sort) {
        return query(desc, queryPredicate, page, sort).map(e -> e.getList());
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, Page<T>> queryPage(BeanType<T> desc, QueryPredicate queryPredicate, Pageable page) {
        return queryPage(desc, queryPredicate, page, page.getSort());
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, Slice<T>> querySlice(BeanType<T> desc, QueryPredicate queryPredicate, Pageable page) {
        return querySlice(desc, queryPredicate, page, page.getSort());
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, Slice<T>> querySlice(BeanType<T> desc, QueryPredicate queryPredicate, Pageable page, Sort sort) {
        return query(desc, queryPredicate, page, sort).map(e -> toSlice(e, page));
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, Page<T>> queryPage(BeanType<T> desc, QueryPredicate queryPredicate, Pageable page, Sort sort) {
        return query(desc, queryPredicate, page, sort).map(e -> toPage(e, page));
    }

    @Transactional(readOnly = true)
    public <T> int findCount(Class<T> beanClass, Expression where) {
        Query<T> query = db.createQuery(beanClass);
        if (where != null) {
            query.where(where);
        }
        return query.findCount();
    }

    public Either<Code, List<Expression>> parsePredicate(BeanType desc, List<PredicateField> fields) {
        return predicateService.parsePredicate(desc, fields).mapLeft(e -> Code.INVALID_ARGUMENT.withMessage(e));
    }

    public Either<Code, Expression> parsePredicate(BeanType desc, AbstractQuery queryMap) {
        return predicateService.parsePredicate(desc, queryMap).mapLeft(e -> Code.INVALID_ARGUMENT.withMessage(e));
    }

    public Either<Code, Expression> parseExpression(String expr, List<Expression> predicateList) {
        return predicateService.parseExpression(expr, predicateList).mapLeft(e -> Code.INVALID_ARGUMENT.withMessage(e));
    }

    public <T> Either<Code, Expression> parsePredicate(BeanType<T> desc, QueryPredicate queryPredicate) {
        return predicateService.parsePredicate(desc, queryPredicate.getPredicate()).flatMap(p -> predicateService.parseExpression(queryPredicate.getExpression(), p)).mapLeft(e -> Code.INVALID_ARGUMENT.withMessage(e));
    }

    public <T> Either<Code, Tuple2<Optional<Property>, String>> parseAggregationProperties(BeanType<T> desc, List<String> aggre) {
        return predicateService.parseAggregationProperties(desc, aggre);
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, PagedList<T>> query(BeanType<T> desc, QueryPredicate queryPredicate, Pageable pageable, Sort sort) {
        Query<T> query = db.createQuery(desc.type());
        if (queryPredicate != null && ObjectUtils.isNotEmpty(queryPredicate.getPredicate())) {
            Either<Code, Expression> predicate = parsePredicate(desc, queryPredicate);
            if (predicate.isLeft()) {
                return (Either) predicate;
            }
            query.where(predicate.get());
        }
        return Either.right(doQuery(query, pageable, sort));
    }

    @Transactional(readOnly = true)
    public <T> PagedList<T> query(Class<T> clazz, Expression expression, Pageable pageable) {
        return query(clazz, null, expression, pageable, pageable.getSort());
    }

    @Transactional(readOnly = true)
    public <T> PagedList<T> query(Class<T> clazz, ExpressionList<T> expression, Pageable pageable) {
        return query(clazz, null, expression, pageable, pageable.getSort());
    }

    @Transactional(readOnly = true)
    public <T> PagedList<T> query(Class<T> clazz, Expression expression, Pageable pageable, Sort sort) {
        return query(clazz, null, expression, pageable, sort);
    }

    @Transactional(readOnly = true)
    public <T> PagedList<T> query(Class<T> clazz, ExpressionList<T> expression, Pageable pageable, Sort sort) {
        return query(clazz, null, expression, pageable, sort);
    }


    @Transactional(readOnly = true)
    public <T> Page<T> queryPage(Class<T> clazz, Expression expression, Pageable pageable) {
        return toPage(query(clazz, null, expression, pageable, pageable.getSort()), pageable);
    }

    @Transactional(readOnly = true)
    public <T> Page<T> queryPage(Class<T> clazz, Expression expression, Pageable pageable, Sort sort) {
        return toPage(query(clazz, null, expression, pageable, sort), pageable);
    }

    @Transactional(readOnly = true)
    public <T> Page<T> queryPage(Class<T> clazz, ExpressionList<T> expression, Pageable pageable) {
        return toPage(query(clazz, null, expression, pageable, pageable.getSort()), pageable);
    }

    @Transactional(readOnly = true)
    public <T> Page<T> queryPage(Class<T> clazz, ExpressionList<T> expression, Pageable pageable, Sort sort) {
        return toPage(query(clazz, null, expression, pageable, sort), pageable);
    }

    @Transactional(readOnly = true)
    public <T> Slice<T> querySlice(Class<T> clazz, Expression expression, Pageable pageable) {
        return toSlice(query(clazz, null, expression, pageable, pageable.getSort()), pageable);
    }

    @Transactional(readOnly = true)
    public <T> Slice<T> querySlice(Class<T> clazz, Expression expression, Pageable pageable, Sort sort) {
        return toSlice(query(clazz, null, expression, pageable, sort), pageable);
    }

    @Transactional(readOnly = true)
    public <T> Slice<T> querySlice(Class<T> clazz, ExpressionList<T> expression, Pageable pageable) {
        return toSlice(query(clazz, null, expression, pageable, pageable.getSort()), pageable);
    }

    @Transactional(readOnly = true)
    public <T> Slice<T> querySlice(Class<T> clazz, ExpressionList<T> expression, Pageable pageable, Sort sort) {
        return toSlice(query(clazz, null, expression, pageable, sort), pageable);
    }

    @Transactional(readOnly = true)
    public <T> List<T> list(Class<T> clazz) {
        return list(clazz, (Expression) null, Pageable.unpaged(), Sort.unsorted());
    }

    @Transactional(readOnly = true)
    public <T> List<T> list(Class<T> clazz, Expression expression) {
        return list(clazz, expression, Pageable.unpaged(), Sort.unsorted());
    }

    @Transactional(readOnly = true)
    public <T> List<T> list(String apiKey, Expression expression) {
        return list(beanClass(apiKey), expression);
    }

    @Transactional(readOnly = true)
    public <T> List<T> list(Class<T> clazz, ExpressionList<T> expression) {
        return list(clazz, expression, Pageable.unpaged(), Sort.unsorted());
    }

    @Transactional(readOnly = true)
    public <T> List<T> list(String apiKey, ExpressionList<T> expression) {
        return list(beanClass(apiKey), expression);
    }

    @Transactional(readOnly = true)
    public <T> List<T> list(Class<T> clazz, Expression expression, Pageable pageable) {
        return list(clazz, expression, pageable, pageable.getSort());
    }

    @Transactional(readOnly = true)
    public <T> List<T> list(Class<T> clazz, ExpressionList<T> expression, Pageable pageable) {
        return list(clazz, expression, pageable, pageable.getSort());
    }

    @Transactional(readOnly = true)
    public <T> List<T> list(Class<T> clazz, Expression expression, Pageable pageable, Sort sort) {
        return list(clazz, null, expression, pageable, sort);
    }

    @Transactional(readOnly = true)
    public <T> List<T> list(Class<T> clazz, ExpressionList<T> expression, Pageable pageable, Sort sort) {
        return list(clazz, null, expression, pageable, sort);
    }

    @Transactional(readOnly = true)
    public <T> List<T> list(Class<T> beanClass, String select, Expression expression, Pageable pageable, Sort sort) {
        return query(beanClass, select, expression, pageable, sort).getList();
    }

    @Transactional(readOnly = true)
    public <T> List<T> list(Class<T> beanClass, String select, ExpressionList<T> expression, Pageable pageable, Sort sort) {
        return query(beanClass, select, expression, pageable, sort).getList();
    }

    @Transactional(readOnly = true)
    public <T> Page<T> queryPage(Class<T> beanClass, String select, Expression expression, Pageable pageable, Sort sort) {
        return toPage(query(beanClass, select, expression, pageable, sort), pageable);
    }

    @Transactional(readOnly = true)
    public <T> Page<T> queryPage(Class<T> beanClass, String select, ExpressionList<T> expression, Pageable pageable, Sort sort) {
        return toPage(query(beanClass, select, expression, pageable, sort), pageable);
    }

    @Transactional(readOnly = true)
    public <T> Slice<T> querySlice(Class<T> beanClass, String select, Expression expression, Pageable pageable, Sort sort) {
        return toSlice(query(beanClass, select, expression, pageable, sort), pageable);
    }

    @Transactional(readOnly = true)
    public <T> Slice<T> querySlice(Class<T> beanClass, String select, ExpressionList<T> expression, Pageable pageable, Sort sort) {
        return toSlice(query(beanClass, select, expression, pageable, sort), pageable);
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, Page<Map<String, Object>>> queryMap(Class<T> beanClass, Model model, QueryPredicate queryPredicate, Pageable page, Sort sort) {
        return queryMap(desc(beanClass), model, queryPredicate, page, sort);
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, Page<Map<String, Object>>> queryMap(BeanType<T> desc, Model model, QueryPredicate queryPredicate, Pageable page, Sort sort) {
        Fetch fetch = model.fetchList(desc, provider);
        Either<Code, PagedList<T>> pagedList = queryMap(desc, fetch, queryPredicate, page, sort);
        return pagedList.map(e -> {
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) fetch.toJSON(e.getList());
            if (page.isPaged() && page.getPageNumber() > 0) {
                return new PageImpl(dataList);
            }
            return new PageImpl(dataList, page, e.getTotalCount());
        });
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, Page<Map<String, Object>>> queryMap(BeanType<T> desc, Model model, ExpressionList<T> expression, Pageable page, Sort sort) {
        Fetch fetch = model.fetchList(desc, provider);
        Either<Code, PagedList<T>> pagedList = queryMap(desc, fetch, expression, page, sort);
        return pagedList.map(e -> {
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) fetch.toJSON(e.getList());
            if (page.isPaged() && page.getPageNumber() > 0) {
                return new PageImpl(dataList);
            }
            return new PageImpl(dataList, page, e.getTotalCount());
        });
    }

    protected <T> Either<Code, PagedList<T>> queryMap(BeanType<T> desc, Fetch fetch, QueryPredicate queryPredicate, Pageable page, Sort sort) {
        Expression where = null;
        if (queryPredicate != null && ObjectUtils.isNotEmpty(queryPredicate.getPredicate())) {
            Either<String, Expression> predicate = predicateService.parsePredicate(desc, queryPredicate.getPredicate()).flatMap(p -> predicateService.parseExpression(queryPredicate.getExpression(), p));
            if (predicate.isLeft()) {
                return (Either) predicate;
            }
            where = predicate.get();
        }
        return queryMap(fetch, where, page, sort);
    }

    protected <T> Either<Code, PagedList<T>> queryMap(BeanType<T> desc, Fetch fetch, ExpressionList<T> expression, Pageable page, Sort sort) {
        return queryMap(fetch, expression, page, sort);
    }

    protected <T> Either<Code, PagedList<T>> queryMap(Fetch fetch, Expression expression, Pageable page, Sort sort) {
        fetch.and(expression);
        PagedList pagedList = fetch.fetch(query -> {
            return doQuery(query, page, sort);
        });
        return Either.right(pagedList);
    }

    protected <T> Either<Code, PagedList<T>> queryMap(Fetch fetch, ExpressionList expression, Pageable page, Sort sort) {
        PagedList pagedList = fetch.fetch(query -> {
            query.where().addAll(expression);
            return doQuery(query, page, sort);
        });
        return Either.right(pagedList);
    }

    @Transactional(readOnly = true)
    public <T> PagedList<T> query(Class<T> beanClass, String select, Expression expression, Pageable pageable, Sort sort) {
        Query<T> query = db.createQuery(beanClass);
        if (StringUtils.isNotEmpty(select)) {
            query.select(select);
        }
        if (expression != null) {
            query.where().add(expression);
        }
        return doQuery(query, pageable, sort);
    }

    @Transactional(readOnly = true)
    public <T> PagedList<T> query(Class<T> beanClass, String select, ExpressionList<T> expression, Pageable pageable, Sort sort) {
        Query<T> query = db.createQuery(beanClass);
        if (StringUtils.isNotEmpty(select)) {
            query.select(select);
        }
        if (expression != null) {
            query.where().addAll(expression);
        }
        return doQuery(query, pageable, sort);
    }

    @Transactional(readOnly = true)
    public <T> PagedList<T> doQuery(Query<T> query, Pageable pageable) {
        return doQuery(query, pageable, pageable.getSort());
    }

    @Transactional(readOnly = true)
    public <T> PagedList<T> doQuery(Query<T> query, Pageable pageable, Sort sort) {
        if (sort != null && sort.isSorted()) {
            for (Sort.Order e : sort) {
                if (e.isDescending()) {
                    query.orderBy().desc(e.getProperty());
                } else {
                    query.orderBy().asc(e.getProperty());
                }
            }
        }

        if (pageable != null && pageable.isPaged()) {
            query.setFirstRow((int) pageable.getOffset()).setMaxRows(pageable.getPageSize());
            PagedList<T> pagedList = query.findPagedList();
            return pagedList;
        } else {
            List<T> pagedList = query.findList();
            return new PagedListImpl<>(pagedList);
        }
    }

    public <T> Either<Code, Object> queryMapAggre(BeanType<T> desc, Expression dataPermission, QueryPredicate queryPredicate, Pageable page, AggreModel aggre) {
        return dataService.queryMapAggre(desc, dataPermission, queryPredicate, page, aggre);
    }

    @Transactional(readOnly = true)
    public <T> T findBeanByCode(Class<?> beanClass, Object code) {
        Property codeProperty = provider.codeProperty(desc(beanClass));
        if (codeProperty == null) {
            throw new IllegalStateException("codeProperty is unset in class " + beanClass.getName());
        }
        return findBeanByCode(beanClass, codeProperty, code);
    }

    @Transactional(readOnly = true)
    public <T> T findBeanByCode(Class<?> beanClass, Property codeProperty, Object code) {
        return (T) db.createQuery(beanClass).where().eq(codeProperty.name(), code).setMaxRows(1).findOne();
    }

    @Transactional(readOnly = true)
    public <T> T referenceById(BeanType<T> desc, Object id) {
        return (T) db.createQuery(desc.type()).select(desc.idProperty().name()).setId(id).findOne();
    }

    @Transactional(readOnly = true)
    public <T> T referenceByCode(BeanType<T> desc, Property codeProperty, Object code) {
        return (T) db.createQuery(desc.type()).select(desc.idProperty().name()).where().eq(codeProperty.name(), code).setMaxRows(1).findOne();
    }

    public Sort sortById(Class<?> beanClass, Sort.Direction asc) {
        return Sort.by(asc, beanType(beanClass).idProperty().name());
    }

    public Sort sortByIdAsc(Class<?> beanClass) {
        return sortById(beanClass, Sort.Direction.ASC);
    }

    public Sort sortByIdDesc(Class<?> beanClass) {
        return sortById(beanClass, Sort.Direction.DESC);
    }

    public <T> Page<T> toPage(PagedList<T> pagedList, Pageable pageable) {
        if (pageable != null && pageable.isPaged()) {
            Page<T> page = new PageImpl<>(pagedList.getList(), pageable, pagedList.getTotalCount());
            return page;
        } else {
            List<T> list = pagedList.getList();
            Page<T> page = new PageImpl<>(list, Pageable.unpaged(), list.size());
            return page;
        }
    }

    public <T> Slice<T> toSlice(PagedList<T> pagedList, Pageable pageable) {
        if (pageable != null && pageable.isPaged()) {
            List<T> list = pagedList.getList();
            boolean hasNext = list.size() >= pageable.getPageSize();
            Slice<T> page = new SliceImpl<>(list, pageable, hasNext);
            return page;
        } else {
            List<T> list = pagedList.getList();
            Slice<T> page = new SliceImpl<>(list, Pageable.unpaged(), false);
            return page;
        }
    }

    public <T> T createBean(String apiKey) {
        return (T) desc(apiKey).createBean();
    }

    public <T> T createBean(Class<T> beanClass) {
        return provider.createBean(beanClass);
    }

    public <T> Either<Code, T> createBean(Class<T> beanClass, final Map source) {
        return createBean(beanClass, source, false);
    }

    public <T> Either<Code, T> createBean(Class<T> beanClass, final Map source, boolean updateOnly) {
        return provider.createBean(beanClass, source, updateOnly);
    }

    public Object copyTo(Object source, Class<?> clazz) {
        try {
            return nodeConversionMap.copyTo(source, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Object copyTo(Object source, Object target) {
        try {
            return nodeConversionMap.copyTo(source, target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> Either<Code, T> copy(final Map source, final T bean) {
        NeedContext context = new NeedContext();
        try (context) {
            return copy(context, desc(bean.getClass()), source, bean);
        }
    }

    public <T> Either<Code, T> copy(BeanType<?> desc, final Map source, final T bean) {
        NeedContext context = new NeedContext();
        try (context) {
            return copy(context, desc, source, bean);
        }
    }

    public <T> Either<Code, T> copy(NeedContext context, BeanType<?> desc, final Map source, final T bean) {
        return Try.of(() -> {
            return (T) nodeConversionMap.copy(context, provider, desc, source, (EntityBean) bean);
        }).toEither().mapLeft(e -> {
            LOGGER.error("copy map source:{} to bean:{} cause", source, bean, e);
            return Code.INTERNAL.withErrors(e);
        });
    }

    public <T> Map findSingleMap(Class<T> beanClass, String select, Expression where) {
        return db.find(beanClass).select(select).where().add(where).asDto(Map.class).findOne();
    }


    @Transactional(readOnly = true)
    public Either<Code, SqlRow> sqlOne(String apiKey, String funcName, Map<String, Object> queryParams, Pageable page) {
        return dataService.sqlQuery(apiKey, funcName, queryParams, page, page.getSort()).map(e -> e.findOne());
    }

    @Transactional(readOnly = true)
    public Either<Code, List<SqlRow>> sqlList(String apiKey, String funcName, Map<String, Object> queryParams, Pageable page) {
        return dataService.sqlQuery(apiKey, funcName, queryParams, page, page.getSort()).map(e -> e.findList());
    }

    @Transactional(readOnly = true)
    public Either<Code, Page<SqlRow>> sqlPage(String apiKey, String funcName, Map<String, Object> queryParams, Pageable page) {
        return dataService.sqlQuery(apiKey, funcName, queryParams, page, page.getSort()).map(e -> e.findPage());
    }

    @Transactional(readOnly = true)
    public Either<Code, TypeQuery<SqlRow>> sqlQuery(String apiKey, String funcName, Map<String, Object> queryParams, Pageable page) {
        return dataService.sqlQuery(apiKey, funcName, queryParams, page, page.getSort());
    }

    @Transactional(readOnly = true)
    public Either<Code, TypeQuery<SqlRow>> sqlQuery(String apiKey, String funcName, Map<String, Object> queryParams, Pageable page, Sort sort) {
        return dataService.sqlQuery(apiKey, funcName, queryParams, page, sort);
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, Object> queryRawSql(Map<String, Object> scope, Pageable page, AggreModel aggre, Option<Model> table) {
        return dataService.queryRawSql(scope, page, aggre, table);
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, Object> queryRawSql(FetchContext context, Map<String, Object> scope, Pageable page, AggreModel aggre, Option<Model> table) {
        return dataService.queryRawSql(context, scope, page, aggre, table);
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, T> queryAggre(Class<T> beanClass, Class<T> dtoClass, QueryPredicate queryPredicate, String aggre) {
        return queryAggre(desc(beanClass), dtoClass, null, queryPredicate, aggre);
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, T> queryAggre(Class<T> beanClass, Class<T> dtoClass, ExpressionList<T> expression, String aggre) {
        return queryAggre(desc(beanClass), dtoClass, expression, aggre);
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, T> queryAggre(BeanType<?> desc, Class<T> dtoClass, Expression filter, QueryPredicate queryPredicate, String aggre) {
        return parsePredicate(desc, queryPredicate).flatMap(where -> {
            if (where == null) {
                where = filter;
            } else if (filter != null) {
                where = Expr.and(where, filter);
            }
            return queryAggre(desc, dtoClass, where, aggre);
        });
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, T> queryAggre(BeanType<?> desc, Class<T> dtoClass, ExpressionList<T> where, String aggre) {
        return queryAggre(desc, dtoClass, (Expression) where, aggre);
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, T> queryAggre(Class<T> beanClass, Class<T> dtoClass, Expression where, String aggre) {
        return queryAggre(desc(beanClass), dtoClass, where, aggre);
    }

    @Transactional(readOnly = true)
    public <T> Either<Code, T> queryAggre(BeanType<?> desc, Class<T> dtoClass, Expression where, String aggre) {
        AggreModel model = new AggreModel(Lists.newArrayList(aggre.split(",")));
        model.setDtoClass(dtoClass);
        return (Either) dataService.queryMapAggre(desc, where, null, Pageable.unpaged(), model);
    }

    public Object toMap(FetchContext context, Model model, BeanType<?> desc, Object bean) {
        return modelConverter.toMap(context, model, desc, bean);
    }

    public <T> Map<String, Object> idToMap(T bean) {
        BeanType<?> desc = provider.desc(bean.getClass());
        Map<String, Object> result = Maps.newHashMapWithExpectedSize(2);
        result.put(desc.idProperty().name(), desc.idProperty().value(bean));
        return result;
    }

    public <T> Map<String, Object> toJSON(T bean) {
        return modelConverter.toJSON((EntityBean) bean);
    }

    public <T extends EntityBean> List<Map<String, Object>> toJSON(Class<T> clazz, Collection<T> beanList) {
        return modelConverter.toJSON(clazz, beanList);
    }

    public <T> Page<Map<String, Object>> toJSON(Page<T> pagedList) {
        if (pagedList.isEmpty()) {
            return Page.empty();
        }
        return modelConverter.toJSON((Page) pagedList);
    }

    public <T> List<Map<String, Object>> toJSON(Collection<T> beanList) {
        return modelConverter.toJSON((Collection) beanList);
    }

    public Map<String, Object> toJSONMap(Model model, Map<String, Object> bean) {
        return modelConverter.toJSONMap(model, bean);
    }

    public Page<Map<String, Object>> toJSONMap(Model model, Page<Map<String, Object>> pagedList) {
        if (pagedList.isEmpty()) {
            return Page.empty();
        }
        return modelConverter.toJSONMap(model, pagedList);
    }

    public List<Map<String, Object>> toJSONMap(Model model, List<Map<String, Object>> beanList) {
        return modelConverter.toJSONMap(model, beanList);
    }


    /**
     * Perform processing just prior to the transaction commit.
     *
     * @param callback
     */
    public void preCommit(Runnable callback) {
        preCommit(db.currentTransaction(), callback);
    }

    /**
     * Perform processing just prior to the transaction commit.
     *
     * @param callback
     */
    public static void preCommit(Transaction transaction, Runnable callback) {
        transaction.register(new TransactionCallbackAdapter() {
            @Override
            public void preCommit() {
                callback.run();
            }
        });
    }

    /**
     * Perform processing just after the transaction commit.
     *
     * @param callback
     */
    public void postCommit(Runnable callback) {
        postCommit(db.currentTransaction(), callback);
    }

    /**
     * Perform processing just after the transaction commit.
     *
     * @param callback
     */
    public static void postCommit(Transaction transaction, Runnable callback) {
        transaction.register(new TransactionCallbackAdapter() {
            @Override
            public void postCommit() {
                callback.run();
            }
        });
    }

    /**
     * Perform processing just prior to the transaction rollback.
     *
     * @param callback
     */
    public void preRollback(Runnable callback) {
        preRollback(db.currentTransaction(), callback);
    }

    public static void preRollback(Transaction transaction, Runnable callback) {
        transaction.register(new TransactionCallbackAdapter() {
            @Override
            public void preRollback() {
                callback.run();
            }
        });
    }

    /**
     * Perform processing just after the transaction rollback.
     *
     * @param callback
     */
    public void postRollback(Runnable callback) {
        postRollback(db.currentTransaction(), callback);
    }

    public static void postRollback(Transaction transaction, Runnable callback) {
        transaction.register(new TransactionCallbackAdapter() {
            @Override
            public void postRollback() {
                callback.run();
            }
        });
    }

    public void setDataService(DataService dataService) {
        this.dataService = dataService;
    }
}
