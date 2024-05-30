package com.gitssie.openapi.xentity;

import io.ebeaninternal.server.deploy.parse.tenant.XEntity;
import io.ebeaninternal.server.deploy.parse.tenant.XEntityFinder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.persistence.EntityNotFoundException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

public class DefaultXEntityFinder implements XEntityFinder {
    private XEntityManager entityManager;

    public DefaultXEntityFinder(XEntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public XEntity getEntity(Class<?> beanClass) {
        XEntityCache item = entityManager.getEntity(beanClass);
        item.loadClass();
        item.loadAssoc();
        return item.desc;
    }

    @Override
    public boolean isChanged(Class<?> entityClass) {
        return false;
    }
}
