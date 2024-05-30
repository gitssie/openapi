package com.gitssie.openapi.web.query;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryForm extends AbstractQuery {
    @Valid
    private List<ColumnField> columns;
    @Valid
    private QueryPredicate query;
    private Map<String, Object> map;

    public QueryPredicate getQuery() {
        return query;
    }

    public void setQuery(QueryPredicate query) {
        this.query = query;
        this.queryPredicate = query;
    }

    public boolean hasPredicate(String field) {
        if (query == null) {
            return false;
        }
        return query.hasPredicate(field);
    }

    public List<ColumnField> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnField> columns) {
        this.columns = columns;
    }

    @JsonAnySetter
    void setOthers(String key, Object value) {
        if (map == null) {
            map = new HashMap<>();
        }
        map.put(key, value);
    }

    @JsonAnyGetter
    @JsonIgnore
    Map<String, Object> getOthers() {
        return map;
    }

    public Object get(String key) {
        if (map == null) {
            return null;
        } else {
            return map.get(key);
        }
    }

    public void clearMap() {
        if (map != null) {
            map.clear();
        }
    }
}
