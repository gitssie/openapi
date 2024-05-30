package com.gitssie.openapi.ebean.repository;

import io.ebean.*;
import io.vavr.Function2;
import io.vavr.Tuple2;
import org.springframework.data.domain.*;
import org.springframework.data.jdbc.repository.query.JdbcQueryMethod;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public class EbeanRepositoryQuery implements RepositoryQuery {
    private Database database;
    private JdbcQueryMethod queryMethod;
    private Parameters<?, ?> parameters;
    private Function<Object[], Tuple2<String, Object[]>> handler;

    private RowMapper<Object> rowMapper;


    public EbeanRepositoryQuery(Database database, JdbcQueryMethod queryMethod, Parameters<?, ?> parameters,
                                Function<Object[], Tuple2<String, Object[]>> handler,
                                RowMapper<Object> rowMapper) {
        this.database = database;
        this.queryMethod = queryMethod;
        this.parameters = parameters;
        this.handler = handler;
        this.rowMapper = rowMapper;

    }

    @Override
    public Object execute(Object[] values) {
        Map<String, Object> map = new HashMap<>();
        Object pageable = null;
        Object sort = null;
        for (Parameter parameter : parameters) {
            if (parameter.getName().isPresent()) {
                map.put(parameter.getName().get(), values[parameter.getIndex()]);
            }
        }
        //如果有分页的参数
        if (parameters.getPageableIndex() >= 0) {
            pageable = values[parameters.getPageableIndex()];
        }
        //排序的函数
        if (parameters.getSortIndex() >= 0) {
            sort = values[parameters.getSortIndex()];
        }
        Object[] codeArgs = new Object[]{map, pageable, sort};
        Tuple2<String, Object[]> sql = toSQL(codeArgs);
        return executeSQL(sql, codeArgs);
    }

    private Tuple2<String, Object[]> toSQL(Object[] codeArgs) {
        return handler.apply(codeArgs);
    }

    private Object executeSQL(Tuple2<String, Object[]> sql, Object[] codeArgs) {
        Function2<String, Object[], Object> queryExecution = getQueryExecution(//
                queryMethod, //
                codeArgs); //determineResultSetExtractor(rowMapper)

        return queryExecution.apply(sql._1, sql._2);
    }

    protected Function2<String, Object[], Object> getQueryExecution(JdbcQueryMethod queryMethod, Object[] codeArgs) {

        if (queryMethod.isModifyingQuery()) {
            return createModifyingQueryExecutor();
        }

        if (queryMethod.isCollectionQuery()) {
            //return extractor != null ? getQueryExecution(extractor) : collectionQuery(rowMapper);
            return (Function2) collectionQuery(rowMapper);
        }

        if (queryMethod.isStreamQuery()) {
            //return extractor != null ? getQueryExecution(extractor) : streamQuery(rowMapper);
            return (Function2) streamQuery(rowMapper);
        }

        if (queryMethod.isPageQuery()) {
            return (Function2) pageQuery(rowMapper, codeArgs);
        }

        if (queryMethod.isSliceQuery()) {
            return (Function2) sliceQuery(rowMapper, codeArgs);
        }

//        return extractor != null ? getQueryExecution(extractor) : singleObjectQuery(rowMapper);
        return singleObjectQuery(rowMapper);
    }

    private Function2<String, Object[], Object> createModifyingQueryExecutor() {
        return (sql, bindArgs) -> {
            SqlUpdate update = database.sqlUpdate(sql).setParameters(bindArgs);
            int updatedCount = update.execute();
            Class<?> returnedObjectType = queryMethod.getReturnedObjectType();
            return (returnedObjectType == boolean.class || returnedObjectType == Boolean.class) ? updatedCount != 0
                    : updatedCount;
        };
    }

    private <T> Function2<String, Object[], List<T>> collectionQuery(RowMapper<T> rowMapper) {
        return (sql, bindArgs) -> {
            if (rowMapper instanceof SQLRowMapper.NativeEntityMapper) {
                SQLRowMapper.NativeEntityMapper ep = (SQLRowMapper.NativeEntityMapper) rowMapper;
                return database.findNative(ep.getReturnedObjectType(), sql)
                        .setParameters(bindArgs)
                        .findList();
            } else {
                return database.sqlQuery(sql)
                        .setParameters(bindArgs)
                        .mapTo(rowMapper)
                        .findList();
            }
        };
    }

    private <T> Function2<String, Object[], Stream<T>> streamQuery(RowMapper<T> rowMapper) {
        return (sql, bindArgs) -> {
            if (rowMapper instanceof SQLRowMapper.NativeEntityMapper) {
                SQLRowMapper.NativeEntityMapper ep = (SQLRowMapper.NativeEntityMapper) rowMapper;
                return database.findNative(ep.getReturnedObjectType(), sql)
                        .setParameters(bindArgs)
                        .findStream();
            } else {
                return database.sqlQuery(sql)
                        .setParameters(bindArgs)
                        .mapTo(rowMapper)
                        .findList().stream();
            }
        };
    }

    private <T> Function2<String, Object[], Page<T>> pageQuery(RowMapper<T> rowMapper, Object[] codeArgs) {
        return (sql, bindArgs) -> {
            List<T> result;
            if (rowMapper instanceof SQLRowMapper.NativeEntityMapper) {
                SQLRowMapper.NativeEntityMapper ep = (SQLRowMapper.NativeEntityMapper) rowMapper;
                result = database.findNative(ep.getReturnedObjectType(), sql)
                        .setParameters(bindArgs)
                        .findList();
            } else {
                result = database.sqlQuery(sql)
                        .setParameters(bindArgs)
                        .mapTo(rowMapper)
                        .findList();
            }
            if (codeArgs.length > 1 && codeArgs[1] instanceof Pageable) {
                Tuple2<String, Object[]> countSql = toSQL(new Object[]{codeArgs[0]});
                long total = database.sqlQuery(countSql._1)
                        .setParameters(countSql._2)
                        .mapTo((rs, i) -> rs.getLong(1))
                        .findOne();
                return new PageImpl<>(result, (Pageable) codeArgs[1], total);
            }
            return new PageImpl<>(result);
        };
    }

    private <T> Function2<String, Object[], Slice<T>> sliceQuery(RowMapper<T> rowMapper, Object[] codeArgs) {
        return (sql, bindArgs) -> {
            List<T> result;
            if (rowMapper instanceof SQLRowMapper.NativeEntityMapper) {
                SQLRowMapper.NativeEntityMapper ep = (SQLRowMapper.NativeEntityMapper) rowMapper;
                result = database.findNative(ep.getReturnedObjectType(), sql)
                        .setParameters(bindArgs)
                        .findList();
            } else {
                result = database.sqlQuery(sql)
                        .setParameters(bindArgs)
                        .mapTo(rowMapper)
                        .findList();
            }
            if (codeArgs.length > 1 && codeArgs[1] instanceof Pageable) {
                Pageable page = (Pageable) codeArgs[1];
                boolean hasNext = result.size() >= page.getPageSize();
                return new SliceImpl<>(result, page, hasNext);

            }
            return new SliceImpl<>(result);
        };
    }

    private <T> Function2<String, Object[], T> singleObjectQuery(RowMapper<T> rowMapper) {
        return (sql, bindArgs) -> {
            SqlQuery.TypeQuery<T> query = database.sqlQuery(sql)
                    .setParameters(bindArgs)
                    .mapTo(rowMapper);

            return query.findOne();
        };
    }

    @Override
    public QueryMethod getQueryMethod() {
        return queryMethod;
    }
}
