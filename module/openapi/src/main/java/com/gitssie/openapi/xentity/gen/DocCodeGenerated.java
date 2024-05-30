package com.gitssie.openapi.xentity.gen;

import io.ebean.annotation.DocCode;
import io.ebean.bean.EntityBean;
import io.ebeaninternal.server.deploy.BeanProperty;

public class DocCodeGenerated extends CodeGenerated {
    @Override
    public Object getInsertValue(BeanProperty prop, EntityBean bean, long now) {
        Object value = prop.value(bean);
        return value;
    }

    @Override
    public Object getUpdateValue(BeanProperty prop, EntityBean bean, long now) {
        return null;
    }

    @Override
    public boolean includeInUpdate() {
        return false;
    }

    @Override
    public boolean includeInAllUpdates() {
        return false;
    }

    @Override
    public boolean includeInInsert() {
        return true;
    }

    @Override
    public boolean isDDLNotNullable() {
        return false;
    }

    @Override
    public Object nextValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        return DocCode.class.getSimpleName();
    }
}
