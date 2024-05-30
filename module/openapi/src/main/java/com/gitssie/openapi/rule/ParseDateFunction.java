package com.gitssie.openapi.rule;

import com.alibaba.fastjson.util.TypeUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.UniqueTag;

import java.util.Date;

public class ParseDateFunction extends BaseFunction {

    @Override
    public Object call(Context ctx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (args.length == 0) {
            return UniqueTag.NULL_VALUE;
        }
        return castToDate(args[0]);
    }

    public Date castToDate(Object value) {
        if (value instanceof Scriptable) {
            String type = ((Scriptable) value).getClassName();
            if (StringUtils.equals(type, "Date")) {
                return (Date) Context.jsToJava(value, Date.class);
            }
        }
        return TypeUtils.castToDate(value);
    }
}
