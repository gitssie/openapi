package com.gitssie.openapi.page;

import com.gitssie.openapi.rule.Rule;
import com.google.common.collect.Maps;
import io.vavr.Value;
import io.vavr.control.Option;
import org.mozilla.javascript.Context;
import org.springframework.validation.FieldError;

import java.util.List;
import java.util.Map;

public class Field {
    private String name; //对象属性
    private String path; //ORM查询路径
    private String op; //ORM查询表达式 eq le gt range contains ...
    private Object value; //默认值
    private Rule rule; //验证规则
    private Object lazy; //数据延迟加载函数
    private Object format; //数据格式化表达式、函数
    private Object filter; //数据过滤函数
    private String field; //JSON属性
    private Object options;//选项格式化
    private transient Option<Map> optionsMap;

    public Field() {
    }

    public Field(String name) {
        this.name = name;
    }

    public Field(String name, String op, Object value, Rule rule) {
        this.name = name;
        this.op = op;
        this.value = value;
        this.rule = rule;
    }

    public Field(String name, String op, Object value, Rule rule, Object lazy, Object format, String path, String field) {
        this.name = name;
        this.op = op;
        this.value = value;
        this.rule = rule;
        this.lazy = lazy;
        this.format = format;
        this.path = path;
        this.field = field;
    }

    public String getField() {
        return field;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public Rule getRule() {
        return rule;
    }

    public void setRule(Rule rule) {
        this.rule = rule;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Object getLazy() {
        return lazy;
    }

    public Object getFormat() {
        return format;
    }

    public void setFormat(Object format) {
        this.format = format;
    }

    public Object getOptions() {
        return options;
    }

    public void setOptions(Object options) {
        this.options = options;
    }

    public Object getFilter() {
        return filter;
    }

    public void setFilter(Object filter) {
        this.filter = filter;
    }

    public Option<Map> getOptionsMap() {
        if (optionsMap != null) {
            return optionsMap;
        }
        if (options == null) {
            optionsMap = Option.none();
        } else if (options instanceof Map) {
            optionsMap = Option.of((Map) options);
        } else if (options instanceof List) {
            List arr = (List) options;
            Map map = Maps.newHashMapWithExpectedSize(arr.size());
            for (Object o : arr) {
                if (o instanceof Map) {
                    Map item = (Map) o;
                    Object value = item.get("value");
                    if (value == null) {
                        continue;
                    }
                    Object label = item.get("label");
                    if (value instanceof Number) {
                        value = ((Number) value).intValue();
                    }
                    map.put(value, label);
                } else {
                    break;
                }
            }
            optionsMap = Option.of(map);
        } else {
            optionsMap = Option.none();
        }
        return optionsMap;
    }

    public FieldError validate(Value<Context> context, Object value, String objectName, String attribute, Map<String, Object> model) {
        if (rule == null) {
            return null;
        }
        return rule.validate(context, value, objectName, attribute, model);
    }

    @Override
    public String toString() {
        return name;
    }
}
