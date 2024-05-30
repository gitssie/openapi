package com.gitssie.openapi.ebean;

import io.ebean.*;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author: Awesome
 * @create: 2024-03-26 16:13
 */
public class QueryExpression implements Query {
    private ExpressionFactory exprFactory;

    public QueryExpression(ExpressionFactory exprFactory) {
        this.exprFactory = exprFactory;
    }

    @Override
    public Query setRawSql(RawSql rawSql) {
        return null;
    }

    @Override
    public Query asOf(Timestamp asOf) {
        return null;
    }

    @Override
    public Query asDraft() {
        return null;
    }

    @Override
    public UpdateQuery asUpdate() {
        return null;
    }

    @Override
    public Query copy() {
        return null;
    }

    @Override
    public Query setPersistenceContextScope(PersistenceContextScope scope) {
        return null;
    }

    @Override
    public Query setDocIndexName(String indexName) {
        return null;
    }

    @Override
    public ExpressionFactory getExpressionFactory() {
        return exprFactory;
    }

    @Override
    public boolean isAutoTuned() {
        return false;
    }

    @Override
    public Query setAutoTune(boolean autoTune) {
        return null;
    }

    @Override
    public Query setAllowLoadErrors() {
        return null;
    }

    @Override
    public Query setLazyLoadBatchSize(int lazyLoadBatchSize) {
        return null;
    }

    @Override
    public Query setIncludeSoftDeletes() {
        return null;
    }

    @Override
    public Query setDisableReadAuditing() {
        return null;
    }

    @Override
    public Query select(String fetchProperties) {
        return null;
    }

    @Override
    public Query select(FetchGroup fetchGroup) {
        return null;
    }

    @Override
    public Query fetch(String path, String fetchProperties) {
        return null;
    }

    @Override
    public Query fetchQuery(String path, String fetchProperties) {
        return null;
    }

    @Override
    public Query fetchCache(String path, String fetchProperties) {
        return null;
    }

    @Override
    public Query fetchLazy(String path, String fetchProperties) {
        return null;
    }

    @Override
    public Query fetch(String path, String fetchProperties, FetchConfig fetchConfig) {
        return null;
    }

    @Override
    public Query fetch(String path) {
        return null;
    }

    @Override
    public Query fetchQuery(String path) {
        return null;
    }

    @Override
    public Query fetchCache(String path) {
        return null;
    }

    @Override
    public Query fetchLazy(String path) {
        return null;
    }

    @Override
    public Query fetch(String path, FetchConfig fetchConfig) {
        return null;
    }

    @Override
    public Query apply(FetchPath fetchPath) {
        return null;
    }

    @Override
    public Query usingTransaction(Transaction transaction) {
        return null;
    }

    @Override
    public Query usingConnection(Connection connection) {
        return null;
    }

    @Override
    public Query usingDatabase(Database database) {
        return null;
    }

    @Override
    public QueryIterator findIterate() {
        return null;
    }

    @Override
    public Stream findStream() {
        return null;
    }

    @Override
    public void findEach(Consumer consumer) {

    }

    @Override
    public void findEachWhile(Predicate consumer) {

    }

    @Override
    public List findList() {
        return null;
    }

    @Override
    public Set findSet() {
        return null;
    }

    @Override
    public boolean isCountDistinct() {
        return false;
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public Object findOne() {
        return null;
    }

    @Override
    public Optional findOneOrEmpty() {
        return Optional.empty();
    }

    @Override
    public List<Version> findVersions() {
        return null;
    }

    @Override
    public List<Version> findVersionsBetween(Timestamp start, Timestamp end) {
        return null;
    }

    @Override
    public int delete() {
        return 0;
    }

    @Override
    public int delete(Transaction transaction) {
        return 0;
    }

    @Override
    public int update() {
        return 0;
    }

    @Override
    public int update(Transaction transaction) {
        return 0;
    }

    @Override
    public int findCount() {
        return 0;
    }

    @Override
    public FutureRowCount findFutureCount() {
        return null;
    }

    @Override
    public FutureIds findFutureIds() {
        return null;
    }

    @Override
    public FutureList findFutureList() {
        return null;
    }

    @Override
    public PagedList findPagedList() {
        return null;
    }

    @Override
    public Query setParameter(String name, Object value) {
        return null;
    }

    @Override
    public Query setParameter(int position, Object value) {
        return null;
    }

    @Override
    public Query setParameter(Object value) {
        return null;
    }

    @Override
    public Query setParameters(Object... values) {
        return null;
    }

    @Override
    public Query setId(Object id) {
        return null;
    }

    @Override
    public Object getId() {
        return null;
    }

    @Override
    public Query where(Expression expression) {
        return null;
    }

    @Override
    public ExpressionList where() {
        return null;
    }

    @Override
    public ExpressionList text() {
        return null;
    }

    @Override
    public ExpressionList filterMany(String propertyName) {
        return null;
    }

    @Override
    public ExpressionList having() {
        return null;
    }

    @Override
    public Query having(Expression addExpressionToHaving) {
        return null;
    }

    @Override
    public Query orderBy(String orderByClause) {
        return null;
    }

    @Override
    public OrderBy orderBy() {
        return null;
    }

    @Override
    public Query setOrderBy(OrderBy orderBy) {
        return null;
    }

    @Override
    public Query setDistinct(boolean isDistinct) {
        return null;
    }

    @Override
    public Query setCountDistinct(CountDistinctOrder orderBy) {
        return null;
    }

    @Override
    public int getFirstRow() {
        return 0;
    }

    @Override
    public Query setFirstRow(int firstRow) {
        return null;
    }

    @Override
    public int getMaxRows() {
        return 0;
    }

    @Override
    public Query setMaxRows(int maxRows) {
        return null;
    }

    @Override
    public Query setMapKey(String mapKey) {
        return null;
    }

    @Override
    public Query setBeanCacheMode(CacheMode beanCacheMode) {
        return null;
    }

    @Override
    public Query setUseQueryCache(CacheMode queryCacheMode) {
        return null;
    }

    @Override
    public Query setProfileLocation(ProfileLocation profileLocation) {
        return null;
    }

    @Override
    public Query setLabel(String label) {
        return null;
    }

    @Override
    public Query setUseDocStore(boolean useDocStore) {
        return null;
    }

    @Override
    public Query setReadOnly(boolean readOnly) {
        return null;
    }

    @Override
    public Query setLoadBeanCache(boolean loadBeanCache) {
        return null;
    }

    @Override
    public Query setTimeout(int secs) {
        return null;
    }

    @Override
    public Query setBufferFetchSizeHint(int fetchSize) {
        return null;
    }

    @Override
    public String getGeneratedSql() {
        return null;
    }

    @Override
    public Query withLock(LockType lockType) {
        return null;
    }

    @Override
    public Query withLock(LockType lockType, LockWait lockWait) {
        return null;
    }

    @Override
    public Query forUpdate() {
        return null;
    }

    @Override
    public Query forUpdateNoWait() {
        return null;
    }

    @Override
    public Query forUpdateSkipLocked() {
        return null;
    }

    @Override
    public boolean isForUpdate() {
        return false;
    }

    @Override
    public LockWait getForUpdateLockWait() {
        return null;
    }

    @Override
    public LockType getForUpdateLockType() {
        return null;
    }

    @Override
    public Query alias(String alias) {
        return null;
    }

    @Override
    public Query setBaseTable(String baseTable) {
        return null;
    }

    @Override
    public Class getBeanType() {
        return null;
    }

    @Override
    public Query setInheritType(Class type) {
        return null;
    }

    @Override
    public Class getInheritType() {
        return null;
    }

    @Override
    public QueryType getQueryType() {
        return null;
    }

    @Override
    public Query setDisableLazyLoading(boolean disableLazyLoading) {
        return null;
    }

    @Override
    public Set<String> validate() {
        return null;
    }

    @Override
    public Query orderById(boolean orderById) {
        return null;
    }

    @Override
    public Object findSingleAttribute() {
        return null;
    }

    @Override
    public List findSingleAttributeList() {
        return null;
    }

    @Override
    public Map findMap() {
        return null;
    }

    @Override
    public void findEach(int batch, Consumer consumer) {

    }

    @Override
    public List findIds() {
        return null;
    }

    @Override
    public DtoQuery asDto(Class dtoClass) {
        return null;
    }

    @Override
    public void cancel() {

    }
}
