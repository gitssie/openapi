package com.gitssie.openapi.web.query;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public class QueryPredicate {
    @Valid
    private List<PredicateField> predicate;
    private String expression;

    public QueryPredicate() {
    }

    public QueryPredicate(List<PredicateField> predicate) {
        this.predicate = predicate;
    }

    public List<PredicateField> getPredicate() {
        return predicate == null ? Collections.EMPTY_LIST : predicate;
    }

    public void setPredicate(List<PredicateField> predicate) {
        this.predicate = predicate;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public QueryPredicate add(PredicateField item) {
        if (predicate == null) {
            predicate = new LinkedList<>();
        }
        predicate.add(item);
        return this;
    }

    public QueryPredicate add(@NotNull String field, String op, Object value) {
        add(new PredicateField(field, op, value));
        return this;
    }

    public <T> QueryPredicate addIf(@NotNull String field, String op, T value, Function<T, Boolean> condition) {
        if (condition.apply(value)) {
            add(new PredicateField(field, op, value));
        }
        return this;
    }

    public boolean hasPredicate(String field) {
        if (predicate == null) {
            return false;
        }
        for (PredicateField p : predicate) {
            if (field.equals(p.getField())) {
                return true;
            }
        }
        return false;
    }
}
