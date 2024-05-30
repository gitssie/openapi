package com.gitssie.openapi.web.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gitssie.openapi.rule.RuleProxyMap;
import com.gitssie.openapi.models.layout.Component;
import com.gitssie.openapi.page.Field;
import com.gitssie.openapi.page.Model;
import com.gitssie.openapi.rule.Rules;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.ebean.Expression;
import io.vavr.Lazy;
import io.vavr.Tuple2;
import io.vavr.Value;
import io.vavr.control.Option;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.Context;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public abstract class AbstractQuery {
    protected static final int SIZE_MID = 1000;
    protected static final int SIZE_LARGE = 5000;
    protected static final int SIZE_XLARGE = 10000;
    protected QueryPredicate queryPredicate;
    private Pageable pageable;

    protected int page = 0;
    protected int size = 20;

    //id asc,created_at desc
    protected String orderBy;

    public void addPredicate(PredicateField predicate) {
        if (queryPredicate == null) {
            queryPredicate = new QueryPredicate(new LinkedList<>());
        }
        queryPredicate.add(predicate);
    }

    protected Pageable getPageable(int page, int size, String orderBy) {
        if (pageable != null) {
            return pageable;
        }
        Sort sort = Sort.unsorted();
        if (StringUtils.isNotEmpty(orderBy)) {
            String[] parts = orderBy.split(",");
            String p, o;
            List<Sort.Order> orders = new LinkedList<>();
            for (String part : parts) {
                part = part.trim();
                int i = part.indexOf(" ");
                if (i > 0) {
                    p = part.substring(0, i).trim();
                    o = part.substring(i + 1).trim();
                    if (StringUtils.equalsIgnoreCase("asc", o)) {
                        orders.add(Sort.Order.asc(p));
                    } else {
                        orders.add(Sort.Order.desc(p));
                    }
                } else {
                    p = part;
                    orders.add(Sort.Order.asc(p));
                }
            }
            sort = Sort.by(orders);
        }
        pageable = PageRequest.of(page, size, sort);
        return pageable;
    }

    @JsonIgnore
    public List<PredicateField> getPredicate() {
        return queryPredicate == null ? Collections.EMPTY_LIST : queryPredicate.getPredicate();

    }

    public void setExpression(String expression) {
        if (queryPredicate == null) {
            queryPredicate = new QueryPredicate(new LinkedList<>());
        }
        queryPredicate.setExpression(expression);
    }

    @JsonIgnore
    public String getExpression() {
        return queryPredicate == null ? null : queryPredicate.getExpression();
    }

    /**
     * 获取组件配置项配置的分页大小以及排序表达式
     *
     * @param context
     * @param component
     */
    public void setDefaults(Value<Context> context, Component component) {
        if (component == null) {
            return;
        }
        Integer size = Rules.toValue(context, Integer.class, component.get("size"), this.size);
        String orderBy = Rules.toValue(context, String.class, component.get("orderBy"), this.orderBy);
        if (size != null) {
            this.size = size;
        }
        if (orderBy != null) {
            this.orderBy = orderBy;
        }
    }

    /**
     * 验证查询表单的查询条件
     *
     * @param model
     * @return
     */
    public Option<Errors> validate(Model model) {
        return validateWithScope(model)._1;
    }

    public Option<Errors> validate(Option<Model> model) {
        return validateWithScope(model)._1;
    }

    public Tuple2<Option<Errors>, Map<String, Object>> validateWithScope(Model model) {
        Lazy<Context> context = Lazy.of(() -> Context.enter());
        try {
            return validate(context, model);
        } finally {
            if (context.isEvaluated()) {
                IOUtils.closeQuietly(context.get());
            }
        }
    }

    public Tuple2<Option<Errors>, Map<String, Object>> validateWithScope(Option<Model> model) {
        if (model.isDefined()) {
            return validateWithScope(model.get());
        }
        List<PredicateField> pfList = getPredicate();
        RuleProxyMap scope = new RuleProxyMap();
        for (PredicateField pf : pfList) {
            scope.put(pf.getField(), scope, pf.getBindValue());
        }
        return new Tuple2<>(Option.none(), scope);
    }

    private Tuple2<Option<Errors>, Map<String, Object>> validate(Value<Context> context, Model model) {
        setDefaults(context, model.getComponent());
        List<Tuple2<Field, PredicateField>> matched = Lists.newArrayList();
        List<PredicateField> pfList = getPredicate();
        Map<String, PredicateField> pfMap = Maps.newHashMapWithExpectedSize(pfList.size());

        //NativeObject scope = new NativeObject();
        RuleProxyMap scope = new RuleProxyMap();
        for (PredicateField pf : pfList) {
            pfMap.put(pf.getField(), pf);
            scope.put(pf.getField(), scope, pf.getBindValue());
        }
        for (Field field : model.getFields()) {
            Object value = getDefaultValue(context, field); //获取默认配置的值
            PredicateField pf = pfMap.get(field.getName());
            if (pf != null) {
                if (ObjectUtils.isEmpty(pf.getBindValue())) {
                    pf.setValue(value);
                    scope.put(field.getName(), scope, value);
                }
                if (StringUtils.isNotEmpty(field.getOp())) {
                    pf.setOp(field.getOp());//这里判断是否要支持多种查询表达式类型
                }
            } else {
                pf = new PredicateField(field.getName(), field.getOp(), value);
                scope.put(field.getName(), scope, value);
            }
            pf.setPath(field.getPath());
            matched.add(new Tuple2<>(field, pf));
        }
        String objectName = "predicate";
        MapBindingResult errors = new MapBindingResult(Collections.emptyMap(), "");

        List<PredicateField> newPredicates = new LinkedList<>();
        for (Tuple2<Field, PredicateField> tp : matched) {
            Field field = tp._1;
            PredicateField pf = tp._2;
            //进行参数验证
            Object value = pf.getBindValue();
            FieldError fieldError = field.validate(context, value, objectName, field.getName(), scope);
            if (fieldError != null) { //验证错误
                errors.addError(fieldError);
                continue;
            }
            if (field.getLazy() != null && (ObjectUtils.isNotEmpty(value) || field.getName() == null)) { //调用JS函数,返回默认特殊查询条件
                Object[] newArr = new Object[]{scope, value};
                Expression expr = Rules.runFunction(context, Expression.class, field.getLazy(), newArr);
                pf.setExpr(Option.of(expr));
            }
            if (ObjectUtils.isNotEmpty(value) || pf.getExpr().isDefined()) {
                newPredicates.add(pf);
            }
        }

        //使用验证通过的SQL表达式
        if (queryPredicate != null) {
            queryPredicate.setPredicate(newPredicates);
        }
        if (errors.hasErrors()) {
            return new Tuple2<>(Option.of(errors), scope);
        } else {
            return new Tuple2<>(Option.none(), scope);
        }
    }


    /**
     * 获取属性的默认值
     *
     * @param field
     * @return
     */
    private Object getDefaultValue(Value<Context> context, Field field) {
        Object value = Rules.toValue(context, field.getValue());
        return value;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public void setDefaultOrderBy(String orderBy) {
        if (StringUtils.isEmpty(getOrderBy())) {
            setOrderBy(orderBy);
        }
    }

    @JsonIgnore
    public Pageable getPageable() {
        return this.getPageable(page, size, orderBy);
    }

    public QueryPredicate getQueryPredicate() {
        return queryPredicate;
    }
}
