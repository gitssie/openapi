package com.gitssie.openapi.auth;

import com.gitssie.openapi.models.auth.URLVerb;
import com.gitssie.openapi.models.auth.VerbAttribute;
import io.ebean.Database;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Component
@Deprecated
public class URLVerbSecurityMetadataSource implements FilterInvocationSecurityMetadataSource, ApplicationListener<ApplicationReadyEvent> {
    private String API_KEY = "apiKey";
    private Database database;
    private FilterInvocationSecurityMetadataSource originMetadataSource;

    private final Map<RequestMatcher, VerbAttribute> matcherMap;

    public URLVerbSecurityMetadataSource(Database database) {
        this.database = database;
        this.matcherMap = new HashMap<>();
    }

    @Override
    public Collection<ConfigAttribute> getAttributes(Object object) throws IllegalArgumentException {
        final HttpServletRequest request = ((FilterInvocation) object).getRequest();
        return match(request, object);
    }

    protected Collection<ConfigAttribute> match(HttpServletRequest request, Object object) {
        RequestMatcher.MatchResult result;
        Map<String, String> var;
        String apiKey = null;
        List<ConfigAttribute> res = new LinkedList<>();
        boolean matched = false;
        for (Map.Entry<RequestMatcher, VerbAttribute> match : matcherMap.entrySet()) {
            result = match.getKey().matcher(request);
            if (result.isMatch()) {
                matched = true;
                var = result.getVariables();
                if (var != null) {
                    apiKey = var.get(API_KEY);
                }
                if (StringUtils.isEmpty(apiKey)) {
                    res.add(match.getValue());
                } else {
                    res.add(match.getValue().clone(apiKey));
                }
                break; //@TODO 是否还需要进行循环匹配?
            }
        }

        if (!matched && originMetadataSource != null) {
            return originMetadataSource.getAttributes(object);
        }
        return res;
    }

    @Override
    public Collection<ConfigAttribute> getAllConfigAttributes() {
        return null;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return false;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        //loadURLVerb();
    }

    protected void loadURLVerb() {
        List<URLVerb> verbs = database.createQuery(URLVerb.class)
                .fetch("permission", "code")
                .findList();

        for (URLVerb verb : verbs) {
            RequestMatcher matcher = new AntPathRequestMatcher(verb.getAntPath(), verb.getMethod());
            String apiKey = "UNKNOWN";
            String code;
            if (verb.getPermission() != null) {
                apiKey = (verb.getPermission().getCode());
            }
            code = verb.getCode();
            VerbAttribute attribute = new VerbAttribute(apiKey, code);
            matcherMap.put(matcher, attribute);
        }
    }

    public FilterInvocationSecurityMetadataSource getOriginMetadataSource() {
        return originMetadataSource;
    }

    public void setOriginMetadataSource(FilterInvocationSecurityMetadataSource originMetadataSource) {
        this.originMetadataSource = originMetadataSource;
    }
}
