package com.gitssie.openapi.models.auth;

import org.apache.commons.lang3.ArrayUtils;

public class RoleName {
    private static final String ROLE_PREFIX = "ROLE_";
    public static final String ROLE_ROOT = "ROLE_ROOT"; //超级管理员,系统所有权限
    public static final String ROLE_ADMIN = "ROLE_ADMIN"; //系统管理员
    public static final String ROLE_TENANT_ADMIN = "ROLE_TENANT_ADMIN"; //租户管理员

    public static final String[] SAFE_ROLES = new String[]{ROLE_ROOT, ROLE_ADMIN, ROLE_TENANT_ADMIN};

    public static final String ROLE_AUTHENTICATED = "ROLE_AUTHENTICATED"; //使用用户登录之后默认获得的角色

    public static final String MATCHED_ROLE_GRANTED = "MATCHED_ROLE_GRANTED";

    public static boolean isSafeRole(String role) {
        return ArrayUtils.contains(SAFE_ROLES, role);
    }

    public static String appendPrefix(String role) {
        return ROLE_PREFIX + role;
    }

    public static String appendPrefixIf(String role) {
        if (role.startsWith(ROLE_PREFIX)) {
            return role;
        }
        return appendPrefix(role);
    }

}
