package com.gitssie.openapi.ebean.repository;

import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.*;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class SQLParameterFunction extends BaseFunction {
    public static final String PARAMETER_FUNC_NAME = "setParameter";
    private final List<Object> args = new ArrayList<>();
    private final StringBuilder buf = new StringBuilder();

    @Override
    public Object call(Context ctx, Scriptable scope, Scriptable thisObj, Object[] args) {
        this.buf.setLength(0);
        if (args.length > 0) {
            Object ag = args[0];
            if (ag instanceof Undefined || ag == null) {
                bindNull();
            } else if (ag instanceof Pageable) {
                bindLimit((Pageable) ag);
            } else if (ag.getClass().isArray()) {
                bindArray((Object[]) ag);
            } else if (ag instanceof Collection) {
                bindCollection((Collection) ag);
            } else {
                bindOne(ag);
            }
        }
        return this.buf.toString();
    }

    private void bindNull() {
        bindOne(null);
    }

    private void bindOne(Object value) {
        this.args.add(unwrap(value));
        this.buf.append("?");
    }

    private Object unwrap(Object value) {
        if (value instanceof Wrapper) {
            return ((Wrapper) value).unwrap();
        } else if (value instanceof Scriptable) {
            Scriptable obj = (Scriptable) value;
            String type = obj.getClassName();
            if (StringUtils.equals("Date", type)) {
                return Context.jsToJava(obj, Date.class);
            }
            return obj;
        } else {
            return value;
        }
    }

    private void bindArray(Object[] arr) {
        int i = 0;
        for (Object item : arr) {
            this.args.add(unwrap(item));
            buf.append("?,");
            i++;
        }
        if (i > 0) {
            buf.setLength(buf.length() - 1);
        }
    }

    private void bindCollection(Collection arr) {
        int i = 0;
        for (Object item : arr) {
            this.args.add(unwrap(item));
            buf.append("?,");
            i++;
        }
        if (i > 0) {
            buf.setLength(buf.length() - 1);
        }
    }

    private void bindLimit(Pageable page) {
        if (page.isPaged()) {
            this.buf.append("limit ").append(page.getOffset()).append(",").append(page.getPageSize());
        }
    }

    public List<Object> getArgs() {
        return args;
    }

    public String getParameterFuncName() {
        return PARAMETER_FUNC_NAME;
    }
}
