package com.gitssie.openapi.models.auth;

import com.gitssie.openapi.models.user.DataPermissionEnum;
import com.gitssie.openapi.models.user.PermissionVerb;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RoleGrantedAuthority implements GrantedAuthority {
    private Long roleId; //角色ID
    private String roleCode; //角色编码
    private DataPermissionEnum dataPermission; //数据权限
    private Map<String, String[]> permissionMap; //操作权限

    public RoleGrantedAuthority(Long roleId, String roleCode, DataPermissionEnum dataPermission, Map<String, String[]> permissionMap) {
        this.roleId = roleId;
        this.roleCode = roleCode;
        this.dataPermission = dataPermission;
        this.permissionMap = permissionMap;
    }

    public void setAuthority(String apiKey, List<PermissionVerb> verbs) {
        String[] verbsCode = new String[verbs.size()];
        for (int i = 0; i < verbs.size(); i++) {
            verbsCode[i] = verbs.get(i).getCode();
        }
        permissionMap.put(apiKey, verbsCode);
    }

    public boolean match(VerbAttribute attribute) {
        String[] verbs = permissionMap.get(attribute.getApiKey());
        if (verbs == null) {
            return false;
        }
        for (String verbCode : verbs) {
            if (PermissionVerb.WILD_WORD.equals(verbCode) ||
                    StringUtils.equals(verbCode, attribute.getCode())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getAuthority() {
        return roleCode;
    }

    public Long getRoleId() {
        return roleId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public DataPermissionEnum getDataPermission() {
        return dataPermission;
    }

    public Map<String, String[]> getPermissionMap() {
        return permissionMap;
    }
}
