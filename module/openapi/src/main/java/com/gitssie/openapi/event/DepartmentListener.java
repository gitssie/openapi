package com.gitssie.openapi.event;

import com.gitssie.openapi.data.CodeException;
import com.gitssie.openapi.models.tree.PathTree;
import com.gitssie.openapi.models.user.Department;
import com.gitssie.openapi.ebean.PathTreeEmbed;
import com.google.common.collect.Sets;
import io.ebean.event.BeanDeleteIdRequest;
import io.ebean.event.BeanPersistController;
import io.ebean.event.BeanPersistRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DepartmentListener implements BeanPersistController {
    @Autowired
    private PathTreeEmbed pathTreeEmbed;
    private String elPath = "tree.path";
    private Set<String> updatedDisabled = Sets.newHashSet("parent");

    @Override
    public int getExecutionOrder() {
        return 0;
    }

    @Override
    public boolean isRegisterFor(Class<?> cls) {
        return cls == Department.class;
    }

    @Override
    public boolean preInsert(BeanPersistRequest<?> request) {
        Department bean = (Department) request.bean();
        PathTree parent = null;
        if (bean.getParent() != null) {
            parent = bean.getParent().getTree();
        }
        PathTree tree = pathTreeEmbed.createNode(request.database(), Department.class, elPath, parent);
        bean.setTree(tree);
        return true;
    }

    @Override
    public boolean preUpdate(BeanPersistRequest<?> request) {
        for (String p : request.updatedProperties()) {
            if (updatedDisabled.contains(p)) {
                throw new CodeException("无法修改父级部门");
            }
        }
        return true;
    }

    @Override
    public boolean preDelete(BeanPersistRequest<?> request) {
        Department bean = (Department) request.bean();
        PathTree parent = bean.getTree();
        boolean exists = pathTreeEmbed.hasChildren(request.database(), Department.class, elPath, parent);
        if(exists){
            throw new CodeException("部门包含子级部门,无法删除");
        }
        return true;
    }

    @Override
    public boolean preSoftDelete(BeanPersistRequest<?> request) {
        return false;
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
}
