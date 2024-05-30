package com.gitssie.openapi.page;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gitssie.openapi.data.Code;
import com.gitssie.openapi.service.Provider;
import io.ebean.Database;
import io.ebean.bean.EntityBean;
import io.ebean.plugin.BeanType;
import io.ebean.plugin.Property;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.springframework.validation.Errors;

public class Save extends Graph<Save> {
    private final NeedContext context;
    private final boolean updateOnly;

    public Save(Model model, BeanType<?> desc, Provider provider, AssocType assocType) {
        this(model, desc, provider, assocType, false);
    }

    public Save(Model model, BeanType<?> desc, Provider provider, AssocType assocType, boolean updateOnly) {
        super(model, desc, provider, assocType);
        this.context = new NeedContext(model);
        this.updateOnly = updateOnly;
    }

    public Either<Code, EntityBean> save(ObjectNode source, ModelNodeConversion mc) {
        return save(context, source, mc);
    }

    public Either<Code, EntityBean> save(NeedContext context, ObjectNode source, ModelNodeConversion mc) {
        Either<Code, EntityBean> beanE = fetchBean(context, source);
        return beanE.flatMap(bean -> {
            try (context) {
                Either<Errors, ?> res = mc.copy(context, provider, desc, model, source, bean);
                if (res.isLeft()) {
                    return Either.left(Code.INVALID_ARGUMENT.withErrors(Option.of(res.getLeft())));
                }
                return Either.right(bean);
            } catch (Exception e) {
                mc.LOGGER.error("save object error", e);
                return Either.left(Code.INVALID_ARGUMENT.withMessage(e.getMessage()));
            }
        });
    }

    private Either<Code, EntityBean> fetchBean(NeedContext context, ObjectNode source) {
        Object id = provider.convertId(desc, source);
        Property codeProperty = provider.codeProperty(desc);
        Object code = null;
        if (id == null && codeProperty != null) {
            code = provider.convertCode(codeProperty, source);
        }
        if (id != null) { //case 1. 指定ID数据更新
            EntityBean bean = provider.findBean(desc.type(), id);
            if (bean == null) {
                return Either.left(Code.NOT_FOUND.withMessage("not found by id " + id));
            } else {
                context.needUpdate.add(bean);
                return Either.right(bean);
            }
        } else if (code != null) {//case 2.根据code决定是更新还是修改
            EntityBean bean = provider.findBeanByCode(desc.type(), codeProperty, code);
            if (bean == null && updateOnly) {
                return Either.left(Code.NOT_FOUND.withMessage("not found by code " + code));
            } else if (bean != null) {
                context.needUpdate.add(bean);
                return Either.right(bean);
            }
        } else if (updateOnly) {
            return Either.left(Code.NOT_FOUND);
        }
        EntityBean bean = (EntityBean) desc.createBean();
        context.needInsert.add(bean);
        return Either.right(bean);
    }

    /**
     * 把创建结果应用到DB上
     *
     * @param db
     */
    public Option<Code> apply(Database db) {
        return context.apply(db);
    }
}
