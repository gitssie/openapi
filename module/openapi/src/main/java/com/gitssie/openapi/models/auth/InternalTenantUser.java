package com.gitssie.openapi.models.auth;

class InternalTenantUser implements InternalUser {
    private Long tenantId;

    public InternalTenantUser(Long tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public Long getId() {
        return null;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return null;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public Long getTenantId() {
        return tenantId;
    }
}
