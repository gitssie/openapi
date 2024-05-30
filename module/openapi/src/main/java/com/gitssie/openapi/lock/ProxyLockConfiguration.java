package com.gitssie.openapi.lock;

import org.springframework.aop.support.annotation.AnnotationClassFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.stereotype.Service;

@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ProxyLockConfiguration {

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public BeanFactoryLockOperationSourceAdvisor lockAdvisor(ObjectProvider<LockOperationSupport> context) {
        AnnotationLockOperationSource lockOperationSource = new AnnotationLockOperationSource();
        LockInterceptor lockInterceptor = new LockInterceptor();
        lockInterceptor.setLockOperationSource(lockOperationSource);
        lockInterceptor.setLockSupportOpt(context);

        BeanFactoryLockOperationSourceAdvisor advisor = new BeanFactoryLockOperationSourceAdvisor();
        advisor.setLockOperationSource(lockOperationSource);
        advisor.setAdvice(lockInterceptor);
        advisor.setClassFilter(new AnnotationClassFilter(Service.class));

        return advisor;
    }

    @Bean
    @ConditionalOnMissingBean(RedisLockRegistry.class)
    public LockOperationSupport lockOperationSupportLocal() {
        return new LockOperationSupportLocal();
    }

    @Bean
    @ConditionalOnBean(RedisLockRegistry.class)
    public LockOperationSupport lockOperationSupportRedis(RedisLockRegistry lockRegistry) {
        return new LockOperationSupportRedis(lockRegistry);
    }
}