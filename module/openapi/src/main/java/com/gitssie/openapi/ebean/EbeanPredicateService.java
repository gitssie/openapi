package com.gitssie.openapi.ebean;

import com.gitssie.openapi.data.Code;
import com.gitssie.openapi.service.Provider;
import com.gitssie.openapi.utils.TypeUtils;
import com.gitssie.openapi.web.query.AbstractQuery;
import com.gitssie.openapi.web.query.PredicateField;
import com.gitssie.openapi.web.query.QueryMap;
import com.gitssie.openapi.web.query.QueryPredicate;
import com.google.common.collect.Maps;
import io.ebean.Expression;
import io.ebean.ExpressionFactory;
import io.ebean.bean.EntityBean;
import io.ebean.core.type.ScalarType;
import io.ebean.plugin.BeanType;
import io.ebean.plugin.Property;
import io.ebeaninternal.server.deploy.BeanProperty;
import io.ebeaninternal.server.deploy.BeanPropertyAssoc;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class EbeanPredicateService {
    private final Map<String, BiFunction<Property, Object[], Either<String, Expression>>> OP_NUMBER = Maps.newHashMap();
    private final Map<String, BiFunction<Property, Object[], Either<String, Expression>>> OP_STRING = Maps.newHashMap();
    private final Map<String, BiFunction<Property, Object[], Either<String, Expression>>> OP_BOOLEAN = Maps.newHashMap();
    private final Map<String, BiFunction<Property, Object[], Either<String, Expression>>> OP_DATE = Maps.newHashMap();
    private final Map<String, BiFunction<Property, Object[], Either<String, Expression>>> OP_JSON = Maps.newHashMap();
    private final Pattern aggrePattern = Pattern.compile("([\\w\\d\\_]+\\.)+");
    private final ConversionService conversionService;
    private ExpressionFactory factory;
    private final Provider provider;

    public EbeanPredicateService(FormattingConversionService conversionService, Provider provider) {
        this.conversionService = conversionService;
        this.provider = provider;
        if (this.provider != null) {
            this.factory = provider.expr();
        }
        this.initPredicate();
    }

    public void initPredicate() {
        BiFunction<Property, Object[], Either<String, Expression>> equals = (p, v) -> required(v, e -> {
            if (v.length > 1) {
                return factory.in(p.name(), v);
            } else {
                return factory.eq(p.name(), v[0]);
            }
        });

        OP_NUMBER.put("isNull", (p, v) -> Either.right(factory.isNull(p.name())));
        OP_NUMBER.put("isNotNull", (p, v) -> Either.right(factory.isNotNull(p.name())));
        OP_NUMBER.put("eq", equals);
        OP_NUMBER.put("in", (p, v) -> required(v, e -> factory.in(p.name(), v)));
        OP_NUMBER.put("notIn", (p, v) -> required(v, e -> factory.not(factory.in(p.name(), v))));
        OP_NUMBER.put("gt", (p, v) -> required(v, e -> factory.gt(p.name(), e)));
        OP_NUMBER.put("ge", (p, v) -> required(v, e -> factory.ge(p.name(), e)));
        OP_NUMBER.put("lt", (p, v) -> required(v, e -> factory.lt(p.name(), e)));
        OP_NUMBER.put("le", (p, v) -> required(v, e -> factory.le(p.name(), e)));
        OP_NUMBER.put("between", (p, v) -> between(v, (e1, e2) -> factory.between(p.name(), e1, e2)));
        OP_NUMBER.put("inRange", (p, v) -> between(v, (e1, e2) -> factory.inRange(p.name(), e1, e2)));


        OP_STRING.put("isNull", (p, v) -> Either.right(factory.isNull(p.name())));
        OP_STRING.put("isNotNull", (p, v) -> Either.right(factory.isNotNull(p.name())));
        OP_STRING.put("eq", equals);
        OP_STRING.put("in", (p, v) -> required(v, e -> factory.in(p.name(), v)));
        OP_STRING.put("notIn", (p, v) -> required(v, e -> factory.not(factory.in(p.name(), v))));
        OP_STRING.put("gt", (p, v) -> required(v, e -> factory.gt(p.name(), e)));
        OP_STRING.put("ge", (p, v) -> required(v, e -> factory.ge(p.name(), e)));
        OP_STRING.put("lt", (p, v) -> required(v, e -> factory.lt(p.name(), e)));
        OP_STRING.put("le", (p, v) -> required(v, e -> factory.le(p.name(), e)));
        OP_STRING.put("like", (p, v) -> string(v, e -> factory.like(p.name(), e)));
        OP_STRING.put("contains", (p, v) -> string(v, e -> factory.contains(p.name(), e)));
        OP_STRING.put("startsWith", (p, v) -> string(v, e -> factory.startsWith(p.name(), e)));
        OP_STRING.put("between", (p, v) -> between(v, (e1, e2) -> factory.between(p.name(), e1, e2)));
        OP_STRING.put("inRange", (p, v) -> between(v, (e1, e2) -> factory.inRange(p.name(), e1, e2)));


        OP_BOOLEAN.put("isNull", (p, v) -> Either.right(factory.isNull(p.name())));
        OP_BOOLEAN.put("isNotNull", (p, v) -> Either.right(factory.isNotNull(p.name())));
        OP_BOOLEAN.put("eq", equals);
        OP_BOOLEAN.put("in", (p, v) -> required(v, e -> factory.in(p.name(), v)));
        OP_BOOLEAN.put("notIn", (p, v) -> required(v, e -> factory.not(factory.in(p.name(), v))));


        OP_DATE.put("isNull", (p, v) -> Either.right(factory.isNull(p.name())));
        OP_DATE.put("isNotNull", (p, v) -> Either.right(factory.isNotNull(p.name())));
        OP_DATE.put("eq", equals);
        OP_DATE.put("in", (p, v) -> required(v, e -> factory.in(p.name(), v)));
        OP_DATE.put("notIn", (p, v) -> required(v, e -> factory.not(factory.in(p.name(), v))));
        OP_DATE.put("gt", (p, v) -> required(v, e -> factory.gt(p.name(), e)));
        OP_DATE.put("ge", (p, v) -> required(v, e -> factory.ge(p.name(), e)));
        OP_DATE.put("lt", (p, v) -> required(v, e -> factory.lt(p.name(), e)));
        OP_DATE.put("le", (p, v) -> required(v, e -> factory.le(p.name(), e)));
        OP_DATE.put("between", (p, v) -> between(v, (e1, e2) -> factory.between(p.name(), e1, e2)));
        OP_DATE.put("inRange", (p, v) -> between(v, (e1, e2) -> factory.inRange(p.name(), e1, e2)));
        OP_DATE.put("date(0)", dateRange(0));
        OP_DATE.put("date(1)", dateRange(1));
        OP_DATE.put("date(-1)", dateRange(-1));
        OP_DATE.put("week(0)", weekRange(0));
        OP_DATE.put("week(1)", weekRange(1));
        OP_DATE.put("week(-1)", weekRange(-1));
        OP_DATE.put("month(0)", monthRange(0));
        OP_DATE.put("month(1)", monthRange(1));
        OP_DATE.put("month(-1)", monthRange(-1));
        OP_DATE.put("month(-2)", monthRange(-2));
        OP_DATE.put("month(-3)", monthRange(-3));
        OP_DATE.put("month(-4)", monthRange(-4));

        OP_JSON.put("isNull", (p, v) -> Either.right(factory.isNull(p.name())));
        OP_JSON.put("isNotNull", (p, v) -> Either.right(factory.isNotNull(p.name())));
        OP_JSON.put("eq", equals);
        OP_JSON.put("contains", (p, v) -> required(v, e -> factory.raw("JSON_CONTAINS(" + p.name() + ",?,'$')", e.toString())));
    }

    public BiFunction<Property, Object[], Either<String, Expression>> dateRange(int d) {
        return dateRange(() -> DateUtils.truncate(new Date(), Calendar.DATE), 1, d);
    }

    public BiFunction<Property, Object[], Either<String, Expression>> weekRange(int d) {
        return dateRange(() -> {
            Calendar calendar = Calendar.getInstance();// 获取当前日期
            calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek()); // 获取本周的日期范围
            Date e1 = DateUtils.addDays(DateUtils.truncate(calendar.getTime(), Calendar.DATE), 1);
            return e1;
        }, 7, d);
    }

    public BiFunction<Property, Object[], Either<String, Expression>> dateRange(Supplier<Date> firstDate, int step, int d) {
        return (p, v) -> {
            Date e1 = firstDate.get();
            Date e2 = DateUtils.addDays(e1, step); //明天
            switch (d) {
                case 0: { //今天
                    break;
                }
                case 1: { //明天
                    e1 = DateUtils.addDays(e1, step);
                    e2 = DateUtils.addDays(e2, step);
                    break;
                }
                case -1: { //昨天
                    e1 = DateUtils.addDays(e1, -step);
                    e2 = DateUtils.addDays(e2, -step);
                    break;
                }
                default: {
                    return Either.left("日期表达式的值不能为空");
                }
            }
            return Either.right(factory.inRange(p.name(), e1, e2));
        };
    }

    public BiFunction<Property, Object[], Either<String, Expression>> monthRange(int d) {
        return (p, v) -> {
            Calendar calendar = Calendar.getInstance();// 获取当前日期
            calendar.set(Calendar.DAY_OF_MONTH, 1); // 获取本月的日期范围
            Date e1 = DateUtils.truncate(calendar.getTime(), Calendar.DATE);
            Date e2 = DateUtils.addMonths(e1, 1);
            switch (d) {
                case 0: { //本月
                    break;
                }
                case 1: { //下月
                    e1 = DateUtils.addMonths(e1, d);
                    e2 = DateUtils.addMonths(e2, d);
                    break;
                }
                default: {
                    if (d < 0 && d > -10) {
                        e1 = DateUtils.addMonths(e1, d);
                        break;
                    }
                    return Either.left("日期表达式的值不能为空");
                }
            }
            return Either.right(factory.inRange(p.name(), e1, e2));
        };
    }

    private <T> Either<String, Expression> required(T[] val, Function<T, Expression> call) {
        if (ObjectUtils.isEmpty(val)) {
            return Either.left("表达式的值不能为空");
        } else {
            return Either.right(call.apply(val[0]));
        }
    }

    private Either<String, Expression> string(Object[] arr, Function<String, Expression> call) {
        if (ObjectUtils.isEmpty(arr)) {
            return Either.left("表达式的值不能为空");
        }
        //空字符串匹配
        Object val = arr[0];
        if (val != null) {
            String valRaw = val.toString();
            if (StringUtils.isBlank(valRaw)) {
                return Either.right(null); //字符串为 ""则不进行查询
            }
            return Either.right(call.apply(valRaw));
        }
        return Either.right(null);
    }

    private <T> Either<String, Expression> between(T[] val, BiFunction<T, T, Expression> call) {
        if (ObjectUtils.isEmpty(val)) {
            return Either.left("表达式的值不能为空");
        } else if (val.length != 2) {
            return Either.left("表达式的值必须为两个");
        } else {
            return Either.right(call.apply(val[0], val[1]));
        }
    }

    private Either<String, Expression> applyPath(Property path, ScalarType<Object> scalarType, String op, Object[] val, Object[] value, Function<String, BiFunction<Property, Object[], Either<String, Expression>>> map) {
        BiFunction<Property, Object[], Either<String, Expression>> func = map.apply(op);
        if (func == null) {
            return Either.left(String.format("%s[%s]不支持的表达式:%s", path.getClass().getSimpleName(), path.name(), op));
        }
        for (int i = 0; i < value.length; i++) {
            value[i] = cast(scalarType, path.type(), val[i]);
        }
        return func.apply(path, value);
    }

    public Either<String, List<Expression>> parsePredicate(BeanType desc, List<PredicateField> fields) {
        return parsePredicate(new BeanTypeProperty(desc), fields);
    }

    public Either<String, Expression> parsePredicate(BeanType desc, QueryPredicate queryMap) {
        return parsePredicate(new BeanTypeProperty(desc), queryMap.getPredicate())
                .flatMap(e -> this.parseExpression(queryMap.getExpression(), e));
    }

    public Either<String, List<Expression>> parsePredicate(Map<String, Property> desc, List<PredicateField> fields) {
        return parsePredicate(desc::get, fields);
    }

    public Either<String, Expression> parsePredicate(BeanType desc, AbstractQuery queryMap) {
        return parsePredicate(desc, queryMap.getPredicate())
                .flatMap(e -> this.parseExpression(queryMap.getExpression(), e));
    }

    public Either<String, Expression> parsePredicate(QueryMap queryMap) {
        return parsePredicate(queryMap.toDesc(), queryMap.getPredicate())
                .flatMap(e -> this.parseExpression(queryMap.getExpression(), e));
    }

    public Either<String, Expression> parsePredicate(String apiKey, QueryMap queryMap) {
        return parsePredicate(provider.desc(apiKey), queryMap.getPredicate())
                .flatMap(e -> this.parseExpression(queryMap.getExpression(), e));
    }

    public Either<String, Expression> parsePredicate(Class<?> beanClass, QueryMap queryMap) {
        return parsePredicate(provider.desc(beanClass), queryMap.getPredicate())
                .flatMap(e -> this.parseExpression(queryMap.getExpression(), e));
    }

    public Either<String, List<Expression>> parsePredicate(Function<String, Property> desc, List<PredicateField> fields) {
        List<Expression> res = new ArrayList<>();
        for (PredicateField field : fields) {
            if (field.getExpr().isDefined()) {
                res.add(field.getExpr().get());
                continue;
            }
            if (StringUtils.isEmpty(field.getOp())) {
                continue;
            }
            Property path = desc.apply(field.getPath());
            if (path == null) {
                return Either.left(field.getPath() + " 属性不存在");
            }
            Either<String, Expression> expE = parsePath(path, field.getOp(), field.getValue());
            if (expE.isLeft()) {
                return (Either) expE;
            }
            Expression expr = expE.get();
            if (expr != null) {
                res.add(expr);
            }
        }
        return Either.right(res);
    }

    public Either<String, Expression> parsePath(Property path, String op, Object[] val) {
        //普通属性
        ScalarType scalarType = null;
        BeanProperty property = null;
        if (path instanceof BeanProperty) {
            property = (BeanProperty) path;
            scalarType = property.scalarType();
        }
        Object[] value = new Object[val == null ? 0 : val.length];
        if (Number.class.isAssignableFrom(path.type())) {
            return applyPath(path, scalarType, op, val, value, OP_NUMBER::get);
        } else if (String.class.isAssignableFrom(path.type())) {
            return applyPath(path, scalarType, op, val, value, OP_STRING::get);
        } else if (Date.class.isAssignableFrom(path.type())) {
            return applyPath(path, scalarType, op, val, value, OP_DATE::get);
        } else if (Boolean.class.isAssignableFrom(path.type())) {
            return applyPath(path, scalarType, op, val, value, OP_BOOLEAN::get);
        } else if (property != null && property.isJsonSerialize()) {
            return applyPath(path, scalarType, op, val, value, OP_JSON::get);
        } else {
            return applyPath(path, scalarType, op, val, value, OP_STRING::get);
        }
    }

    protected Object cast(ScalarType<Object> type, Class<?> target, Object value) {
        if (value == null) {
            return null;
        } else if (Date.class.isAssignableFrom(target)) { //日期类型 使用Spring的类型转换
            return TypeUtils.castToDate(value);
        } else if (target.isAssignableFrom(value.getClass())) { //子类
            return value;
        } else if (type != null && value instanceof String) {
            return type.parse((String) value);
        } else if (provider != null && provider.isEntityBean(target)) {
            return provider.reference(target, value);
        } else if (type == null || type.isJdbcNative()) {
            //使用Spring的类型转换
            return conversionService.convert(value, target);
        } else {
            return value;
        }
    }

    public Either<String, Expression> parseExpression(List<Expression> predicateList) {
        return parseExpression(null, predicateList);
    }

    public Either<String, Expression> parseExpression(String expr, List<Expression> predicateList) {
        Expression p = null, pL = null;
        if (StringUtils.isEmpty(expr)) {
            for (Expression e : predicateList) {
                if (p == null) {
                    p = e;
                } else {
                    p = factory.and(p, e);
                }
            }
            return Either.right(p);
        }
        expr = expr.toUpperCase();
        Stack<Expression> stack = new Stack();
        int op = 1;
        int j = 0, b = 0, l = 0, len = expr.length();
        for (int i = 0; i < len; i++) {
            char ch = expr.charAt(i);
            if (ch == '(') {
                b++;
                if (p != null) {
                    stack.push(p);
                    p = null;
                }
            } else if (ch == ')') {
                b--;
                if (stack.isEmpty()) {
                    continue;
                }
                pL = stack.pop();
                if ((op & 1) == 1) {
                    p = factory.and(pL, p); //
                } else {
                    p = factory.or(pL, p); //
                }
                op >>= 1;
            } else if (ch == ' ') {
                continue;
            } else if (ch == 'O' && pL != null) {
                j = i + 1;
                if (j < len && expr.charAt(j) == 'R') {
                    op <<= 1;
                } else {
                    return Either.left(String.format("(%s:%s)附近指令格式错误", i, ch));
//                    break;
                }
                i = j;
            } else if (ch == 'A' && pL != null) {
                j = i + 2;
                if (j < len && expr.charAt(j - 1) == 'N' && expr.charAt(j) == 'D') {
                    op <<= 1;
                    op |= 1;
                } else {
                    return Either.left(String.format("(%s:%s)附近指令格式错误", i, ch));
//                    break;
                }
                i = j;
            } else if (ch > '0' && ch <= '9') {//digital
                j = i + 1;
                while (j < len && (expr.charAt(j) >= '0' && expr.charAt(j) <= '9')) j++;
                if (i + 1 == j) {
                    l = ch - '0'; //单数字
                } else {
                    l = Integer.parseInt(expr.substring(i, j)); //两位数字 10、11、
                }
                i = j - 1;
                if (l > predicateList.size()) {
                    return Either.left(String.format("(%s:%s)指令引用超出范围", i, l));
                }
                pL = predicateList.get(l - 1);
                if (p == null) {
                    p = pL;
                } else {
                    if ((op & 1) == 1) {
                        p = factory.and(p, pL); //
                    } else {
                        p = factory.or(p, pL); //
                    }
                    op >>= 1;
                }
            } else {
                return Either.left(String.format("(%s:%s)格式错误", i, ch));
//                break;
            }
        }
        if (b != 0) {
            return Either.left("()格式错误");
        }
        return Either.right(p);
    }


    /**
     * 解析聚合函数
     *
     * @param desc
     * @param aggre
     * @return
     */
    public <T> Either<Code, Tuple2<Optional<Property>, String>> parseAggregationProperties(BeanType<T> desc, List<String> aggre) {
        if (ObjectUtils.isEmpty(aggre)) {
            return Either.left(Code.FAILED_PRECONDITION.withMessage("聚合函数不能为空"));
        }
        String aggreStr = StringUtils.join(aggre, ',');
        return parseAggregationProperties(desc, aggreStr);
    }

    public <T> Either<Code, Tuple2<Optional<Property>, String>> parseAggregationProperties(BeanType<T> desc, String aggreStr) {
        if (ObjectUtils.isEmpty(aggreStr)) {
            return Either.left(Code.FAILED_PRECONDITION.withMessage("聚合函数不能为空"));
        }
        Matcher mt = aggrePattern.matcher(aggreStr);
        String propName = null;
        StringBuilder buf = new StringBuilder();
        int i = 0;
        while (mt.find()) {
            if (propName == null) {
                propName = mt.group(1);
            } else if (!propName.equals(mt.group(1))) {
                return Either.left(Code.FAILED_PRECONDITION.withMessage("不支持不同实体类型的聚合函数"));
            }
            buf.append(aggreStr, i, mt.start());
            i = mt.end();
//            System.out.println(String.format("%s,%s,%s", mt.start(), mt.end(), mt.group(1)));
        }
        if (i < aggreStr.length()) {
            buf.append(aggreStr, i, aggreStr.length());
        }
        if (propName != null && desc != null) {
            propName = propName.substring(0, propName.length() - 1);
            Property p = desc.property(propName);
            if (p == null || !(p instanceof BeanPropertyAssoc)) {
                return Either.left(Code.FAILED_PRECONDITION.withMessage("只支持关联属性聚合函数"));
            }
            return Either.right(new Tuple2<>(Optional.of(p), buf.toString()));
        } else {
            return Either.right(new Tuple2<>(Optional.empty(), buf.toString()));
        }
    }
}
