package com.gitssie.openapi.event;

import com.gitssie.openapi.auth.RolePermissionEvaluator;
import com.gitssie.openapi.models.auth.RoleName;
import com.gitssie.openapi.models.user.UserRole;
import io.ebean.ValuePair;
import io.ebean.event.BeanDeleteIdRequest;
import io.ebean.event.BeanPersistController;
import io.ebean.event.BeanPersistRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UserRoleListener implements BeanPersistController, ApplicationListener<ApplicationReadyEvent> {
    private RolePermissionEvaluator permissionEvaluator;

    @Override
    public int getExecutionOrder() {
        return Integer.MIN_VALUE;
    }

    @Override
    public boolean isRegisterFor(Class<?> cls) {
        return UserRole.class.isAssignableFrom(cls);
    }

    @Override
    public boolean preInsert(BeanPersistRequest<?> request) {
        if (permissionEvaluator.isAdmin()) {
            return true;
        }
        UserRole role = (UserRole) request.bean();
        String code = StringUtils.trim(role.getCode());
        if (StringUtils.isEmpty(code)) {
            return true;
        }
        if (RoleName.isSafeRole(code)) {
            role.setCode(RoleName.appendPrefix(code));
        }
        return true;
    }

    @Override
    public boolean preUpdate(BeanPersistRequest<?> request) {
        if (permissionEvaluator.isAdmin()) {
            return true;
        }
        ValuePair code = request.updatedValues().get("code");
        //如果修改了编码
        if (code != null && (RoleName.isSafeRole((String) code.getOldValue()) || RoleName.isSafeRole((String) code.getNewValue()))) {
            return false; //无法修改或设置系统安全角色
        }
        return true;
    }

    @Override
    public boolean preDelete(BeanPersistRequest<?> request) {
        if (permissionEvaluator.isAdmin()) {
            return true;
        }
        UserRole role = (UserRole) request.bean();
        String code = StringUtils.trim(role.getCode());
        if (RoleName.isSafeRole(code)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean preSoftDelete(BeanPersistRequest<?> request) {
        return preDelete(request);
    }

    @Override
    public void preDelete(BeanDeleteIdRequest request) {

    }

    @Override
    public void postInsert(BeanPersistRequest<?> request) {

    }

    @Override
    public void postUpdate(BeanPersistRequest<?> request) {

    }

    @Override
    public void postDelete(BeanPersistRequest<?> request) {

    }

    @Override
    public void postSoftDelete(BeanPersistRequest<?> request) {

    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        permissionEvaluator = event.getApplicationContext().getBean(RolePermissionEvaluator.class);
    }
}
