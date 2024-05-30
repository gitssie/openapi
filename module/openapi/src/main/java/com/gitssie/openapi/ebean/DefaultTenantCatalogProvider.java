package com.gitssie.openapi.ebean;

import io.ebean.config.TenantCatalogProvider;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class DefaultTenantCatalogProvider implements TenantCatalogProvider {
    private Expression tenantCatalogExp;

    public DefaultTenantCatalogProvider(@Value("${ebean.tenant.catalogEl:}") String tenantCatalogEl) {
        if (StringUtils.isNotEmpty(tenantCatalogEl)) {
            ExpressionParser parser = new SpelExpressionParser();
            tenantCatalogExp = parser.parseExpression(tenantCatalogEl);
        }
    }

    @Override
    public String catalog(Object tenantId) {
        if (tenantCatalogExp == null) {
            return null;
        }
        StandardEvaluationContext ctx = new StandardEvaluationContext(tenantId);
        return tenantCatalogExp.getValue(ctx, String.class);
    }
}
