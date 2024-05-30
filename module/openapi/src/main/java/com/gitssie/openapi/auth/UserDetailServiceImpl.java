package com.gitssie.openapi.auth;

import com.gitssie.openapi.models.auth.*;
import com.gitssie.openapi.models.user.RolePermission;
import com.gitssie.openapi.models.user.User;
import com.gitssie.openapi.models.user.UserRole;
import com.google.common.collect.Maps;
import io.ebean.Database;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserDetailServiceImpl implements UserDetailsService, ApplicationListener<InteractiveAuthenticationSuccessEvent> {

    private Database db;

    public UserDetailServiceImpl(Database db) {
        this.db = db;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = db.createQuery(User.class)
                .where().eq("username", username)
                .findOne();
        if (user == null) {
            throw new UsernameNotFoundException("Cant not found user by username:" + username);
        }
        return toUserDetails(user);
    }

    private UserDetails toUserDetails(User user) {
        LazyUser lazyUser = new LazyUser(db.name(), user);
        UserDetails userDetails = new SecurityUser(lazyUser, () -> loadAuthorities(user));
        return userDetails;
    }

    private Collection<? extends GrantedAuthority> loadAuthorities(User user) {
        List<GrantedAuthority> authorities = new LinkedList<>();
        authorities.add(new SimpleGrantedAuthority(RoleName.ROLE_AUTHENTICATED));
        List<UserRole> roles = user.getRoles();
        if (ObjectUtils.isEmpty(roles)) {
            return authorities;
        }
        List<RolePermission> permissions = db.createQuery(RolePermission.class)
                .fetch("permission", "code")
                .where().in("role", roles).findList();


        Map<Long, RoleGrantedAuthority> grantedAuthorityMap = Maps.newHashMapWithExpectedSize(roles.size());
        RoleGrantedAuthority authority;
        String apiKey;
        for (UserRole role : roles) {
            authority = new RoleGrantedAuthority(role.getId(), role.getCode(), role.getDataPermission(), new HashMap<>());
            grantedAuthorityMap.put(role.getId(), authority);
            authorities.add(authority);
        }
        for (RolePermission permission : permissions) {
            Long roleId = permission.getRole().getId();
            authority = grantedAuthorityMap.get(roleId);
            if (authority == null || permission.getPermission() == null) {
                continue;
            }
            apiKey = permission.getPermission().getCode();
            authority.setAuthority(apiKey, permission.getVerbs());
        }

        return authorities;
    }


    @Override
    public void onApplicationEvent(InteractiveAuthenticationSuccessEvent event) {
        Authentication authResult = event.getAuthentication();
        if (authResult.getPrincipal() instanceof SecurityUser) {
            SecurityUser securityUser = (SecurityUser) authResult.getPrincipal();
            InternalUser internalUser = securityUser.getUser();
            if (internalUser != null && internalUser instanceof User) {
                //无状态的数据更新
                User user = (User) internalUser;
                user.setLastestLoginAt(new Date());
                db.createUpdate(User.class, "update User set lastestLoginAt=? where id=?")
                        .setParameter(1, new Date())
                        .setParameter(2, user.getId())
                        .execute();
            }
        }
    }
}
