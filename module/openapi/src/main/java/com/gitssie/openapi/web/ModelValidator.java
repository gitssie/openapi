package com.gitssie.openapi.web;

import com.gitssie.openapi.page.Model;
import com.gitssie.openapi.web.query.AbstractQuery;
import io.vavr.control.Option;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.List;
import java.util.Map;

/**
 * @author: Awesome
 * @create: 2024-05-20 14:53
 */
public class ModelValidator implements Validator {

    private Model model;

    public ModelValidator(Model model) {
        this.model = model;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return AbstractQuery.class.isAssignableFrom(clazz)
                || List.class.isAssignableFrom(clazz)
                || Map.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors result) {
        Option<Errors> errors = Option.none();
        if (target instanceof AbstractQuery) {
            AbstractQuery query = (AbstractQuery) target;
            errors = query.validate(model);
        } else if (target instanceof Map) {
            errors = model.validate((Map) target);
        } else if (target instanceof List) {
            errors = model.validate((List) target);
        }
        if (errors.isDefined()) {
            result.addAllErrors(errors.get());
        }
    }
}
