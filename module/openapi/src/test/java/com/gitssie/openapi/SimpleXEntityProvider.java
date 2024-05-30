package com.gitssie.openapi;

import io.ebean.bean.XEntityProvider;
import io.ebeaninternal.server.deploy.parse.tenant.XEntity;
import io.ebeaninternal.server.deploy.parse.tenant.XEntityFinder;

public class SimpleXEntityProvider implements XEntityProvider {
    @Override
    public XEntityFinder create() {
        return new XEntityFinder() {

            @Override
            public XEntity getEntity(Class<?> beanClass) {
                return new XEntity(beanClass);
            }

            @Override
            public boolean isChanged(Class<?> entityClass) {
                return false;
            }
        };
    }
}
