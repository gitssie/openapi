package com.gitssie.openapi.service;

import com.gitssie.openapi.data.Code;
import com.gitssie.openapi.models.BasicDomain;
import com.gitssie.openapi.models.audit.AuditStatus;
import com.gitssie.openapi.models.user.User;
import io.ebean.Database;
import io.vavr.control.Either;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

@Service
public class AuditService {
    @Autowired
    private Database db;

    /**
     * 提交审批
     *
     * @param apiKey
     * @param user
     * @param beans
     * @param <T>
     */
    public <T extends BasicDomain> List<Either<Code, T>> submit(String apiKey, User user, List<T> beans) {
        List<Either<Code, T>> result = new LinkedList<>();
        for (T bean : beans) {
            Either<Code, T> one = submit(apiKey, user, bean);
            result.add(one);
        }
        return result;
    }

    /**
     * 提交审批
     *
     * @param apiKey
     * @param user
     * @param bean
     * @param <T>
     */
    public <T extends BasicDomain> Either<Code, T> submit(String apiKey, User user, T bean) {
        return AuditStatus.of(bean.getStatus()).map(st -> {
            if (st.canBeProcessing()) {
                bean.setStatus(AuditStatus.PROCESSING.ordinal());
                bean.setLockStatus(true);
                bean.set("submitBy", user); //提交人
                //后续需要记录日志
                db.update(bean);
            }
            return bean;
        });
    }

    /**
     * 批量审批通过
     *
     * @param apiKey
     * @param user
     * @param beans
     * @param <T>
     * @return
     */
    public <T extends BasicDomain> List<Either<Code, T>> approval(String apiKey, User user, List<T> beans) {
        List<Either<Code, T>> result = new LinkedList<>();
        for (T bean : beans) {
            Either<Code, T> one = approval(apiKey, user, bean);
            result.add(one);
        }
        return result;
    }

    /**
     * 单个审批通过
     *
     * @param apiKey
     * @param user
     * @param bean
     * @param <T>
     * @return
     */
    public <T extends BasicDomain> Either<Code, T> approval(String apiKey, User user, T bean) {
        return AuditStatus.of(bean.getStatus()).map(st -> {
            if (st.canBeApproved()) {
                bean.setStatus(AuditStatus.APPROVED.ordinal());
                bean.setLockStatus(true);
                //后续需要记录日志
                db.update(bean);
            }
            return bean;
        });
    }

    public <T extends BasicDomain> List<Either<Code, T>> review(String apiKey, User user, List<T> beans) {
        List<Either<Code, T>> result = new LinkedList<>();
        for (T bean : beans) {
            Either<Code, T> one = review(apiKey, user, bean);
            result.add(one);
        }
        return result;
    }

    public <T extends BasicDomain> Either<Code, T> review(String apiKey, User user, T bean) {
        return AuditStatus.of(bean.getStatus()).map(st -> {
            if (st.canBeReview()) {
                bean.setStatus(AuditStatus.PENDING.ordinal());
                bean.setLockStatus(false);
                //后续需要记录日志
                db.update(bean);
            }
            return bean;
        });
    }
}
