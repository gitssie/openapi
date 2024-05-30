package com.gitssie.openapi.web.query;

import io.ebean.Expression;
import io.ebean.annotation.JsonIgnore;
import io.vavr.control.Option;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class PredicateField {
    protected String field;
    protected String name;
    protected String label;
    protected String op;
    protected Object value;
    protected String path;
    @JsonIgnore
    private Option<Expression> expr = Option.none();
    @JsonIgnore
    private Object bindValue;

    public PredicateField() {
    }

    public PredicateField(@NotNull String field, String label, String op, Object value) {
        this.field = field;
        this.label = label;
        this.op = op;
        this.value = value;
    }

    public PredicateField(@NotNull String field, Object value) {
        this(field, null, null, value);
    }

    public PredicateField(@NotNull String field, String op, Object value) {
        this(field, null, op, value);
    }

    public String getField() {
        return StringUtils.defaultString(field, name);
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getOp() {
        return StringUtils.defaultString(op, "eq");
    }

    public void setOp(String op) {
        this.op = op;
    }

    /**
     * 转化为SQL绑定的值,如何是数组就表示多个,主要是方便处理
     *
     * @return
     */
    public Object getBindValue() {
        if (value == null) {
            return null;
        }
        if (bindValue != null) {
            return bindValue;
        } else if (value instanceof Collection<?>) {
            bindValue = ((Collection<?>) value).toArray();
        } else if (value instanceof String) {
            bindValue = StringUtils.trim((String) value);
        } else {
            bindValue = value;
        }
        return bindValue;
    }

    /**
     * 把参数值,转化为数组,方便进行统一处理
     *
     * @return
     */
    public Object[] getValue() {
        Object value = getBindValue();
        if (value == null) {
            return new Object[0];
        }
        if (value.getClass().isArray()) {
            return (Object[]) value;
        }
        return new Object[]{value};
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Option<Expression> getExpr() {
        return expr;
    }

    public void setExpr(Option<Expression> expr) {
        this.expr = expr;
    }

    public String getPath() {
        if (StringUtils.isNotEmpty(path)) {
            return path;
        }
        return getField();
    }

    public void setPath(String path) {
        this.path = path;
    }
}
