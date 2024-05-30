package com.gitssie.openapi.event;

import com.gitssie.openapi.models.BasicDomain;
import com.gitssie.openapi.models.user.User;
import io.ebean.event.BeanDeleteIdRequest;
import io.ebean.event.BeanPersistController;
import io.ebean.event.BeanPersistRequest;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class EntityBeanListener implements BeanPersistController {
    @Override
    public int getExecutionOrder() {
        return 0;
    }

    @Override
    public boolean isRegisterFor(Class<?> cls) {
        return BasicDomain.class.isAssignableFrom(cls);
    }

    @Override
    public boolean preInsert(BeanPersistRequest<?> request) {
        BasicDomain bean = (BasicDomain) request.bean();
        preInsertOwner(bean);
        preInsertDimDepart(bean);
        return true;
    }

    private void preInsertOwner(BasicDomain bean) {
        User user = bean.getCreatedBy();
        //如果未设置数据归属人,则数据默认由创建者所有
        if (user != null && bean.getOwner() == null) {
            bean.setOwner(user);
        }
    }

    //设置数据的部门归所
    private void preInsertDimDepart(BasicDomain bean) {
        User user = bean.getOwner();
        //如果数据未设置部门,则在新增的时候使用数据归所者的部门
        if (user != null && bean.getDimDepart() == null) {
            bean.setDimDepart(user.getDimDepart());
        }
    }

    private void preUpdateDimDepart(BasicDomain bean, Set<String> updatedProperties) {
        //如果更新了数据归宿,但是没有主动的更新部门,则取数据归宿人的部门信息
        User user = bean.getOwner();
        if (updatedProperties.contains("owner") && !updatedProperties.contains("dimDepart")) {
            if (user != null) {
                bean.setDimDepart(user.getDimDepart());
            }
        }
    }

    @Override
    public boolean preUpdate(BeanPersistRequest<?> request) {
        BasicDomain bean = (BasicDomain) request.bean();
        Set<String> updatedProperties = request.updatedProperties();
        preUpdateDimDepart(bean, updatedProperties);

        return true;
    }

    @Override
    public boolean preDelete(BeanPersistRequest<?> request) {
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
