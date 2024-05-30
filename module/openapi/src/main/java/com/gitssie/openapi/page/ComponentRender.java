package com.gitssie.openapi.page;

import com.gitssie.openapi.models.layout.Component;
import com.gitssie.openapi.models.layout.PageLayout;
import com.gitssie.openapi.service.PageService;
import com.gitssie.openapi.service.Provider;
import com.gitssie.openapi.utils.TypeUtils;
import com.gitssie.openapi.rule.Rule;
import com.gitssie.openapi.xentity.XEntityCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.ebean.plugin.BeanType;
import io.ebeaninternal.server.deploy.BeanDescriptor;
import io.ebeaninternal.server.deploy.BeanProperty;
import io.ebeaninternal.server.el.ElPropertyDeploy;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Function;

public class ComponentRender {
    public static final String[] defaultProps = new String[]{"entityType", "status", "lockStatus", "owner", "createdBy", "updatedBy", "createdAt", "updatedAt"};

    public static final String APIKEY = "apiKey";
    public static final String NAME = "name";
    public static final String VALUE = "value";
    public static final String RULE = "rule";
    public static final String LAZY = "lazy";
    public static final String FORMAT = "format";
    public static final String PATH = "path";
    public static final String OP = "op";
    public static final String ASSOC = "assoc";
    public static final String MAPPEDBY = "mappedBy";
    public static final String FILTER = "filter";
    public static final String COLUMNS = "columns";
    public static final String OPTIONS = "options";

    public static final String TYPE = "type";

    public static final String FIELD = "field";
    public static final String RAW_SQL = "rawSql";

    public Component layout(PageService pageService, Provider provider, XEntityCache entity, Component component) {
        BeanType<?> desc = provider.desc(entity.getName());
        Map<String, List<LazyValue>> context = Maps.newHashMap();
        layout(context, provider, desc, component);
        if (ObjectUtils.isEmpty(context)) {
            return component;
        }
        List<String> fields = new LinkedList<>();
        Map<String, Object> fieldMap;
        FetchContext stack = new FetchContext(provider);
        for (Map.Entry<String, List<LazyValue>> entry : context.entrySet()) {
            for (LazyValue value : entry.getValue()) {
                fields.add(value.getName());
            }
            Map<String, Map<String, Object>> fieldsMap = pageService.getLayoutFields(entry.getKey(), fields);
            for (LazyValue value : entry.getValue()) {
                fieldMap = fieldsMap.get(value.getName());
                if (fieldMap == null) {
                    continue;
                }
                fieldMap.put(NAME, value.getOriginName());
                if (value.isEmpty()) {
                    value.setValue(fieldMap);
                } else {
                    value.apply(stack,fieldMap);
                }
            }
            fields.clear();
        }
        stack.close();
        return component;
    }

    private void layout(Map<String, List<LazyValue>> context, Provider provider, BeanType<?> desc, Component component) {
        if (ObjectUtils.isNotEmpty(component.getColumns())) {
            List<Object> columns = component.getColumns();
            String apiKey = TypeUtils.castToString(component.get(APIKEY));
            String name = TypeUtils.castToString(component.get(NAME));
            desc = assocDesc(provider, desc, apiKey, name);
            if (desc == null) {
                return;
            }
            for (int i = 0; i < columns.size(); i++) {
                Object column = columns.get(i);
                if (ObjectUtils.isEmpty(column)) {
                    continue;
                }
                if (column instanceof String) {
                    columns.set(i, layoutColumn(context, desc, (String) column));
                } else if (column instanceof Map<?, ?>) {
                    Map<String, ?> columnAsMap = (Map<String, ?>) column;

                    Object nameKey = columnAsMap.get(NAME);
                    if (ObjectUtils.isEmpty(nameKey) || !(nameKey instanceof String)) {
                        continue;
                    } else {
                        columns.set(i, layoutColumn(context, desc, (String) nameKey, columnAsMap));
                    }
                }
            }
        }

        if (ObjectUtils.isNotEmpty(component.getComponents())) {
            for (Component child : component.getComponents()) {
                layout(context, provider, desc, child);
            }
        }
    }

    private BeanType<?> assocDesc(Provider provider, BeanType<?> desc, String apiKey, String name) {
        if (StringUtils.equals(desc.name(), apiKey)) {
            return desc;
        }
        if (StringUtils.isNotEmpty(apiKey)) {
            return provider.desc(apiKey);
        } else if (StringUtils.isNotEmpty(name)) {
            BeanProperty property = (BeanProperty) desc.property(name);
            if (property == null) {
                return null;
            } else if (!property.isAssocProperty()) {
                return null;
            } else {
                return property.descriptor();
            }
        } else {
            return desc;
        }
    }

    private Object layoutColumn(Map<String, List<LazyValue>> context, BeanType<?> desc, String name) {
        return layoutColumn(context, desc, name, null);
    }

    private Object layoutColumn(Map<String, List<LazyValue>> context, BeanType<?> desc, String name, Map<String, ?> column) {
        int i = name.indexOf('.');
        if (i <= 0) {
            return column == null ? name : column;
        }
        BeanDescriptor bd = (BeanDescriptor) desc;
        ElPropertyDeploy elPropertyDeploy = bd.elPropertyDeploy(name);
        if (elPropertyDeploy == null) {
            return name;
        }
        //寻找最终的属性
        BeanProperty property = elPropertyDeploy.beanProperty();
        String apiKey = property.descriptor().name();
        String field = property.name();

        List<LazyValue> list = context.get(apiKey);
        if (list == null) {
            list = new LinkedList<>();
            context.put(apiKey, list);
        }
        LazyValue value = new LazyValue(field, name);
        if (ObjectUtils.isNotEmpty(column)) {
            value.setCallback((ctx,res) -> {
                if (res instanceof Map) {
                    Map map = (Map) res;
                    map.putAll(column);
                    return map;
                } else {
                    return res;
                }
            });
        }
        list.add(value);
        return value;
    }

    public Model parse(PageLayout page, Component component) {
        String apiKey = ObjectUtils.getIfNull(page.getApiKey(), () -> page.getEntity().getName());
        Model root = new Model(apiKey);
        root.setDataPermission(page.getDataPermission());
        parse(root, component, this::parseColumn);
        for (String defaultProp : defaultProps) {
            root.add(defaultProp, null);
        }
        return root;
    }

    private Field parseColumn(Map<String, ?> columnAsMap) {
        return null;
    }

    public static Field createField(String name, Map<String, ?> columnAsMap) {
        if (ObjectUtils.isEmpty(columnAsMap)) {
            return new Field(name);
        }
        Field field = new Field(
                name,
                TypeUtils.castToString(columnAsMap.get(OP)),
                columnAsMap.get(VALUE),
                Rule.instance(columnAsMap.get(RULE)),
                columnAsMap.get(LAZY),
                columnAsMap.get(FORMAT),
                TypeUtils.castToString(columnAsMap.get(PATH)),
                TypeUtils.castToString(columnAsMap.get(FIELD))
        );
        Object options = columnAsMap.get(OPTIONS);
        if (ObjectUtils.isNotEmpty(options)) {
            field.setOptions(options);
        }
        Object filter = columnAsMap.get(FILTER);
        if (ObjectUtils.isNotEmpty(filter)) {
            field.setFilter(filter);
        }
        return field;
    }

    public static ModelAssoc parseModelAssoc(Map<String, ?> columnAsMap) {
        if (ObjectUtils.isEmpty(columnAsMap)) {
            return null;
        }
        Object assocConfig = columnAsMap.get(ASSOC);
        if (ObjectUtils.isEmpty(assocConfig) || !(assocConfig instanceof Map)) {
            return null;
        }
        // assoc: { apiKey: "", type: "", name: "", mappedBy: "" },
        columnAsMap = (Map<String, ?>) assocConfig;
        ModelAssoc assoc = new ModelAssoc(
                TypeUtils.castToString(columnAsMap.get(APIKEY)),
                TypeUtils.castToString(columnAsMap.get(NAME)),
                StringUtils.defaultString(TypeUtils.castToString(columnAsMap.get(TYPE)), "one"),
                TypeUtils.castToString(columnAsMap.get(MAPPEDBY)),
                columnAsMap.get(FILTER)
        );
        return assoc;
    }

    /**
     * 解析JS配置的组件
     *
     * @param apiKey
     * @param component
     * @param parseColumn
     * @return
     */
    public static Model parse(String apiKey, Component component, Function<Map<String, ?>, Field> parseColumn) {
        Model root = new Model(apiKey, component); //关联组件的目的是为了其它的地方能拿到配置项
        parse(root, component, parseColumn);
        return root;
    }

    private static void parse(Model model, Component component, Function<Map<String, ?>, Field> parseColumn) {
        if (ObjectUtils.isNotEmpty(component.getColumns())) {
            for (Object column : component.getColumns()) {
                if (ObjectUtils.isEmpty(column)) {
                    continue;
                }
                if (column instanceof String) {
                    model.add((String) column, Collections.EMPTY_MAP);
                } else if (column instanceof Map) {
                    Map<String, Object> columnAsMap = (Map<String, Object>) column;
                    String fieldNme = TypeUtils.castToString(columnAsMap.get(NAME));
                    Object childrenColumns = columnAsMap.get(COLUMNS);
                    if (childrenColumns != null && childrenColumns instanceof List) {
                        Component cp = new Component(columnAsMap);
                        cp.setColumns((List) childrenColumns);
                        if (component.getComponents() == null) {
                            component.setComponents(Lists.newLinkedList());
                        }
                        component.getComponents().add(cp);
                    } else if (StringUtils.isNotEmpty(fieldNme)) {//这里主要是为了兼容页面的显示组件
                        Field field = parseColumn == null ? null : parseColumn.apply(columnAsMap);
                        if (field != null) {
                            model.add(field);
                        } else if (columnAsMap.containsKey(OP)) { //兼容使用json配置视图的查询columns不是实体属性
                            continue;
                        } else {
                            model.add(fieldNme, columnAsMap);
                        }
                    }
                }
            }
        }
        if (ObjectUtils.isNotEmpty(component.getComponents())) {
            for (Component child : component.getComponents()) {
                String apiKey = TypeUtils.castToString(child.get(APIKEY));
                String assocName = TypeUtils.castToString(child.get(NAME));
                Model assoc = model.addChild(apiKey, assocName, child);
                parse(assoc, child, parseColumn);
            }
        }
    }
}
