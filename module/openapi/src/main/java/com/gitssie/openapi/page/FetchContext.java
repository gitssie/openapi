package com.gitssie.openapi.page;

import com.gitssie.openapi.rule.Rules;
import com.gitssie.openapi.service.Provider;
import com.gitssie.openapi.xentity.XEntityCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.ebean.bean.EntityBean;
import io.ebean.plugin.BeanType;
import io.ebean.plugin.Property;
import io.ebeaninternal.server.deploy.BeanProperty;
import io.ebeaninternal.server.deploy.BeanPropertyAssocOne;
import io.vavr.Function2;
import io.vavr.Lazy;
import io.vavr.Tuple2;
import io.vavr.Value;
import io.vavr.control.Option;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Undefined;

import java.io.Closeable;
import java.util.*;
import java.util.function.Supplier;

public class FetchContext implements Closeable {
    public static String LABEL = "label";
    protected final Provider provider;
    protected final Lazy<Context> context;
    protected boolean lazyFetch;
    private Map<Class<?>, BeanType<?>> desc;
    private Map<BeanType<?>, List<Path>> idMap;
    private Map<Tuple2<Boolean, Property>, List<Path>> mappedMap;
    private Map<Field, List<Path>> lazyMap;
    private FetchContext stack;

    public FetchContext(Provider provider) {
        this.provider = provider;
        this.context = Lazy.of(() -> Context.enter());
    }

    public FetchContext(Provider provider, Lazy<Context> context) {
        this.provider = provider;
        this.context = context;
    }

    private FetchContext newStack() {
        if (stack == null) {
            stack = new FetchContext(provider, context);
            stack.lazyFetch = this.lazyFetch;
            stack.desc = this.desc;
        }
        return stack;
    }

    public BeanType<?> desc(Object bean) {
        Class<?> clazz = bean.getClass();
        if (this.desc == null) {
            this.desc = Maps.newHashMapWithExpectedSize(4);
        }
        BeanType<?> res = this.desc.get(clazz);
        if (res == null) {
            res = provider.desc(clazz);
            this.desc.put(clazz, res);
        }
        return res;
    }

    public LazyValue addPath(Property property, BeanType<?> desc, EntityBean bean, List<Field> fields, Function2<FetchContext, Object, Object> callback) {
        LazyValue value = addPath(property, desc, bean, fields);
        value.setCallback(callback);
        return value;
    }

    public LazyValue addPath(String name, Object id, BeanType<?> desc, List<Field> fields, Function2<FetchContext, Object, Object> callback) {
        LazyValue value = addPath(name, id, desc, fields);
        value.setCallback(callback);
        return value;
    }

    public LazyValue addPath(String name, Object valueRef, BeanProperty mappedBy, List<Field> fields, Function2<FetchContext, Object, Object> callback) {
        LazyValue value = addPath(name, valueRef, mappedBy, fields, false);
        value.setCallback(callback);
        return value;
    }

    public LazyValue addPathMany(String name, Object valueRef, BeanProperty mappedBy, List<Field> fields, Function2<FetchContext, Object, Object> callback) {
        LazyValue value = addPath(name, valueRef, mappedBy, fields, true);
        value.setCallback(callback);
        return value;
    }

    public LazyValue addPath(Property property, BeanType<?> desc, EntityBean bean, Field field) {
        LazyValue lazyValue = addPath(property, desc, bean, (List<Field>) null);
        setFormat(field, lazyValue, bean);
        return lazyValue;
    }

    private LazyValue addPath(Property property, BeanType<?> desc, EntityBean bean, List<Field> fields) {
        if (idMap == null) {
            idMap = Maps.newHashMap();
        }
        return addPathToFetchMap(idMap, property.name(), desc, bean, fields);
    }

    public LazyValue addPath(String name, Object id, BeanType<?> desc, List<Field> fields) {
        if (id == null) {
            return null;
        }
        if (idMap == null) {
            idMap = Maps.newHashMap();
        }
        return addPathToFetchMap(idMap, name, id, desc, fields);
    }

    public LazyValue addPath(String name, Object valueRef, BeanProperty mappedBy, List<Field> fields, boolean many) {
        if (valueRef == null) {
            return null;
        }
        if (mappedBy.isScalar()) {
            valueRef = mappedBy.convert(valueRef);
        }
        if (mappedMap == null) {
            mappedMap = Maps.newHashMapWithExpectedSize(8);
        }
        Tuple2<Boolean, Property> key = new Tuple2<>(many, mappedBy);
        List<Path> paths = mappedMap.get(key);
        if (paths == null) {
            paths = new LinkedList<>();
            mappedMap.put(key, paths);
        }
        LazyValue value = new LazyValue(name);
        paths.add(new Path(valueRef, value, fields));
        return value;
    }

    public LazyValue addLazyValue(String name, Object value, Field field, Object bean) {
        LazyValue res = new LazyValue(name);
        setFormat(field, res, bean);
        res.setValue(value);
        return res;
    }

    public LazyValue addLazyLabel(String name, Object value, Field field) {
        LazyValue res = new LazyValue(name);
        res.setValue(value);
        setOptionLabel(field, res);
        return res;
    }

    public LazyValue addLazyPath(String name, Object bean, Field field, Function2<FetchContext, Object, Object> callback) {
        LazyValue value = addLazyPathValue(name, bean, field);
        value.setCallback(callback);
        return value;
    }

    public LazyValue addLazyPath(String name, Object bean, Field field) {
        return addLazyPathValue(name, bean, field);
    }

    public LazyValue addLazyPathValue(String name, Object bean, Field field) {
        if (bean == null) {
            return null;
        }
        if (lazyMap == null) {
            lazyMap = Maps.newHashMapWithExpectedSize(8);
        }
        List<Path> idSet = lazyMap.get(field);
        if (idSet == null) {
            idSet = new LinkedList<>();
            lazyMap.put(field, idSet);
        }
        LazyValue value = new LazyValue(name);
        setFormat(field, value, bean);
        idSet.add(new Path(bean, value, null));
        return value;
    }

    private LazyValue addPathToFetchMap(Map<BeanType<?>, List<Path>> idMap, String name, BeanType<?> desc, EntityBean bean, List<Field> fields) {
        Object id = desc.idProperty().value(bean);
        return addPathToFetchMap(idMap, name, id, desc, fields);
    }

    private LazyValue addPathToFetchMap(Map<BeanType<?>, List<Path>> idMap, String name, Object id, BeanType<?> desc, List<Field> fields) {
        if (id == null) {
            return null;
        }
        List<Path> idSet = idMap.get(desc);
        if (idSet == null) {
            idSet = new LinkedList<>();
            idMap.put(desc, idSet);
        }
        LazyValue value = new LazyValue(name);
        idSet.add(new Path(id, value, fields));
        return value;
    }

    public String getLabelKey(String key) {
        return key + "-" + LABEL;
    }

    private void doFetch() {
        List<Supplier<?>> collects = new LinkedList<>();
        doFetch(collects);
        doFetchMappedBy(collects);
        doLazyFetch(collects);
        for (Supplier<?> collect : collects) {
            collect.get();
        }
        if (stack != null) {
            stack.doFetch();
        }
    }

    @Override
    public void close() {
        try {
            doFetch();
        } finally {
            if (context.isEvaluated()) {
                IOUtils.closeQuietly(context.get());
            }
        }
    }

    private void doFetch(List<Supplier<?>> collects) {
        //根据关联ID加载数据
        if (ObjectUtils.isEmpty(idMap)) {
            return;
        }
        for (Map.Entry<BeanType<?>, List<Path>> entry : idMap.entrySet()) {
            BeanType<?> desc = entry.getKey();
            XEntityCache entity = provider.getEntity(desc.type());
            Property id = desc.idProperty();
            Property code = provider.codeProperty(desc);
            Property nameable = null;
            if (entity.getNameable() != null) {
                nameable = desc.property(entity.getNameable().getName());
            }
            String select = collectSelect(entry.getValue(), code, nameable);
            if (StringUtils.isEmpty(select)) {
                collects.add(new CollectPath(entry.getValue()));
            } else {
                List<Object> idSet = collectIdList(entry.getValue());
                Map beanMap = provider.createQuery(entity.getBeanType())
                        .select(select)
                        .where().in(id.name(), idSet)
                        .setMapKey(id.name())
                        .findMap();

                collects.add(new CollectEntry(this, id, code, nameable, entry.getValue(), beanMap));
            }
        }
        idMap.clear();
    }

    private void doFetchMappedBy(List<Supplier<?>> collects) {
        if (ObjectUtils.isEmpty(mappedMap)) {
            return;
        }
        Property code = null;
        Property nameable = null;
        for (Map.Entry<Tuple2<Boolean, Property>, List<Path>> entry : mappedMap.entrySet()) {
            Tuple2<Boolean, Property> tuple = entry.getKey();
            BeanProperty mappedBy = (BeanProperty) tuple._2;
            String select = collectSelect(entry.getValue(), mappedBy, code, nameable);
            if (StringUtils.isEmpty(select)) {
                collects.add(new CollectPath(entry.getValue()));
            } else {
                List<Object> idSet = collectIdList(entry.getValue());
                List beanList = provider.createQuery(mappedBy.descriptor().type())
                        .select(select)
                        .where().in(mappedBy.name(), idSet)
                        .findList();

                if (tuple._1) { //many
                    collects.add(new CollectMappedByMany(this, mappedBy, code, nameable, entry.getValue(), beanList));
                } else {
                    collects.add(new CollectMappedBy(this, mappedBy, code, nameable, entry.getValue(), beanList));
                }
            }
        }
        mappedMap.clear();
    }


    private void doLazyFetch(List<Supplier<?>> collects) {
        //根据延迟加载函数进行加载数据
        if (ObjectUtils.isEmpty(lazyMap)) {
            return;
        }
        for (Map.Entry<Field, List<Path>> entry : lazyMap.entrySet()) {
            collects.add(doLazyFetch(entry.getKey(), entry.getValue()));
        }
    }

    private Supplier<?> doLazyFetch(Field field, List<Path> paths) {
        List<Object> idSet = collectIdList(paths);
        //调用JS包装的Java方法，进行数据查询,返回结果需要按ID分组
        Object result = Rules.toValue(context, null, field.getLazy(), idSet);
        final Map mapResult = jsToMap(result);
        //设置取值
        return () -> {
            FetchContext stack = newStack();
            for (Path path : paths) {
                Object value = mapResult.get(path.id);
                if (ObjectUtils.isEmpty(value)) {
                    continue;
                } else if (path.value.isEmpty()) {
                    path.value.setValue(value);
                } else {
                    Object realValue = path.value.apply(stack, value);
                    path.value.setValue(realValue);
                }
            }
            return true;
        };
    }

    private String collectSelect(List<Path> values, Property... properties) {
        StringBuilder buf = new StringBuilder();
        for (Property property : properties) {
            if (property == null) {
                continue;
            }
            buf.append(property.name());
            buf.append(",");
        }
        Set<String> fields = new HashSet<>();
        for (Path value : values) {
            if (ObjectUtils.isEmpty(value.fields)) {
                continue;
            }
            for (Field field : value.fields) {
                fields.add(field.getName());
            }
        }
        for (String field : fields) {
            buf.append(field).append(",");
        }
        if (buf.length() > 0) {
            buf.setLength(buf.length() - 1);
        }
        return buf.toString();
    }


    private List<Object> collectIdList(List<Path> values) {
        List<Object> idSet = Lists.newArrayListWithExpectedSize(values.size());
        for (Path value : values) {
            idSet.add(value.id);
        }
        return idSet;
    }

    private void setOptionLabel(Field field, LazyValue lazyValue) {
        Option<Map> options = field.getOptionsMap();
        if (options.isEmpty()) {
            return;
        }
        Map optionsMap = options.get();
        lazyValue.setFormat(value -> {
            if (value == null && field.getValue() != null) {
                value = Rules.toValue(context, field.getValue());
            }
            if (value == null) {
                return null;
            }
            return optionsMap.get(value);
        });
    }


    private void setFormat(Field field, LazyValue lazyValue, Object bean) {
        Object format = field.getFormat();
        if (field.getValue() != null) { //默认值
            lazyValue.setFormat(value -> {
                if (value == null) {
                    value = Rules.toValue(context, field.getValue(), bean);
                }
                if (ObjectUtils.isNotEmpty(format)) {
                    return this.formatValue(value, format); //格式化
                } else {
                    return value;
                }
            });
        } else if (ObjectUtils.isNotEmpty(format)) { //格式化
            lazyValue.setFormat((e) -> this.formatValue(e, format));
        }
    }

    private Object formatValue(Object value, Object format) {
        if (Rules.isFunction(format)) {
            return Rules.runFunction(context, format, value);
        } else if (format instanceof String) {
            return Rules.format.format(value, (String) format);
        }
        return value;
    }

    private static class Path {
        Object id; //id or bean
        LazyValue value;
        List<Field> fields;

        public Path(Object id, LazyValue value, List<Field> fields) {
            this.id = id;
            this.value = value;
            this.fields = fields;
        }
    }

    private static class CollectEntry implements Supplier<Boolean> {
        protected FetchContext context;
        protected Property[] properties;
        protected String[] propertiesName;
        protected List<Path> collects;
        protected Map beanMap;

        public CollectEntry(FetchContext context, Property id, Property code, Property nameable, List<Path> collects, Map beanMap) {
            this.context = context;
            this.properties = new Property[]{id, code, nameable};
            this.propertiesName = new String[]{id.name(), code == null ? null : code.name(), LABEL};
            this.collects = collects;
            this.beanMap = beanMap;
        }

        public CollectEntry(FetchContext context, Property[] properties, String[] propertiesName, List<Path> collects) {
            this.context = context;
            this.properties = properties;
            this.propertiesName = propertiesName;
            this.collects = collects;
        }

        protected Object filterBean(Path path) {
            return beanMap.get(path.id);
        }

        protected boolean isEmpty() {
            return ObjectUtils.isEmpty(beanMap);
        }

        protected void initialize() {

        }

        protected boolean collectValues() {
            initialize();
            if (isEmpty()) {
                return true;
            }
            FetchContext stack = context.newStack();
            for (Path path : collects) {
                Object beanOrList = filterBean(path);
                if (ObjectUtils.isEmpty(beanOrList)) {
                    continue;
                }
                LazyValue lazyValue = path.value;
                if (lazyValue.isEmpty()) {
                    Object result = collectDefaultValue(beanOrList, properties, propertiesName);
                    lazyValue.setValue(result);
                } else {
                    Object realValue = lazyValue.apply(stack, beanOrList);
                    if (realValue != null) {
                        collectAfterCallback(realValue, beanOrList, properties, propertiesName);
                    }
                }
            }
            return true;
        }

        @Override
        public Boolean get() {
            return collectValues();
        }

        private void collectAfterCallback(Object realValue, Object beanOrList, Property[] properties, String[] names) {
            if (realValue instanceof Map) {
                collectDefaultValue((Map) realValue, beanOrList, properties, names);
            } else if (realValue instanceof Collection) {
                Collection beanList = (Collection) realValue;
                for (Object bean : beanList) {
                    if (bean instanceof Map) {
                        collectDefaultValue((Map) bean, beanOrList, properties, names);
                    }
                }
            }
        }

        private Object collectDefaultValue(Object beanOrList, Property[] properties, String[] names) {
            if (beanOrList instanceof List) {
                List beanList = (List) beanOrList;
                List resList = context.newLinkedList();
                for (Object bean : beanList) {
                    Map<String, Object> resMap = context.newHashMap(properties.length + 1);
                    collectDefaultValue(resMap, bean, properties, names);
                    resList.add(resMap);
                }
                return resList;
            } else {
                Map<String, Object> resMap = context.newHashMap(properties.length + 1);
                return collectDefaultValue(resMap, beanOrList, properties, names);
            }
        }

        private Map<String, Object> collectDefaultValue(Map<String, Object> resMap, Object bean, Property[] properties, String[] names) {
            for (int i = 0; i < properties.length; i++) {
                if (properties[i] == null || names[i] == null) {
                    continue;
                }
                if (!resMap.containsKey(names[i])) {
                    resMap.put(names[i], properties[i].value(bean));
                }
            }
            return resMap;
        }
    }


    private static class CollectMappedBy extends CollectEntry {
        private Property mappedBy;
        private List beanList;

        public CollectMappedBy(FetchContext context, Property mappedBy, Property code, Property nameable, List<Path> collects, List beanList) {
            super(context, new Property[]{code, nameable}, new String[]{code == null ? null : code.name(), LABEL}, collects);
            this.mappedBy = mappedBy;
            this.beanList = beanList;
        }

        @Override
        protected void initialize() {
            if (beanMap == null) {
                beanMap = Maps.newHashMapWithExpectedSize(beanList.size());
                Property id = null;
                for (Object bean : beanList) {
                    Object mappedValue = mappedBy.value(bean);
                    if (mappedValue == null) {
                        continue;
                    }
                    if (mappedValue instanceof EntityBean) {
                        if (id == null) {
                            if ((mappedBy instanceof BeanPropertyAssocOne)) {
                                id = ((BeanPropertyAssocOne<?>) mappedBy).targetDescriptor().idProperty();
                            }
                        }
                        if (id == null) {
                            continue;
                        }
                        beanMap.putIfAbsent(id.value(mappedValue), bean);
                    } else {
                        beanMap.putIfAbsent(mappedValue, bean);
                    }
                }
                beanList = null;
            }
        }
    }

    private static class CollectMappedByMany extends CollectEntry {
        private Property mappedBy;
        private List beanList;

        public CollectMappedByMany(FetchContext context, Property mappedBy, Property code, Property nameable, List<Path> collects, List beanList) {
            super(context, new Property[]{code, nameable}, new String[]{code == null ? null : code.name(), LABEL}, collects);
            this.mappedBy = mappedBy;
            this.beanList = beanList;
        }

        @Override
        protected void initialize() {
            if (beanMap == null) {
                beanMap = Maps.newHashMapWithExpectedSize(beanList.size());
                Property id = null;
                Object idValue;

                for (Object bean : beanList) {
                    Object mappedValue = mappedBy.value(bean);
                    if (mappedValue == null) {
                        continue;
                    }
                    if (mappedValue instanceof EntityBean) {
                        if (id == null) {
                            if ((mappedBy instanceof BeanPropertyAssocOne)) {
                                id = ((BeanPropertyAssocOne<?>) mappedBy).targetDescriptor().idProperty();
                            }
                        }
                        if (id == null) {
                            continue;
                        }
                        idValue = id.value(mappedValue);
                        addToMap(beanMap, idValue, bean);
                    } else {
                        addToMap(beanMap, mappedValue, bean);
                    }
                }
                beanList = null;
            }
        }

        private void addToMap(Map beanMap, Object key, Object value) {
            List list = (List) beanMap.get(key);
            if (list == null) {
                list = Lists.newLinkedList();
                beanMap.put(key, list);
            }
            list.add(value);
        }
    }

    private static class CollectPath implements Supplier<Boolean> {
        private List<Path> collects;

        public CollectPath(List<Path> collects) {
            this.collects = collects;
        }

        @Override
        public Boolean get() {
            for (Path value : collects) {
                LazyValue lazyValue = value.value;
                lazyValue.setValue(value.id);
            }
            return true;
        }
    }

    public Value<Context> get() {
        return context;
    }

    public Map jsToMap(Object value) {
        if (value == null || value instanceof Undefined) {
            return null;
        } else if (value instanceof Map) {
            return (Map) value;
        } else {
            return (Map) Context.jsToJava(value, Map.class);
        }
    }

    public boolean enableLazyFetch() {
        return lazyFetch;
    }

    public void setLazyFetch(boolean lazyFetch) {
        this.lazyFetch = lazyFetch;
    }

    public <E> List<E> newLinkedList() {
        return Lists.newLinkedList();
    }

    public <E> List<E> newArrayList() {
        return Lists.newArrayList();
    }

    public <E> List<E> newArrayList(int size) {
        return Lists.newArrayListWithExpectedSize(size);
    }

    public Map<String, Object> newHashMap() {
        return Maps.newLinkedHashMap();
    }

    public Map<String, Object> newLinkedHashMap() {
        return Maps.newLinkedHashMap();
    }

    public Map<String, Object> newHashMap(int size) {
        return Maps.newLinkedHashMapWithExpectedSize(size);
    }

}
