package com.gitssie.openapi.models.auth;

import io.vavr.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.function.Supplier;

public class SecurityUser implements UserDetails, InternalUser {
    private static final long serialVersionUID = 7868865888146329914L;
    private InternalUser user;
    private Collection<? extends GrantedAuthority> authorities;
    private transient Supplier<Collection<? extends GrantedAuthority>> authoritiesSupplier;

    public SecurityUser(InternalUser user, Supplier<Collection<? extends GrantedAuthority>> authoritiesSupplier) {
        this.user = user;
        this.authoritiesSupplier = authoritiesSupplier;
    }

    public SecurityUser(InternalUser user, Collection<? extends GrantedAuthority> authorities) {
        this.user = user;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (authoritiesSupplier != null) {
            authorities = authoritiesSupplier.get();
            authoritiesSupplier = null;
        }
        return authorities;
    }

    public static boolean contains(Authentication authentication, String authority) {
        for (GrantedAuthority item : authentication.getAuthorities()) {
            if (item.getAuthority().equals(authority)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Long getId() {
        return user.getId();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }


    @Override
    public Long getTenantId() {
        return user.getTenantId();
    }

    public <T extends InternalUser> T getUser() {
        if (user instanceof Value) {
            return (T)((Value<?>) user).get();
        }
        return (T) user;
    }
}
