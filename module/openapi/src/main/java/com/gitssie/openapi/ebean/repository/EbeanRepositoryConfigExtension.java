package com.gitssie.openapi.ebean.repository;

import com.google.common.collect.Lists;
import io.ebean.bean.EntityBean;
import org.springframework.data.jdbc.repository.config.JdbcRepositoryConfigExtension;
import org.springframework.data.jdbc.repository.support.JdbcRepositoryFactoryBean;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.stereotype.Component;

import javax.persistence.Entity;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Locale;

/**
 * @author: Awesome
 * @create: 2024-03-11 17:30
 */

public class EbeanRepositoryConfigExtension extends RepositoryConfigurationExtensionSupport {

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.config.RepositoryConfigurationExtension#getModuleName()
     */
    @Override
    public String getModuleName() {
        return "Ebean";
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#getModulePrefix()
     */
    @Override
    protected String getModulePrefix() {
        return getModuleName().toLowerCase(Locale.US);
    }

    @Override
    protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
        return Lists.newArrayList(Entity.class);
    }

    @Override
    protected boolean isStrictRepositoryCandidate(RepositoryMetadata metadata) {
        boolean supported = EntityBean.class.isAssignableFrom(metadata.getDomainType());
        return supported || isStrictRepositoryCandidate(metadata);
    }

    @Override
    public String getRepositoryFactoryBeanClassName() {
        return EbeanJdbcRepositoryFactoryBean.class.getName();
    }
}
