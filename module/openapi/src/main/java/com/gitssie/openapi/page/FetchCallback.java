package com.gitssie.openapi.page;

public interface FetchCallback {

    void apply(FetchContext context,Fetch next, Object beanList, Object beanMap, Object nextBeanList, Object nextBeanMap);
}
