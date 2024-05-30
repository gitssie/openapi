package com.gitssie.openapi.web.query;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

public class QueryAggre extends AbstractQuery {
    @Valid
    @NotEmpty
    private List<String> aggre;
    @Valid
    private QueryPredicate query;

    public List<String> getAggre() {
        return aggre;
    }

    public void setAggre(List<String> aggre) {
        this.aggre = aggre;
    }

    public QueryPredicate getQuery() {
        return query;
    }

    public void setQuery(QueryPredicate query) {
        this.query = query;
    }
}
