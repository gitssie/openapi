package com.gitssie.openapi.auth;

import com.gitssie.openapi.models.auth.InternalUser;
import com.gitssie.openapi.models.auth.RoleGrantedAuthority;
import com.gitssie.openapi.models.auth.RoleName;
import com.gitssie.openapi.models.auth.VerbAttribute;
import io.vavr.control.Option;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.access.vote.RoleVoter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

import java.io.Serializable;
import java.util.Collection;

import static org.springframework.security.access.AccessDecisionVoter.ACCESS_GRANTED;

@Component
public class RolePermissionEvaluator implements PermissionEvaluator {
    private RoleVoter roleAdminVoter;
    private Collection<ConfigAttribute> roleRoot;
    private Collection<ConfigAttribute> roleAdmin;
    private Collection<ConfigAttribute> roleTenant;

    public RolePermissionEvaluator() {
        this.roleAdminVoter = new RoleVoter();
        this.roleRoot = SecurityConfig.createList(RoleName.ROLE_ROOT);
        this.roleAdmin = SecurityConfig.createList(RoleName.ROLE_ROOT, RoleName.ROLE_ADMIN);
        this.roleTenant = SecurityConfig.createList(RoleName.ROLE_ROOT, RoleName.ROLE_ADMIN, RoleName.ROLE_TENANT_ADMIN);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object target, Object permission) {
        return hasPermission(authentication, new VerbAttribute((String) target, (String) permission));
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return false;
    }

    public boolean isTenantAdmin(Authentication authentication) {
        int vote = roleAdminVoter.vote(authentication, null, roleTenant);
        if (vote == ACCESS_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return isAdmin(authentication);
    }

    public boolean isAdmin(Authentication authentication) {
        int vote = roleAdminVoter.vote(authentication, null, roleAdmin);
        if (vote == ACCESS_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isRoot() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return isRoot(authentication);
    }

    public boolean isRoot(Authentication authentication) {
        int vote = roleAdminVoter.vote(authentication, null, roleRoot);
        if (vote == ACCESS_GRANTED) {
            return true;
        } else {
            return false;
        }
    }


    public boolean hasPermission(Authentication authentication, VerbAttribute attribute) {
        if (authentication == null) {
            return false;
        }
        //判断是否是管理员,如果是管理员则ALL PASS
        if (isTenantAdmin(authentication)) {
            return true;
        }
//        不是管理员则按分配的角色权限进行判断
        Collection<? extends GrantedAuthority> authorities = extractAuthorities(authentication);
        for (GrantedAuthority authority : authorities) {
            if (!(authority instanceof RoleGrantedAuthority)) {
                if (attribute.equals(authority.getAuthority())) {
                    return true;
                } else {
                    continue;
                }
            }
            RoleGrantedAuthority roleGrantedAuthority = (RoleGrantedAuthority) authority;
            if (roleGrantedAuthority.match(attribute)) {
                //通过请求上下文进行传递最佳匹配的角色ID
                setRoleContext(roleGrantedAuthority);
                return true;
            }
        }
        return false;
    }

    private void setRoleContext(RoleGrantedAuthority roleGrantedAuthority) {
        Option.of(RequestContextHolder.getRequestAttributes()).forEach(e -> {
            e.setAttribute(RoleName.MATCHED_ROLE_GRANTED, roleGrantedAuthority, 0);
        });
    }

    Collection<? extends GrantedAuthority> extractAuthorities(Authentication authentication) {
        return authentication.getAuthorities();
    }

    public Long currentTenantId() {
        Long tenantId = null;
        Authentication token = SecurityContextHolder.getContext().getAuthentication();
        if (token != null) {
            Object user = token.getPrincipal();
            if (user instanceof InternalUser) {
                Long tenantOrigin = ((InternalUser) user).getTenantId();
                if (tenantOrigin != null) {
                    tenantId = tenantOrigin;
                }
            }
        }
        return tenantId;
    }
}
