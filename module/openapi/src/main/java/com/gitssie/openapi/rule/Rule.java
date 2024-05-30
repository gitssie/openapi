package com.gitssie.openapi.rule;

import io.vavr.Function3;
import io.vavr.Tuple2;
import io.vavr.Value;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.*;
import org.springframework.validation.FieldError;

import java.util.Map;

/**
 * @author: Awesome
 * @create: 2024-02-04 16:38
 */
public class Rule {
    private String name;
    private Object[] data = new Object[0];
    private Rule[] and = new Rule[0];
    private Rule[] or = new Rule[0];
    private String format;

    private TestFunction test;

    public Rule(String name, Function3<Object, String, Map<String, Object>, Boolean> test) {
        this.name = name;
        this.test = new TestFunction(test);
    }

    public Rule(String name, Object[] data, Function3<Object, String, Map<String, Object>, Boolean> test) {
        this.name = name;
        this.data = data;
        this.test = new TestFunction(test);
    }

    public Rule(String name, Object[] data, TestFunction test) {
        this.name = name;
        this.data = data;
        this.test = test;
    }

    public Rule(Function function) {
        this.test = new TestFunction(function);
    }

    public Rule and(Rule rule) {
        Rule res = clone();
        int len = this.and.length;
        res.and = copy(this.and, len + 1);
        res.and[len] = rule;
        res.or = copy(this.or, this.or.length);
        return res;
    }

    public Rule or(Rule rule) {
        Rule res = clone();
        int len = this.or.length;
        res.or = copy(this.or, len + 1);
        res.or[len] = rule;
        res.and = copy(this.and, this.and.length);
        return res;
    }

    public Rule clone() {
        Rule res = new Rule(this.name, this.data, this.test);
        res.format = format;
        return res;
    }

    private Rule[] copy(Rule[] rules, int len) {
        Rule[] res = new Rule[len];
        System.arraycopy(rules, 0, res, 0, rules.length);
        return res;
    }

    public FieldError validate(Value<Context> context, Object value, String objectName, String attribute, Map<String, Object> model) {
        Tuple2<Boolean, String> valid = this.test.apply(context, value, attribute, model);
        if (valid._1) {
            //验证AND
            for (Rule rule : this.and) {
                FieldError result = rule.validate(context, value, objectName, attribute, model);
                if (result != null) { //AND未通过
                    return result;
                }
            }
            return null;
        } else {
            //验证OR
            for (Rule rule : this.or) {
                FieldError result = rule.validate(context, value, objectName, attribute, model);
                if (result == null) { //有OR通过
                    return null;
                }
            }
        }
        int len = data.length;
        Object[] arg = new Object[len + 2];
        arg[0] = attribute;
        System.arraycopy(data, 0, arg, 1, len);
        String message = valid._2 != null ? valid._2 : StringUtils.defaultString(format, name);
        FieldError error = new FieldError(objectName, attribute, value, true, new String[]{name}, arg, message);
        return error;
    }

    public String getName() {
        return name;
    }

    public static Rule instance(Object rule) {
        if (rule instanceof Function) {
            return new Rule((Function) rule);
        }
        return (Rule) rule;
    }


    private static class TestFunction {
        private Function call;
        private Function3<Object, String, Map<String, Object>, Boolean> test;

        public TestFunction(Function call) {
            this.call = call;
        }

        public TestFunction(Function3<Object, String, Map<String, Object>, Boolean> test) {
            this.test = test;
        }

        public Tuple2<Boolean, String> apply(Value<Context> context, Object value, String attribute, Map<String, Object> model) {
            boolean pass = true;
            String msg = null;
            if (test != null) {
                pass = test.apply(value, attribute, model);
            } else if (call != null) {
                Object res = Rules.toValue(context, call, model, value, attribute);
                if (res instanceof Boolean) {
                    pass = (boolean) res;
                } else if (res instanceof String) {
                    pass = false;
                    msg = (String) res;
                }
            }
            return new Tuple2(pass, msg);
        }
    }
}
