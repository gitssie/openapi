package com.gitssie.openapi.service;

import com.gitssie.openapi.models.auth.RoleGrantedAuthority;
import com.gitssie.openapi.models.auth.RoleName;
import com.gitssie.openapi.models.user.DataPermissionEnum;
import com.gitssie.openapi.models.user.Department;
import com.gitssie.openapi.models.user.User;
import com.gitssie.openapi.models.user.UserRole;
import com.google.common.collect.Lists;
import io.ebean.Expr;
import io.ebean.Expression;
import io.ebean.config.CurrentUserProvider;
import io.vavr.control.Option;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.List;
import java.util.function.Function;

@Service
public class AuthenticationService {
    @Autowired
    private CurrentUserProvider userProvider;
    @Autowired
    private TreeService treeService;

    public Option<User> getCurrentUser() {
        return Option.of((User) userProvider.currentUser());
    }

    public Option<User> getCurrentUser(Authentication authentication) {
//        authentication.getPrincipal();;
        return Option.of((User) userProvider.currentUser());
    }

    public Option<RoleGrantedAuthority> bestRoleForRequest() {
        return Option.of(RequestContextHolder.getRequestAttributes()).flatMap(ex -> {
            return Option.of((RoleGrantedAuthority) ex.getAttribute(RoleName.MATCHED_ROLE_GRANTED, 0));
        });
    }

    public DataPermissionEnum getDataPermission() {
        return bestRoleForRequest().map(e -> e.getDataPermission()).getOrElse(() -> {
            return DataPermissionEnum.ALL;
        });
    }

    public Expression dataFilterWhere(DataPermissionEnum dataPermission) {
        Option<User> userOpt = getCurrentUser();
        if (userOpt.isEmpty()) {
            return null;
        }
        User user = userOpt.get();

        //如果用户的部门不存在,则只看自己的数据
        /*
        if (user.getDimDepart() == null) {
            switch (dataPermission) {
                case SELF:
                case SELF_STAFF:
                case DEPART:
                case DEPART_STAFF:
                    return Expr.eq("owner", user);
                case ALL:
            }
        }*/
        //正常的数据权限过滤
        switch (dataPermission) {
            case SELF:
            case SELF_STAFF:
                return Expr.eq("owner", user);
            case DEPART:
                return Expr.eq("dimDepart", user.getDimDepart());
            case DEPART_STAFF:
                return Expr.in("dimDepart", departStaffDeparts(user.getDimDepart()));
            case ALL:
        }
        return null;
    }

    /**
     * 本部门及下属部门
     *
     * @return
     */
    public List<Department> departStaffDeparts(Department department) {
        if (department == null) {
            return Lists.newLinkedList();
        }
        List<Department> children = treeService.children(Department.class, "id", department.getTree());
        children.add(department);
        return children;
    }

    public Expression where(int dataPermission) {
        return dataFilterWhere(DataPermissionEnum.values()[dataPermission]);
    }

    public Expression where(DataPermissionEnum dataPermission) {
        return dataFilterWhere(dataPermission);
    }

    public Function<DataPermissionEnum, Expression> filterByRole(Authentication authentication) {
        return (dataPermission) -> {
            return filterExpr(dataPermission);
        };
    }

    /**
     * 数据过滤查询条件
     *
     * @return
     */
    private Expression filterExpr(DataPermissionEnum dataPermission) {
        if (dataPermission == null) {
            return dataFilterWhere(getDataPermission());
        } else {
            return where(dataPermission);
        }
    }
}
