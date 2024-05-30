package com.gitssie.openapi.page;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gitssie.openapi.data.Code;
import io.ebean.Expression;
import io.ebean.bean.EntityBean;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.springframework.validation.Errors;

public class Patch {
    private final Model model;
    private final Fetch fetch;
    private final NeedContext context;

    public Patch(Model model, Fetch fetch) {
        this.model = model;
        this.fetch = fetch;
        this.context = new NeedContext(model, true);
    }

    public void and(Expression where) {
        fetch.and(where);
    }

    public Object fetch() {
        return fetch.fetch();
    }

    public NeedContext context() {
        return context;
    }

    /**
     * 修改数据
     *
     * @param bean
     * @param body
     * @param mc
     * @return
     */
    public Either<Code, Object> patch(Object bean, ObjectNode body, ModelNodeConversion mc) {
        try (context) {
            Either<Errors, ?> res = mc.copy(context, fetch.provider, fetch.desc, model, body, (EntityBean) bean);
            if (res.isLeft()) {
                return Either.left(Code.INVALID_ARGUMENT.withErrors(Option.of(res.getLeft())));
            }
            context.needUpdate.add((EntityBean) bean);
            return Either.right(bean);
        } catch (Exception e) {
            mc.LOGGER.error("patch object error", e);
            return Either.left(Code.INVALID_ARGUMENT.withMessage(e.getMessage()));
        }
    }
}
