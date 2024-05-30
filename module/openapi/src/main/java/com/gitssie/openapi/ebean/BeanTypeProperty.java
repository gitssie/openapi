package com.gitssie.openapi.ebean;

import io.ebean.plugin.BeanType;
import io.ebean.plugin.Property;
import io.ebeaninternal.server.deploy.BeanDescriptor;
import io.ebeaninternal.server.deploy.BeanProperty;
import io.ebeaninternal.server.el.ElPropertyDeploy;

import java.util.function.Function;

public class BeanTypeProperty implements Function<String, Property> {
    private BeanType<?> desc;

    public BeanTypeProperty(BeanType<?> desc) {
        this.desc = desc;
    }

    @Override
    public Property apply(String name) {
        int i = name.indexOf('.');
        if (i <= 0) {
            return desc.property(name);
        }
        BeanDescriptor bd = (BeanDescriptor) desc;
        ElPropertyDeploy elPropertyDeploy = bd.elPropertyDeploy(name);
        if (elPropertyDeploy == null) {
            return null;
        }
        //寻找最终的属性
        BeanProperty property = elPropertyDeploy.beanProperty();
        return new SimpleProperty(elPropertyDeploy.elName(), property.type());
    }
}
