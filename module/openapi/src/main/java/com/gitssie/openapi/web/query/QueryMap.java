package com.gitssie.openapi.web.query;

import com.gitssie.openapi.ebean.SimpleProperty;
import io.ebean.plugin.Property;
import org.apache.commons.lang3.ObjectUtils;

import javax.validation.Valid;
import java.util.*;
import java.util.function.Function;

public class QueryMap extends AbstractQuery {
    @Valid
    private List<ColumnField> columns;
    @Valid
    private Map<String, Object> query;

    public QueryMap() {
    }

    public QueryMap(Map<String, Object> query) {
        this.query = query;
        if (ObjectUtils.isNotEmpty(query)) {
            for (Map.Entry<String, Object> entry : query.entrySet()) {
                addPredicate(new PredicateField(entry.getKey(), null, entry.getValue()));
            }
        }
    }

    public Map<String, Object> getQuery() {
        if (query == null) {
            query = new HashMap<>();
        }
        return query;
    }

    public void setQuery(LinkedHashMap<String, Object> query) {
        this.query = query;
    }

    public List<ColumnField> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnField> columns) {
        this.columns = columns;
    }

    public <T> QueryMap addPredicate(String field, String op, Function<Object, T> cast) {
        return addPredicate(field, op, cast, Optional.empty());
    }

    public <T> QueryMap addPredicateIf(String field, String op, Function<Object, T> cast) {
        return addPredicate(field, op, cast, (Optional<T>) null);
    }

    public <T> QueryMap addPredicate(String field, String op, Function<Object, T> cast, T defaultValue) {
        return addPredicate(field, op, cast, Optional.of(defaultValue));
    }

    public <T> QueryMap addPredicate(String field, String op, Function<Object, T> cast, Optional<T> defaultValue) {
        T param = cast.apply(getQuery().get(field));
        if (param == null && defaultValue == null) {
            return this;
        }
        param = param == null ? defaultValue.get() : param;
        addPredicate(new PredicateField(field, op, param));
        return this;
    }

    public <T> QueryMap addValue(String field, String op, Function<Object, T> cast, Object value) {
        addPredicate(new PredicateField(field, op, cast.apply(value)));
        return this;
    }

    public <T> QueryMap addValue(String field, String op, Object... value) {
        addPredicate(new PredicateField(field, op, value));
        return this;
    }

    public <T> QueryMap addValueIf(String field, String op, Object value) {
        if (value != null) {
            return addValue(field, op, value);
        }
        return this;
    }

    public Map<String, Property> toDesc() {
        if (queryPredicate == null || ObjectUtils.isEmpty(queryPredicate.getPredicate())) {
            return Collections.EMPTY_MAP;
        }
        Map<String, Property> map = new HashMap<>();
        Class<?> clazz = String.class;
        for (PredicateField field : queryPredicate.getPredicate()) {
            for (Object o : field.getValue()) {
                if (o != null) {
                    clazz = o.getClass();
                    break;
                }
            }
            map.put(field.getPath(), new SimpleProperty(field.getPath(), clazz));
        }
        return map;
    }

    public void setSizeMid() {
        setSize(SIZE_MID);
    }

    public Object get(String key) {
        return getQuery().get(key);
    }
}
