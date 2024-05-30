package com.gitssie.openapi.page;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gitssie.openapi.data.Code;
import com.gitssie.openapi.service.Provider;
import io.ebean.bean.EntityBean;
import io.ebean.plugin.BeanType;
import io.ebeaninternal.server.deploy.BeanDescriptor;
import io.ebeaninternal.server.deploy.BeanPropertyAssoc;
import io.ebeaninternal.server.deploy.BeanPropertyAssocOne;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;

public class Create extends Graph<Create> {
    private final NeedContext context;

    public Create(Model model, BeanType<?> desc, Provider provider, AssocType assocType) {
        super(model, desc, provider, assocType);
        context = new NeedContext(model);
    }

    public NeedContext context() {
        return context;
    }

    /**
     * 新增数据
     *
     * @param source
     * @param mc
     * @return
     */
    public <T extends EntityBean> Either<Code, T> create(ObjectNode source, ModelNodeConversion mc) {
        try (context) {
            T bean = createBean();
            Either<Errors, ?> res = mc.copy(context, provider, desc, model, source, bean);
            if (res.isLeft()) {
                return Either.left(Code.INVALID_ARGUMENT.withErrors(Option.of(res.getLeft())));
            }
            context.needInsert.add(bean);
            //@TODO fetch next
            return Either.right(bean);
        } catch (Exception e) {
            mc.LOGGER.error("create object error", e);
            return Either.left(Code.INVALID_ARGUMENT.withMessage(e.getMessage()));
        }
    }

    private <T> T createBean() {
        T bean = (T) desc.createBean();
        return bean;
    }

    protected void addJoin(String name, Model model, BeanType<?> childDesc, Provider provider) {
        addJoin(name, model, childDesc, provider, (type) -> model.create(childDesc, provider, type));
    }

    public void addValue(Model model, BeanPropertyAssoc property, Provider provider) {
        BeanType<?> childDesc = provider.desc(property.targetType());
        Create node = model.create(childDesc, provider, property.isAssocMany() ? AssocType.MANY : AssocType.ONE); //@TODO one to one or many to one
        addNode(node);
    }
}
