package com.gitssie.openapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitssie.openapi.config.EnableEbeanRepositories;
import com.gitssie.openapi.ebean.GeneratorType;
import com.gitssie.openapi.ebean.IdWorkerGenerator;
import com.gitssie.openapi.ebean.RedisIdWorkerGenerator;
import com.gitssie.openapi.ebean.SpringJdbcTransactionManager;
import com.gitssie.openapi.file.COSAssets;
import com.gitssie.openapi.file.COSProperties;
import com.gitssie.openapi.models.ObjectBean;
import com.gitssie.openapi.utils.Json;
import com.gitssie.openapi.xentity.DefaultXEntityFinder;
import com.gitssie.openapi.xentity.DefaultXEntityProvider;
import com.gitssie.openapi.xentity.XEntityManager;
import com.gitssie.openapi.xentity.gen.CodeGenerated;
import com.gitssie.openapi.xentity.gen.DocCodeGenerated;
import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.ExpressionFactory;
import io.ebean.bean.XEntityProvider;
import io.ebean.config.*;
import io.ebean.event.BeanPersistController;
import io.ebean.event.BeanPersistListener;
import io.ebean.event.BeanQueryAdapter;
import io.ebeaninternal.server.expression.DefaultExpressionFactory;
import io.vavr.Lazy;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.util.List;

@Configuration
@EnableCaching
//@EnableWebSecurity
@EnableEbeanRepositories
@EnableConfigurationProperties(COSProperties.class)
@EnableSpringDataWebSupport
@EnableAspectJAutoProxy(exposeProxy = true)
@ComponentScan("com.gitssie.openapi")
public class AppConfig {
    @Autowired(required = false)
    private List<BeanPersistController> listeners;
    @Autowired(required = false)
    private List<BeanPersistListener> eventListeners;
    @Autowired(required = false)
    private List<BeanQueryAdapter> queryAdapters;

    @Bean
    public Database createEbeanDatabase(DatabaseConfig serverConfig) {
        return DatabaseFactory.create(serverConfig);
    }


    @Bean
    @ConditionalOnMissingBean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public XEntityProvider entityProvider(XEntityManager manager) throws Exception {
        XEntityProvider entityProvider = new DefaultXEntityProvider(new DefaultXEntityFinder(manager));
        manager.setTenantProvider(entityProvider.tenantProvider());
        manager.afterPropertiesSet();
        return entityProvider;
    }

    @Bean
    @ConditionalOnMissingBean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public IdGenerator idGenerator(ObjectProvider<StringRedisTemplate> stringRedisTemplate) {
        StringRedisTemplate redisTemplate = stringRedisTemplate.getIfAvailable();
        if (redisTemplate != null) {
            String redisKey = GeneratorType.IdWorker;
            return new RedisIdWorkerGenerator(GeneratorType.IdWorker, redisKey, redisTemplate);
        } else {
            return new IdWorkerGenerator(GeneratorType.IdWorker, 1);
        }
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public DatabaseConfig createDatabaseConfig(DataSource dataSource,
                                               IdGenerator idGenerator,
                                               CurrentTenantProvider tenantProvider,
                                               TenantCatalogProvider tenantCatalogProvider,
                                               CurrentUserProvider currentUserProvider,
                                               XEntityProvider entityProvider,
                                               ApplicationContext context) {
        DatabaseConfig config = new DatabaseConfig();
        config.setDataSource(dataSource);
        config.setClassLoadConfig(new ClassLoadConfig(context.getClassLoader()));
        config.loadFromProperties();
        config.setDdlRun(false);
        config.setDdlCreateOnly(false);
        config.setDdlGenerate(false);
        config.setDdlStrictMode(false);
        config.setDdlExtra(false);

        config.setTenantCatalogProvider(tenantCatalogProvider);
        config.setCurrentUserProvider(currentUserProvider);
        config.setCurrentTenantProvider(tenantProvider);
//        config.setChangeLogPrepare(new DefaultChangeLogPrepare());
//        config.setChangeLogListener(new DefaultChangeLogListener());


        if (ObjectUtils.isNotEmpty(listeners)) {
            config.setPersistControllers(listeners);
        }
        if (ObjectUtils.isNotEmpty(eventListeners)) {
            config.setPersistListeners(eventListeners);
        }
        if (ObjectUtils.isNotEmpty(queryAdapters)) {
            config.setQueryAdapters(queryAdapters);
        }

        config.add(idGenerator);
        config.add(new CodeGenerated());
        config.add(new DocCodeGenerated());


        config.putServiceObject(XEntityProvider.class.getName(), entityProvider);

        config.setExternalTransactionManager(new SpringJdbcTransactionManager());
        return config;
    }

    @Bean
    public RestTemplate defaultRestTemplate(RestTemplateBuilder b) {
        return b.build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "objectBeanClass")
    public Lazy<Class<?>> objectBeanClass() {
        Lazy<Class<?>> lazy = Lazy.of(() -> ObjectBean.class);
        return lazy;
    }

//    @Bean
//    @ConditionalOnMissingBean
//    public ObjectMapper objectMapper() {
//        return Json.mapper();
//    }

    @Bean
    @ConditionalOnProperty(prefix = "file.cos", name = "secret-id")
    public COSAssets cosAssets(COSProperties properties) {
        return new COSAssets(properties);
    }


}