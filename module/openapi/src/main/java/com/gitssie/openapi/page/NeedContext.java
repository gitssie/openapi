package com.gitssie.openapi.page;

import com.gitssie.openapi.data.Code;
import io.ebean.Database;
import io.ebean.bean.EntityBean;
import io.vavr.Function2;
import io.vavr.Lazy;
import io.vavr.control.Option;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.mozilla.javascript.Context;
import org.springframework.data.relational.core.sql.In;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class NeedContext implements Function<Database, Option<Code>>, Closeable {
    protected final boolean isUpdate;
    protected final List<EntityBean> needInsert = new ArrayList<>();
    protected final List<EntityBean> needUpdate = new ArrayList<>();
    protected final List<EntityBean> needDelete = new ArrayList<>();
    protected final Model model;
    protected final Lazy<Context> context;

    public NeedContext(Model model, boolean isUpdate) {
        this.model = model;
        this.isUpdate = isUpdate;
        this.context = Lazy.of(() -> Context.enter());
    }

    public NeedContext(Model model) {
        this(model, false);
    }

    public NeedContext() {
        this(null);
    }


    private Option<Code> apply(List<Function2<NeedContext, EntityBean, Option<Code>>> callList, EntityBean bean) {
        if (ObjectUtils.isEmpty(callList)) {
            return Option.none();
        }
        for (Function2<NeedContext, EntityBean, Option<Code>> call : callList) {
            Option<Code> code = call.apply(this, bean);
            if (code != null && code.isDefined()) {
                return code;
            }
        }
        return Option.none();
    }


    private Option<Code> preInsert(EntityBean[] beans) {
        if (model == null || ObjectUtils.isEmpty(model.preInsert)) {
            return Option.none();
        }
        Option<Code> code = Option.none();
        for (EntityBean bean : beans) {
            code = apply(model.preInsert, bean);
            if (code.isDefined()) {
                return code;
            }
        }
        return code;
    }

    private Option<Code> preUpdate(EntityBean[] beans) {
        if (model == null || ObjectUtils.isEmpty(model.preUpdate)) {
            return Option.none();
        }
        Option<Code> code = Option.none();
        for (EntityBean bean : beans) {
            code = apply(model.preUpdate, bean);
            if (code.isDefined()) {
                return code;
            }
        }
        return code;
    }

    private Option<Code> preDelete(EntityBean[] beans) {
        if (model == null || ObjectUtils.isEmpty(model.preDelete)) {
            return Option.none();
        }
        Option<Code> code = Option.none();
        for (EntityBean bean : beans) {
            code = apply(model.preDelete, bean);
            if (code.isDefined()) {
                return code;
            }
        }
        return code;
    }

    private Option<Code> preApply() {
        if (model == null || ObjectUtils.isEmpty(model.preApply)) {
            return Option.none();
        }
        Option<Code> code = Option.none();
        for (Function<NeedContext, Option<Code>> call : model.preApply) {
            code = call.apply(this);
            if (code != null && code.isDefined()) {
                return code;
            }
        }
        return code;
    }

    private Option<Code> postApply() {
        if (model == null || ObjectUtils.isEmpty(model.postApply)) {
            return Option.none();
        }
        Option<Code> code = Option.none();
        for (Function<NeedContext, Option<Code>> call : model.postApply) {
            code = call.apply(this);
            if (code != null && code.isDefined()) {
                return code;
            }
        }
        return code;
    }

    public void addInsert(Object bean) {
        needInsert.add((EntityBean) bean);
    }

    public void addUpdate(Object bean) {
        needUpdate.add((EntityBean) bean);
    }

    public void addDelete(Object bean) {
        needDelete.add((EntityBean) bean);
    }

    @Override
    public void close()  {
        if (context.isEvaluated()) {
            IOUtils.closeQuietly(context.get());
        }
    }

    @Override
    public Option<Code> apply(Database database) {
        Option<Code> code = preApply();
        if (code != null && code.isDefined()) {
            return code;
        }
        EntityBean[] arr = new EntityBean[0];
        code = preInsert(needInsert.toArray(arr));
        if (code != null && code.isDefined()) {
            return code;
        }
        database.insertAll(needInsert); //insert

        code = preUpdate(needUpdate.toArray(arr));
        if (code != null && code.isDefined()) {
            return code;
        }
        database.updateAll(needUpdate); //update

        code = preDelete(needDelete.toArray(arr));
        if (code != null && code.isDefined()) {
            return code;
        }
        database.deleteAll(needDelete); //delete
        if (code != null && code.isDefined()) {
            return code;
        }
        code = postApply();
        //after callback
        return code != null ? code : Option.none();
    }
}
