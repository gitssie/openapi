package com.gitssie.openapi.page;

import com.gitssie.openapi.rule.RuleProxyMap;
import com.gitssie.openapi.service.Provider;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Undefined;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FetchContextMap extends FetchContext {
    public FetchContextMap(Provider provider) {
        super(provider);
    }

    @Override
    public <E> List<E> newLinkedList() {
        return new NativeArray(16);
    }

    @Override
    public <E> List<E> newArrayList() {
        return new NativeArray(16);
    }

    @Override
    public <E> List<E> newArrayList(int size) {
        return new NativeArray(size);
    }

    @Override
    public Map<String, Object> newHashMap() {
        return new RuleProxyMap();
    }

    @Override
    public Map<String, Object> newLinkedHashMap() {
        return new RuleProxyMap(new LinkedHashMap<>());
    }

    @Override
    public Map<String, Object> newHashMap(int size) {
        return new RuleProxyMap(Maps.newHashMapWithExpectedSize(size));
    }
}
