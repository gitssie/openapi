package com.gitssie.openapi.ebean.repository;

import com.gitssie.openapi.page.FetchContext;
import io.ebean.bean.EntityBean;
import io.vavr.control.Option;
import org.springframework.beans.BeanUtils;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.stereotype.Service;

import java.lang.reflect.Constructor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;


@Service
public class SQLRowMapper {
    private JdbcConverter converter;

    public SQLRowMapper(JdbcConverter converter) {
        this.converter = converter;
    }

    public <T> io.ebean.RowMapper<T> createRowMapper(Class<?> returnedObjectType, boolean camelCase) {
        //entity
        if (EntityBean.class.isAssignableFrom(returnedObjectType)) {
            return new NativeEntityMapper<>(returnedObjectType);
        }
        //map
        if (Map.class.isAssignableFrom(returnedObjectType)) {
            return (io.ebean.RowMapper) new RawSqlMapper<>(new ColumnMapRowMapper(camelCase));
        }
        //java type
        if (BeanUtils.isSimpleValueType(returnedObjectType)) {
            return new RawSqlMapper(SingleColumnRowMapper.newInstance(returnedObjectType, converter.getConversionService()));
        }
        //java bean
        Optional<Constructor<?>> constructor = ReflectionUtils.findConstructor(returnedObjectType, new Object[0]);
        if (constructor.isPresent()) {
            return (io.ebean.RowMapper) new RawSqlMapper<>(new DataClassRowMapper(returnedObjectType));
        }
        //single column
        RowMapper<Object> rowMapper = (RowMapper) SingleColumnRowMapper.newInstance(returnedObjectType, converter.getConversionService());
        return new RawSqlMapper(rowMapper);
    }

    static class NativeEntityMapper<T> implements io.ebean.RowMapper<T> {
        private Class<?> returnedObjectType;

        public NativeEntityMapper(Class<?> returnedObjectType) {
            this.returnedObjectType = returnedObjectType;
        }

        @Override
        public T map(ResultSet resultSet, int rowNum) throws SQLException {
            throw new UnsupportedOperationException();
        }

        public Class<?> getReturnedObjectType() {
            return returnedObjectType;
        }
    }

    static class RawSqlMapper<T> implements io.ebean.RowMapper<T> {
        private RowMapper<T> rowMapper;

        public RawSqlMapper(RowMapper<T> rowMapper) {
            this.rowMapper = rowMapper;
        }

        @Override
        public T map(ResultSet resultSet, int rowNum) throws SQLException {
            return rowMapper.mapRow(resultSet, rowNum);
        }
    }
}
