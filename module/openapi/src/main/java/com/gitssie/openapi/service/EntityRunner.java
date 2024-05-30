package com.gitssie.openapi.service;

import com.gitssie.openapi.data.Code;
import com.gitssie.openapi.ebean.dbmigration.DBMigrationService;
import com.gitssie.openapi.models.xentity.EntityChangeSet;
import com.gitssie.openapi.models.xentity.EntityChangeSetSql;
import com.gitssie.openapi.models.xentity.EntityMapping;
import com.gitssie.openapi.xentity.XEntityCache;
import com.gitssie.openapi.xentity.XEntityManager;
import io.ebean.Database;
import io.ebean.SqlUpdate;
import io.ebean.Transaction;
import io.ebean.config.CurrentTenantProvider;
import io.ebean.ddlrunner.DdlRunner;
import io.ebeaninternal.api.SpiEbeanServer;
import io.ebeaninternal.dbmigration.model.ModelContainer;
import io.ebeaninternal.dbmigration.model.ModelDiff;
import io.ebeaninternal.server.deploy.BeanDescriptor;
import io.ebeaninternal.server.deploy.BeanDescriptorManagerProvider;
import io.ebeaninternal.server.deploy.parse.tenant.XEntity;
import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.PreparedStatement;
import java.sql.SQLException;


@Service
public class EntityRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityRunner.class);
    private final DBMigrationService migrationService;
    private final XEntityManager entityManager;
    private final SpiEbeanServer database;

    public EntityRunner(DBMigrationService migrationService, XEntityManager entityManager, Database database) {
        this.migrationService = migrationService;
        this.entityManager = entityManager;
        this.database = (SpiEbeanServer) database;
    }

    /**
     * 创建数据库的表
     *
     * @return
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Either<Code, String> createTable(EntityMapping mapping) {
        return createOrAlterTable(mapping);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Either<Code, String> alterTable(EntityMapping mapping) {
        return createOrAlterTable(mapping);
    }

    private void executeDDL(String sql) throws SQLException {
        Transaction transaction = database.currentTransaction();
        assert transaction.isActive();
        LOGGER.info(sql);
        DdlRunner runner = new DdlRunner(true, getClass().getSimpleName(), database.databasePlatform().getName());
        runner.runAll(sql, transaction.connection());
    }

    public ModelContainer getCurrentVersion(EntityMapping mapping) {
        EntityChangeSet changeSet = getEntityChangeSet(mapping);
        if (changeSet == null) {
            return new ModelContainer();
        } else {
            return migrationService.xmlToChangeSet(changeSet.getChangeSet(), String.valueOf(changeSet.getVersion()));
        }
    }

    private EntityChangeSet updateChangeSet(EntityMapping mapping, String xml) {
        EntityChangeSet changeSet = getEntityChangeSet(mapping);
        if (changeSet == null) {
            changeSet = new EntityChangeSet();
            changeSet.setId(mapping.getId());
            changeSet.setEntityId(mapping.getId());
        }
        changeSet.setChangeSet(xml);
        database.save(changeSet);
        return changeSet;
    }

    private EntityChangeSet getEntityChangeSet(EntityMapping mapping) {
        return database.find(EntityChangeSet.class, mapping.getId());
    }

    private EntityChangeSetSql insertChangeSetSQL(EntityMapping mapping, String sql) {
        EntityChangeSetSql changeSetSql = new EntityChangeSetSql();
        changeSetSql.setEntityId(mapping.getId());
        changeSetSql.setChangeSql(sql);
        database.save(changeSetSql);
        return changeSetSql;
    }

    private BeanDescriptorManagerProvider getBeanDescriptorManagerProvider() {
        return database.config().getServiceObject(BeanDescriptorManagerProvider.class);
    }

    private Either<Code, String> createOrAlterTable(EntityMapping mapping) {
        XEntityCache entity = entityManager.loadXEntity(mapping);
        entity.loadAll();
        BeanDescriptorManagerProvider provider = getBeanDescriptorManagerProvider();
        try {
            BeanDescriptor<?> beanDescriptor = provider.createBeanDescriptor(entity.getBeanType(), entity.desc);
            ModelContainer current = getCurrentVersion(mapping);
            ModelContainer model = migrationService.changeSet(beanDescriptor);
            ModelDiff diff = new ModelDiff(current);
            diff.compareTo(model);
            if (diff.isEmpty()) {
                return Either.right("没有字段需要进行变更");
            }
            String xml = migrationService.changeSetToXML(model);
            String sql = migrationService.changeSetToSQL(diff, model);

            updateChangeSet(mapping, xml);
            insertChangeSetSQL(mapping, sql);

            LOGGER.info(xml);
            executeDDL(sql);
            return Either.right(sql);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Either<Code, Boolean> deploy(EntityMapping mapping) {
        XEntityCache entity = entityManager.loadXEntity(mapping);
        entity.loadAll();
        BeanDescriptorManagerProvider provider = getBeanDescriptorManagerProvider();
        try {
            CurrentTenantProvider tenantProvider = entityManager.getTenantProvider();
            boolean success = provider.redeploy(tenantProvider.currentId(), entity.getBeanType(), entity.desc);
            if (success) {
                entityManager.updateXEntityCache(tenantProvider.currentId(), entity);
            }
            return Either.right(success);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
