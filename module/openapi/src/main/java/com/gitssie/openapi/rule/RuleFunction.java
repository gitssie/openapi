package com.gitssie.openapi.rule;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.util.function.Function;

/**
 * @author: Awesome
 * @create: 2024-02-04 17:22
 */
public class RuleFunction extends BaseFunction {

    private Function<Object[], Rule> rule;

    public RuleFunction(Function<Object[], Rule> rule) {
        this.rule = rule;
    }

    @Override
    public Object call(Context ctx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return rule.apply(args);
    }
}
