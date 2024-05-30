package com.gitssie.openapi.ebean.repository;

import io.ebean.Database;
import io.ebean.bean.EntityBean;
import io.vavr.Tuple2;
import io.vavr.control.Option;
import org.springframework.beans.BeanUtils;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.jdbc.repository.query.JdbcQueryMethod;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.*;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class EbeanJdbcQueryLookupStrategy implements QueryLookupStrategy {
    private EbeanJsEngine jsEngine;
    private Database database;
    private SQLRowMapper rowMapper;

    public EbeanJdbcQueryLookupStrategy(EbeanJsEngine jsEngine, Database database, JdbcConverter converter) {
        this.jsEngine = jsEngine;
        this.database = database;
        this.rowMapper = new SQLRowMapper(converter);
    }

    @Override
    public RepositoryQuery resolveQuery(Method method, RepositoryMetadata meta, ProjectionFactory factory, NamedQueries namedQueries) {
        try {
            JdbcQueryMethod queryMethod = new JdbcQueryMethod(method, meta, factory, namedQueries, null);

            Option<Function<Object[], Tuple2<String, Object[]>>> handler = jsEngine.getHandler(meta.getRepositoryInterface(), queryMethod.getName());
            if (handler.isEmpty()) {
                throw QueryCreationException.create(String.format("can't find function %s in JavaScript code.", queryMethod.getName()), null, EbeanJdbcQueryLookupStrategy.class, method);
            }
            Class<?> returnedObjectType = resolveTypeToRead(queryMethod.getResultProcessor());
            return new EbeanRepositoryQuery(database,
                    queryMethod,
                    createParameters(method),
                    handler.get(),
                    rowMapper.createRowMapper(returnedObjectType,true));
        } catch (Exception e) {
            throw QueryCreationException.create(method.getName(), e, EbeanJdbcQueryLookupStrategy.class, method);
        }
    }

    protected Class<?> resolveTypeToRead(ResultProcessor resultProcessor) {

        ReturnedType returnedType = resultProcessor.getReturnedType();

        if (returnedType.getReturnedType().isAssignableFrom(returnedType.getDomainType())) {
            return returnedType.getDomainType();
        }
        // Slight deviation from R2DBC: Allow direct mapping into DTOs
        return returnedType.isProjecting() ? returnedType.getReturnedType() : returnedType.getDomainType();
    }

    protected Parameters<?, ?> createParameters(Method method) {
        return new DefaultParameters(method);
    }
}
