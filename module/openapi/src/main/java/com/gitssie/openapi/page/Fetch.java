package com.gitssie.openapi.page;

import com.gitssie.openapi.rule.Rules;
import com.gitssie.openapi.service.Provider;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.ebean.*;
import io.ebean.bean.EntityBean;
import io.ebean.plugin.BeanType;
import io.ebeaninternal.server.deploy.BeanDescriptor;
import io.ebeaninternal.server.el.ElFilter;
import io.vavr.Function2;
import io.vavr.Function3;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class Fetch extends Graph<Fetch> {
    private final FetchGroup<?> fetchGroup;
    private Expression where;
    private final Function<Query<?>, Object> fetchType;
    private final AtomicBoolean fetchState = new AtomicBoolean(false);
    private Object fetchResult;

    public Fetch(Model model, Provider provider, BeanType<?> desc, FetchGroup<?> fetchGroup, AssocType assocType) {
        super(model, desc, provider, assocType);
        this.fetchGroup = fetchGroup;
        this.fetchType = new DoFetch(assocType);
    }

    public void and(Expression expr) {
        if (expr == null) {
            return;
        }
        if (where == null) {
            where = expr;
        } else {
            where = Expr.and(where, expr);
        }
    }

    public Object fetch() {
        return fetch(this.fetchType);
    }

    public <T> T fetch(Function<Query<?>, T> fetchType) {
        boolean unset = fetchState.compareAndSet(false, true);
        if (!unset) {
            return (T) fetchResult;
        }
        Query<?> query = provider.createQuery(desc.type()).select((FetchGroup) fetchGroup);
        if (where != null) {
            query.where(where);
        }
        T result = fetchType.apply(query);
        fetchResult = result;
        return result;
    }

    protected Object filter(FetchContext context, BeanType<?> desc, Object beanList) {
        ModelAssoc assoc = model.assocAnnotation;
        if (assoc != null && assoc.filter != null) {
            Filter<?> filter = new ElFilter((BeanDescriptor<?>) desc);
            Rules.runFunction(context.get(), assoc.filter, filter);
            if (beanList instanceof List) {
                return filter.filter((List) beanList);
            } else {
                List oneList = Lists.newArrayListWithExpectedSize(1);
                oneList.add(beanList);
                oneList = filter.filter(oneList);
                if (oneList.isEmpty()) {
                    return null;
                } else {
                    return oneList.get(0); //only one bean
                }
            }
        }
        return beanList;
    }

    protected boolean prepareJoin(Object result) {
        if (ObjectUtils.isEmpty(result)) {
            return false;
        }
        return this.setJoinWhere(result);
    }

    protected boolean setJoinWhere(Object joinRef) {
        if (this.mappedBy.isAssocId()) { //实体关联属性
            return setJoinById(joinRef);
        } else {
            return setJoinByOtherMapped(joinRef);
        }
    }

    private boolean setJoinById(Object joinRef) {
        if (joinRef instanceof Collection) {
            and(Expr.in(mappedBy.name(), (Collection) joinRef));
        } else {
            and(Expr.eq(mappedBy.name(), joinRef));
        }
        return true;
    }

    private boolean setJoinByOtherMapped(Object joinRef) {
        if (joinRef instanceof List) {
            List data = (List) joinRef;
            Set values = Sets.newHashSetWithExpectedSize(data.size());
            for (Object bean : data) {
                Object value = mapped.value(bean);
                if (ObjectUtils.isNotEmpty(value)) {
                    values.add(value);
                }
            }
            if (values.isEmpty()) {
                return false;
            }
            this.and(Expr.in(mappedBy.name(), values));
        } else {
            Object value = mapped.value(joinRef);
            if (ObjectUtils.isEmpty(value)) {
                return false;
            }
            this.and(Expr.eq(mappedBy.name(), value));
        }
        return true;
    }

    protected void addJoin(String name, Model model, BeanType<?> childDesc, Provider provider) {
        addJoin(name, model, childDesc, provider, (type) -> model.fetch(childDesc, provider, type));
    }

    public Object toJSON(Object bean) {
        FetchContext context = new FetchContext(provider);
        try (context) {
            ModelConverter modelConverter = new ModelConverter(provider);
            Object result = doFetch(context, bean, (fetch, obj) -> modelConverter.toMap(context, fetch.model, fetch.desc, obj), this::setAssocValue);
            return result;
        }
    }

    private Object doFetch(final FetchContext context, Object bean, Function2<Fetch, Object, Object> callback) {
        return doFetch(context, bean, callback, null);
    }

    public Object doFetch(final FetchContext context, Object bean, Function2<Fetch, Object, Object> callback, FetchCallback onPost) {
        Object beanMap = callback.apply(this, bean);
        if (this.next == null) {
            return beanMap;
        }
        for (Fetch next : this.next) {
            //Object beanFilter = next.filter(context, this.desc, bean); //filter data by where expression
            boolean joined = next.prepareJoin(bean); //add join query
            if (!joined) { //关联的数据为空,无需查询
                continue;
            }
            Object nextBean = next.fetch();
            Object nextMap = next.doFetch(context, nextBean, callback, onPost);
            if (onPost != null) {
                onPost.apply(context, next, bean, beanMap, nextBean, nextMap);
            }
        }
        return beanMap;
    }

    private void setAssocValue(FetchContext context, Fetch next, Object beanList, Object beanMap, Object nextBeanList, Object nextBeanMap) {
        ModelAssoc assoc = next.model.assocAnnotation;
        if (beanMap instanceof Map) {
            Map map = (Map) beanMap;
            if (assoc != null && assoc.isMap()) {
                List<Map> nextBeanMaps = (List) nextBeanMap;
                if (ObjectUtils.isEmpty(nextBeanMaps)) {
                    map.put(next.mapProperty, null);
                } else {
                    map.put(next.mapProperty, nextBeanMaps.get(0)); //Map关联只可能有一条数据
                }
            } else {
                map.put(next.mapProperty, nextBeanMap);
            }
        } else if (beanList instanceof List) {
            Object format = next.model.getFormat();
            boolean isFormat = Rules.isFunction(format);
            if (assoc == null || !assoc.isMap()) {
                return;
            } else if (ObjectUtils.isEmpty(nextBeanMap) && !isFormat) {
                return;
            }
            List<Map> nextBeanMaps = (List) nextBeanMap;
            List<EntityBean> nextBeanLists = (List<EntityBean>) nextBeanList;

            if (nextBeanMaps.size() != nextBeanLists.size()) {
                throw new IllegalStateException(String.format("nextBeanList size(%s) not equals nextBeanMap size(%s)", nextBeanLists.size(), nextBeanLists.size()));
            }

            //被关联的数据
            Map childMap = Maps.newHashMapWithExpectedSize(nextBeanMaps.size());
            for (int i = 0; i < nextBeanMaps.size(); i++) {
                Map map = nextBeanMaps.get(i);
                Object key = map.get(next.mappedBy.name());
                if (ObjectUtils.isEmpty(key)) {
                    key = next.mappedBy.value(nextBeanLists.get(i));
                }
                if (ObjectUtils.isEmpty(key)) {
                    continue;
                }
                childMap.put(key, map);
            }

            //数据关联
            List<EntityBean> beanLists = (List) beanList;
            List<Map> beanMaps = (List) beanMap;
            if (beanLists.size() != beanMaps.size()) {
                throw new IllegalStateException(String.format("beanList size(%s) not equals beanMap size(%s)", beanLists.size(), beanMaps.size()));
            }
            for (int i = 0; i < beanMaps.size(); i++) {
                Map map = beanMaps.get(i);
                Object key = map.get(next.mapped.name());
                if (ObjectUtils.isEmpty(key)) {
                    key = next.mapped.value(beanLists.get(i));
                }
                Object value = childMap.get(key);
                if (isFormat) {
                    value = Rules.toValue(context.context, format, value);
                }
                map.put(next.mapProperty, value);
            }
        }
    }

    private class DoFetch<T> implements Function<Query<T>, Object> {
        private final AssocType assocType;

        public DoFetch(AssocType assocType) {
            this.assocType = assocType;
        }

        @Override
        public Object apply(Query<T> query) {
            if (assocType == AssocType.MANY) {
                return query.findList();
            } else {
                return query.findOne();
            }
        }
    }

}
