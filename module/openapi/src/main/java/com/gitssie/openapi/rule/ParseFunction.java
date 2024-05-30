package com.gitssie.openapi.rule;

import com.alibaba.fastjson.util.TypeUtils;
import com.gitssie.openapi.rule.Rules;
import org.apache.commons.lang3.ObjectUtils;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.UniqueTag;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

public class ParseFunction extends BaseFunction {

    @Override
    public Object call(Context ctx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (args.length == 0) {
            return UniqueTag.NULL_VALUE;
        }
        String type = (String) Context.jsToJava(args[0], String.class);
        type = type.toLowerCase();

        Object value = null;
        if (args.length > 1) {
            value = args[1];
        }
        Object result = null;
        switch (type) {
            case "int":
                result = TypeUtils.castToInt(value);
                break;
            case "long":
                result = TypeUtils.castToLong(value);
                break;
            case "float":
                result = castToFloat(value,args);
                break;
            case "double":
                result = castToDouble(value,args);
                break;
            case "decimal":
            case "bigdecimal":
                result = castToBigDecimal(value, args);
                break;
            case "date":
                result = castToDate(value);
                break;
            case "toseconds":
            case "seconds":
                result = castToSeconds(value);
                break;
        }
        if (args.length > 2) {
            return ObjectUtils.defaultIfNull(result, args[2]);
        }
        return result;
    }

    /**
     * parse('float',value,2)
     *
     * @param value
     * @param args
     * @return
     */
    public Float castToFloat(Object value, Object[] args) {
        if (args.length > 2) {
            BigDecimal res = castToBigDecimal(value, args);
            return res == null ? null : res.floatValue();
        } else {
            return TypeUtils.castToFloat(value);
        }
    }

    /**
     * parse('double',value,2)
     *
     * @param value
     * @param args
     * @return
     */
    public Double castToDouble(Object value, Object[] args) {
        if (args.length > 2) {
            BigDecimal res = castToBigDecimal(value, args);
            return res == null ? null : res.doubleValue();
        } else {
            return TypeUtils.castToDouble(value);
        }
    }

    /**
     * parse('decimal',value,2)
     *
     * @param value
     * @param args
     * @return
     */
    public BigDecimal castToBigDecimal(Object value, Object[] args) {
        BigDecimal res = TypeUtils.castToBigDecimal(value);
        if (res != null && args.length > 2) {
            int scale = 2;
            scale = ObjectUtils.defaultIfNull(TypeUtils.castToInt(args[2]), scale);
            int roundingMode = RoundingMode.DOWN.ordinal();
            if (args.length > 3) {
                roundingMode = ObjectUtils.defaultIfNull(TypeUtils.castToInt(args[3]), roundingMode);
            }
            //截取值
            res = res.setScale(scale, roundingMode);
        }
        return res;
    }

    public Date castToDate(Object value) {
        return Rules.castToDate(value);
    }

    public long castToSeconds(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Date) {
            return ((Date) value).getTime() / 1000;
        }
        Date date = castToDate(value);
        if (date != null) {
            return date.getTime() / 1000;
        }
        return 0;
    }
}
