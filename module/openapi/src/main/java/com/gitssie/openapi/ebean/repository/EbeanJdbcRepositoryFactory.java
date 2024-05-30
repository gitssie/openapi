package com.gitssie.openapi.ebean.repository;

import io.ebean.Database;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.lang.Nullable;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Optional;

public class EbeanJdbcRepositoryFactory extends RepositoryFactorySupport {
    private static final String INTERFACE_NEEDS_TO_BE_RESOURCE = "For repository you need to provide javascript file path for the interface. Use @Resource for repository interface.";
    private EbeanJsEngine jsEngine;
    private Database database;
    private JdbcConverter converter;
    private ApplicationEventPublisher publisher;

    public EbeanJdbcRepositoryFactory(EbeanJsEngine jsEngine, Database database, JdbcConverter converter, ApplicationEventPublisher publisher) {
        this.jsEngine = jsEngine;
        this.database = database;
        this.converter = converter;
        this.publisher = publisher;
    }

    @Override
    public <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> aClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Object getTargetRepository(RepositoryInformation repositoryInformation) {
        Class<?> repositoryInterface = repositoryInformation.getRepositoryInterface();
        Resource resource = repositoryInterface.getAnnotation(Resource.class);
        if (resource == null || StringUtils.isEmpty(resource.name())) {
            throw new IllegalStateException(INTERFACE_NEEDS_TO_BE_RESOURCE);
        } else {
            compileResource(repositoryInterface, resource.name());
        }
        return instantiateClass(repositoryInformation.getRepositoryBaseClass(), database, repositoryInformation.getDomainType());
    }

    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
        return SimpleEbeanRepository.class;
    }

    @Override
    protected Optional<QueryLookupStrategy> getQueryLookupStrategy(@Nullable QueryLookupStrategy.Key key,
                                                                   QueryMethodEvaluationContextProvider evaluationContextProvider) {

        return Optional.of(new EbeanJdbcQueryLookupStrategy(jsEngine, database, converter));
    }

    protected void compileResource(Class<?> repositoryInterface, String path) {
        try {
            jsEngine.compileAndCached(repositoryInterface, path);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Compile JavaScript file %s error.", path), e);
        }

    }

}
