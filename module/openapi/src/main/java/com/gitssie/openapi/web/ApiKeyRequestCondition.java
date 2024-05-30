package com.gitssie.openapi.web;

import com.gitssie.openapi.web.annotation.ActionMethod;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.server.PathContainer;
import org.springframework.web.servlet.mvc.condition.AbstractRequestCondition;
import org.springframework.web.servlet.mvc.condition.ParamsRequestCondition;
import org.springframework.web.util.ServletRequestPathUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ApiKeyRequestCondition extends AbstractRequestCondition<ParamsRequestCondition> {
    private final String apiKey;
    private final Set<String> expression;
    private final ParamsRequestCondition funcNameCondition;
    private final ParamsRequestCondition matchResult;

    public ApiKeyRequestCondition(String apiKey, String funcName) {
        this.apiKey = apiKey;
        expression = new HashSet<>(4);
        expression.add(apiKey);
        if (StringUtils.isNotBlank(funcName)) {
            expression.add(funcName);
            funcNameCondition = new ParamsRequestCondition(ActionMethod.FUNC_NAME + "=" + funcName);
            matchResult = new ParamsRequestCondition(apiKey, funcName);
        } else {
            funcNameCondition = null;
            matchResult = new ParamsRequestCondition(apiKey);
        }
    }

    @Override
    public ParamsRequestCondition combine(ParamsRequestCondition other) {
        return null;
    }

    @Override
    public ParamsRequestCondition getMatchingCondition(HttpServletRequest request) {
        if (StringUtils.isEmpty(apiKey)) {
            return null;
        }
        PathContainer path = ServletRequestPathUtils.getParsedRequestPath(request).pathWithinApplication();
        String lookupPath = path.value();
        int idx = lookupPath.indexOf(apiKey);
        int end = idx + apiKey.length();
        boolean match = idx > 0 && lookupPath.charAt(idx - 1) == '/';
        match = match && (lookupPath.length() == end || lookupPath.charAt(end) == '/');
        if (!match) {
            return null;
        }
        if (funcNameCondition != null) {
            ParamsRequestCondition condition = funcNameCondition.getMatchingCondition(request);
            if (condition == null) {
                return null;
            }
        }
        return matchResult;
    }

    @Override
    public int compareTo(ParamsRequestCondition other, HttpServletRequest request) {
        return 0;
    }

    @Override
    protected Collection<?> getContent() {
        return expression;
    }

    @Override
    protected String getToStringInfix() {
        return " && ";
    }
}
