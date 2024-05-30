package com.gitssie.openapi.page;

import com.gitssie.openapi.data.Code;
import com.gitssie.openapi.rule.RuleProxyMap;
import com.gitssie.openapi.models.layout.Component;
import com.gitssie.openapi.models.user.DataPermissionEnum;
import com.gitssie.openapi.service.Provider;
import com.gitssie.openapi.rule.Rules;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.ebean.Expression;
import io.ebean.FetchGroup;
import io.ebean.FetchGroupBuilder;
import io.ebean.bean.EntityBean;
import io.ebean.plugin.BeanType;
import io.ebean.plugin.Property;
import io.ebeaninternal.server.deploy.BeanPropertyAssoc;
import io.vavr.Function2;
import io.vavr.Value;
import io.vavr.control.Option;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.Context;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;

import java.util.*;
import java.util.function.Function;

public class Model {
    protected static final List<Field> STAR_FIELD = Lists.newArrayList(new Field("*"));
    protected String name = "";
    protected String apiKey;
    protected List<Field> fields;
    protected Map<String, Model> assoc;
    protected boolean isLazyFetch = false;
    protected boolean includeAllLoadedProps;
    protected DataPermissionEnum dataPermission; //数据权限
    protected List<Function2<NeedContext, EntityBean, Option<Code>>> preInsert;
    protected List<Function2<NeedContext, EntityBean, Option<Code>>> preUpdate;
    protected List<Function2<NeedContext, EntityBean, Option<Code>>> preDelete;
    protected List<Function<NeedContext, Option<Code>>> preApply;
    protected List<Function<NeedContext, Option<Code>>> postApply;

    protected Component component; //关联的组件
    protected ModelAssoc assocAnnotation;

    public Model(String apiKey, boolean includeAllLoadedProps) {
        this.apiKey = apiKey;
        this.includeAllLoadedProps = includeAllLoadedProps;
    }

    public Model(String apiKey, List<Field> fields) {
        this.apiKey = apiKey;
        this.fields = fields;
    }

    public Model(String apiKey) {
        this(apiKey, (Component) null);
    }

    public Model(String apiKey, Component component) {
        this.apiKey = apiKey;
        this.fields = Lists.newLinkedList();
        this.assoc = Maps.newLinkedHashMap();
        this.component = component;
    }

    public Model(String apiKey, String... fields) {
        this(apiKey);
        for (String field : fields) {
            add(field, null);
        }
    }

    public void add(String assocName, String fieldNme, Map<String, Object> columnAsMap) {
        if (StringUtils.isEmpty(assocName)) {
            this.add(fieldNme, columnAsMap);
            return;
        }
        Model model = assoc.get(assocName);
        if (model == null) {
            model = new Model(null);
            model.name = assocName;
            assoc.put(assocName, model);
            Field field = createField(assocName, Collections.EMPTY_MAP); //这里的属性是否需要加验证
            fields.add(field);
            model.isLazyFetch = Rules.isFunction(field.getLazy());
        }
        model.add(fieldNme, columnAsMap);
    }

    public void add(Field field) {
        fields.add(field);
    }

    private Field createField(String name, Map<String, ?> columnAsMap) {
        return ComponentRender.createField(name, columnAsMap);
    }

    private ModelAssoc parseModelAssoc(Map<String, ?> columnAsMap) {
        return ComponentRender.parseModelAssoc(columnAsMap);
    }

    public void add(String name, Map<String, Object> columnAsMap) {
        int i = name.indexOf('.');
        if (i > 0) {
            String childName = name.substring(0, i);
            name = name.substring(i + 1);
            add(childName, name, columnAsMap);
        } else if (StringUtils.equals("*", name)) {
            includeAllLoadedProps = true;
        } else {
            Field field = createField(name, columnAsMap);
            fields.add(field);
        }
    }


    protected Model addChild(String apiKey, String assocName, Component child) {
        if (StringUtils.isEmpty(assocName)) {
            return this;
        }
        Model model = assoc.get(assocName);
        if (model == null) {
            model = new Model(apiKey, child); //关联组件的目的是为了其它的地方能拿到配置项
            model.name = assocName;
            model.assocAnnotation = parseModelAssoc(child.getOthers()); //模型关联关系信息
            if (model.assocAnnotation != null) {
                model.apiKey = model.assocAnnotation.apiKey;
            }
            assoc.put(assocName, model);
            Field field = createField(assocName, child.getOthers());
            fields.add(field);
            model.isLazyFetch = Rules.isFunction(field.getLazy());
        } else {
            model.merge(this, apiKey, assocName, child);
        }
        return model;
    }

    protected void merge(Model parent, String apiKey, String assocName, Component child) {
        if (StringUtils.isEmpty(this.apiKey)) {
            this.apiKey = apiKey;
        }
        if (component == null) {
            component = child;
        }
        if (assocAnnotation == null) {
            assocAnnotation = parseModelAssoc(child.getOthers()); //模型关联关系信息
        }
        Field field = createField(assocName, child.getOthers());
        Field origin = null;
        for (Field f : parent.fields) {
            if (StringUtils.equals(f.getName(), f.getName())) {
                origin = f;
                break;
            }
        }
        if (origin != null) {
            origin.setFilter(field.getFilter());
            origin.setOptions(field.getOptions());
            origin.setOp(field.getOp());
            origin.setFormat(field.getFormat());
            origin.setRule(field.getRule());
            origin.setValue(field.getValue());
        } else {
            parent.fields.add(field);
        }

    }

    public String getName() {
        return name;
    }

    public List<Field> getFields() {
        return fields;
    }

    public boolean isEmpty() {
        return ObjectUtils.isEmpty(fields) && !includeAllLoadedProps;
    }

    public List<Field> getQueryFields() {
        if (ObjectUtils.isNotEmpty(fields)) {
            return fields;
        } else if (includeAllLoadedProps) {
            return STAR_FIELD;
        } else {
            return fields;
        }
    }

    private String getSelectProperties(StringBuilder buf) {
        buf.setLength(0);
        if (ObjectUtils.isEmpty(fields)) {
            return null; //如果没有配置，则查询全部
        } else if (includeAllLoadedProps) {
            buf.append("*,");
        }
        //过滤掉重复的属性
        Set<String> fields = new TreeSet<>();
        for (Field name : this.fields) {
            if (name.getLazy() != null || StringUtils.isEmpty(name.getName())) {
                continue;
            }
            fields.add(name.getName());
        }
        //关联mappedBy属性查询
        if (this.assocAnnotation != null) {
            if (StringUtils.isNotEmpty(this.assocAnnotation.mappedBy)) {
                fields.add(this.assocAnnotation.mappedBy);
            }
        }
        //查询出下一阶段需要分步Join关联的属性
        if (ObjectUtils.isNotEmpty(this.assoc)) {
            for (Model model : this.assoc.values()) {
                if (model.assocAnnotation != null && StringUtils.isNotEmpty(model.assocAnnotation.name)) {
                    fields.add(model.assocAnnotation.name);
                }
            }
        }

        int i = 0;
        for (String name : fields) {
            i++;
            buf.append(name).append(",");
        }
        if (i > 0) {
            buf.setLength(buf.length() - 1);
        }
        return buf.toString();
    }

    public <T> Save saveOne(BeanType<T> desc, Provider provider) {
        return new Save(this, desc, provider, AssocType.ONE);
    }

    public <T> Save saveOne(BeanType<T> desc, Provider provider, boolean updateOnly) {
        return new Save(this, desc, provider, AssocType.ONE, updateOnly);
    }

    public <T> Create createOne(BeanType<T> desc, Provider provider) {
        return create(desc, provider, AssocType.ONE);
    }

    public <T> Create create(BeanType<T> desc, Provider provider, AssocType assocType) {
        Create create = new Create(this, desc, provider, assocType);
        List<Map.Entry<String, Model>> splitFetch = new LinkedList<>();
        for (Map.Entry<String, Model> entry : assoc.entrySet()) {
            Property property = desc.property(entry.getKey());
            if (property != null) {
                //普通的属性
            } else {
                splitFetch.add(entry);
            }
        }
        //其它被动关联的属性
        for (Map.Entry<String, Model> entry : splitFetch) {
            Model model = entry.getValue();
            BeanType<?> childDesc = provider.desc(model.apiKey);
            create.addJoin(entry.getKey(), model, childDesc, provider);
        }
        return create;
    }

    public <T> Patch patchOne(BeanType<T> desc, Provider provider) {
        return patch(desc, provider, AssocType.ONE);
    }

    public <T> Patch patchList(BeanType<T> desc, Provider provider) {
        return patch(desc, provider, AssocType.MANY);
    }

    public <T> Patch patch(BeanType<T> desc, Provider provider, AssocType fetchType) {
        Fetch fetch = this.patchLazy(desc, provider, fetchType);
        return new Patch(this, fetch);
    }

    public <T> Fetch fetchOne(BeanType<T> desc, Provider provider) {
        return fetch(desc, provider, AssocType.ONE);
    }

    public <T> Fetch fetchList(BeanType<T> desc, Provider provider) {
        return fetch(desc, provider, AssocType.MANY);
    }

    /**
     * 加载数据级关联数据, 根据model的配置字段进行按需加载
     *
     * @param desc
     * @param provider
     * @param fetchType
     * @param <T>
     * @return
     */
    public <T> Fetch fetch(BeanType<T> desc, Provider provider, AssocType fetchType) {
        StringBuilder buf = new StringBuilder();
        String select = getSelectProperties(buf);
        FetchGroupBuilder<?> query = FetchGroup.of(desc.type());
        query.select(select);
        Fetch fetch;
        if (assoc != null) {
            List<Map.Entry<String, Model>> splitFetch = new LinkedList<>();
            for (Map.Entry<String, Model> entry : assoc.entrySet()) {
                Property property = desc.property(entry.getKey());
                if (property != null && property instanceof BeanPropertyAssoc) {
                    BeanPropertyAssoc assocProp = (BeanPropertyAssoc) property;
                    //@TODO 是否需要进行关联join查询
                    select = entry.getValue().getSelectProperties(buf);
                    if (assocProp.isAssocMany()) {
                        query.fetchLazy(assocProp.name(), select);
                    } else if (StringUtils.isNotEmpty(assocProp.mappedBy())) {
                        query.fetch(assocProp.name(), select);
                    } else {
                        query.fetchLazy(assocProp.name(), select);
                    }
                } else {
                    splitFetch.add(entry);
                }
            }
            fetch = new Fetch(this, provider, desc, query.build(), fetchType);
            for (Map.Entry<String, Model> entry : splitFetch) {
                Model model = entry.getValue();
                if (model.isLazyFetch) {
                    continue;
                }
                if (StringUtils.isEmpty(model.apiKey)) {
                    throw new IllegalArgumentException("split join fetch property " + entry.getKey() + ",the apikey is empty");
                }
                BeanType<?> childDesc = provider.desc(model.apiKey);
                fetch.addJoin(entry.getKey(), model, childDesc, provider);
            }
        } else {
            fetch = new Fetch(this, provider, desc, query.build(), fetchType);
        }
        return fetch;
    }


    <T> Fetch patchLazy(BeanType<T> desc, Provider provider, AssocType fetchType) {
        FetchGroupBuilder<?> query = FetchGroup.of(desc.type());
        Fetch fetch = new Fetch(this, provider, desc, query.build(), fetchType);
        return fetch;
    }

    public boolean isIncludeAllLoadedProps() {
        return includeAllLoadedProps;
    }

    public void addPreInsert(Function2<NeedContext, EntityBean, Option<Code>> call) {
        if (preInsert == null) {
            preInsert = Lists.newLinkedList();
        }
        preInsert.add(call);
    }

    public void addPreUpdate(Function2<NeedContext, EntityBean, Option<Code>> call) {
        if (preUpdate == null) {
            preUpdate = Lists.newLinkedList();
        }
        preUpdate.add(call);
    }

    public void addPreDelete(Function2<NeedContext, EntityBean, Option<Code>> call) {
        if (preDelete == null) {
            preDelete = Lists.newLinkedList();
        }
        preDelete.add(call);
    }


    public void addPreApply(Function<NeedContext, Option<Code>> call) {
        if (preApply == null) {
            preApply = Lists.newLinkedList();
        }
        preApply.add(call);
    }

    public void addPostApply(Function<NeedContext, Option<Code>> call) {
        if (postApply == null) {
            postApply = Lists.newLinkedList();
        }
        postApply.add(call);
    }

    public Expression getDataPermissionWhere(Function<DataPermissionEnum, Expression> fetchWhere) {
        return fetchWhere.apply(dataPermission);
    }

    public ModelAssoc getAssocAnnotation() {
        return assocAnnotation;
    }

    public void setDataPermission(DataPermissionEnum dataPermission) {
        this.dataPermission = dataPermission;
    }

    public Component getComponent() {
        return component;
    }


    protected Object getFormat() {
        return component.get(ComponentRender.FORMAT);
    }

    /**
     * 根据model配置对实体进行参数验证
     *
     * @param body
     */
    public Option<Errors> validate(Map<String, Object> body) {
        MapBindingResult errors = new MapBindingResult(body, "");
        Rules.toValue(context -> validate(context, body, errors, name));
        if (errors.hasErrors()) {
            return Option.of(errors);
        } else {
            return Option.none();
        }
    }

    /**
     * 根据model配置对实体数组进行参数验证
     *
     * @param body
     * @return
     */
    public Option<Errors> validate(List<?> body) {
        MapBindingResult errors = new MapBindingResult(Collections.EMPTY_MAP, "");
        Rules.toValue(context -> validate(context, body, errors, name));
        if (errors.hasErrors()) {
            return Option.of(errors);
        } else {
            return Option.none();
        }
    }

    private MapBindingResult validate(Value<Context> context, Map<String, Object> body, MapBindingResult errors, String objectName) {
        if (ObjectUtils.isEmpty(fields)) {
            return errors;
        }
        Map<String, Object> scope = new RuleProxyMap(body);
        for (Field field : fields) {
            String name = field.getName();
            if (name == null) {
                continue;
            }
            Object value = body.get(name);
            //设置默认值
            if (field.getValue() != null && value == null) {
                value = field.getValue();
                body.put(name, value);
            }
            //对输入属性进行验证
            FieldError error = field.validate(context, value, objectName, name, scope);
            if (error != null) {
                errors.addError(error);
            }
        }
        if (errors.hasErrors()) {
            return errors;
        }
        //对子属性、关联属性进行验证
        if (ObjectUtils.isEmpty(assoc)) {
            return errors;
        }
        for (Map.Entry<String, Model> entry : assoc.entrySet()) {
            String key = entry.getKey();
            Model model = entry.getValue();
            Object value = body.get(key);
            if (ObjectUtils.isEmpty(value)) {
                continue;
            }
            String assocName = StringUtils.isEmpty(objectName) ? key : StringUtils.joinWith(".", objectName, key);
            if (value instanceof Map) {
                model.validate(context, (Map) value, errors, assocName);
            } else if (value instanceof List) {
                model.validate(context, (List) value, errors, assocName);
            } else {
                throw new IllegalArgumentException("invalid assoc model value,the type:" + value.getClass());
            }
            if (errors.hasErrors()) {
                return errors;
            }
        }
        return errors;
    }

    private MapBindingResult validate(Value<Context> context, List<?> body, MapBindingResult errors, String objectName) {
        if (ObjectUtils.isEmpty(body)) {
            return errors;
        }
        for (Object value : body) {
            if (value instanceof Map) {
                validate(context, (Map) value, errors, objectName);
            } else {
                throw new IllegalArgumentException("invalid assoc model value in array,the type:" + value.getClass());
            }
            if (errors.hasErrors()) {
                return errors;
            }
        }
        return errors;
    }
}
