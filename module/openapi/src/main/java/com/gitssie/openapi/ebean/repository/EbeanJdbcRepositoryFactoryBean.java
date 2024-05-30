package com.gitssie.openapi.ebean.repository;

import io.ebean.Database;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport;
import org.springframework.util.Assert;

import java.io.Serializable;

public class EbeanJdbcRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable>
        extends TransactionalRepositoryFactoryBeanSupport<T, S, ID> implements ApplicationEventPublisherAware {

    private ApplicationEventPublisher publisher;
    private BeanFactory beanFactory;
    private Database database;
    private JdbcConverter converter;
    private EbeanJsEngine jsEngine;

    public EbeanJdbcRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    protected RepositoryFactorySupport doCreateRepositoryFactory() {
        return new EbeanJdbcRepositoryFactory(jsEngine, database, converter, publisher);
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        super.setApplicationEventPublisher(publisher);
        this.publisher = publisher;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        super.setBeanFactory(beanFactory);
        this.beanFactory = beanFactory;
    }

    @Autowired
    public void setConverter(JdbcConverter converter) {
        Assert.notNull(converter, "JdbcConverter must not be null");
        this.converter = converter;
    }

    @Override
    public void afterPropertiesSet() {
        if (this.database == null) {
            Assert.state(beanFactory != null, "If no DataAccessStrategy is set a BeanFactory must be available.");
            this.database = this.beanFactory.getBeanProvider(Database.class).getIfAvailable();
        }
        if (this.jsEngine == null) {
            jsEngine = new EbeanJsEngine();
        }
        super.afterPropertiesSet();
    }
}
