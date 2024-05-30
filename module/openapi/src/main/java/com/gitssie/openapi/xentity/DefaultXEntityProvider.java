package com.gitssie.openapi.xentity;

import io.ebean.bean.XEntityProvider;
import io.ebean.config.CurrentTenantProvider;
import io.ebeaninternal.server.deploy.parse.tenant.XEntityFinder;

public class DefaultXEntityProvider implements XEntityProvider {
    private final XEntityFinder finder;

    public DefaultXEntityProvider(XEntityFinder finder) {
        this.finder = finder;
    }

    @Override
    public XEntityFinder create() {
        return finder;
    }

    @Override
    public CurrentTenantProvider tenantProvider() {
        return () -> 1L;
    }
}
