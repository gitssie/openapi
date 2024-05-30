package com.gitssie.openapi.lock;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor;

public class BeanFactoryLockOperationSourceAdvisor extends AbstractBeanFactoryPointcutAdvisor {
    private final LockOperationSourcePointcut pointcut = new LockOperationSourcePointcut();

    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }

    public void setClassFilter(ClassFilter classFilter) {
        this.pointcut.setClassFilter(classFilter);
    }

    public void setLockOperationSource(AnnotationLockOperationSource lockOperationSource) {
        pointcut.setLockOperationSource(lockOperationSource);
    }
}
