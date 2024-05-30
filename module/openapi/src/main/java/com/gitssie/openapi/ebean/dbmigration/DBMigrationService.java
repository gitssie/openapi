package com.gitssie.openapi.ebean.dbmigration;


import com.gitssie.openapi.ebean.VisitSingleUsing;
import com.google.common.collect.Lists;
import io.ebean.Database;
import io.ebean.migration.MigrationVersion;
import io.ebeaninternal.api.SpiEbeanServer;
import io.ebeaninternal.dbmigration.ddlgeneration.*;
import io.ebeaninternal.dbmigration.ddlgeneration.platform.PlatformDdl;
import io.ebeaninternal.dbmigration.migration.*;
import io.ebeaninternal.dbmigration.migrationreader.MigrationXmlReader;
import io.ebeaninternal.dbmigration.model.MConfiguration;
import io.ebeaninternal.dbmigration.model.ModelContainer;
import io.ebeaninternal.dbmigration.model.ModelDiff;
import io.ebeaninternal.dbmigration.model.build.ModelBuildBeanVisitor;
import io.ebeaninternal.dbmigration.model.build.ModelBuildContext;
import io.ebeaninternal.server.deploy.BeanDescriptor;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class DBMigrationService {
    private SpiEbeanServer server;

    public DBMigrationService(Database database) {
        this.server = (SpiEbeanServer) database;
    }

    public ModelContainer changeSet(Class<?> beanClass) {
        return changeSet(server.descriptor(beanClass));
    }

    public ModelContainer changeSet(BeanDescriptor<?> desc) {
        ModelContainer model = new ModelContainer();
        ModelBuildContext context = new ModelBuildContext(model, server.databasePlatform(), server.config().getConstraintNaming(), true);
        ModelBuildBeanVisitor visitor = new ModelBuildBeanVisitor(context);
        VisitSingleUsing visit = new VisitSingleUsing(visitor, server);
        visit.visitBean(desc);
        // adjust the foreign keys on the 'draft' tables
        context.adjustDraftReferences();
        return model;
    }

    public ModelContainer xmlToChangeSet(String xml, String version) {
        ModelContainer model = new ModelContainer();
        Migration migration = MigrationXmlReader.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        MigrationVersion v = MigrationVersion.parse(version);
        model.apply(migration, v);
        return model;
    }

    /**
     * 生成Create Table 语句
     *
     * @param model
     * @return
     */
    public String changeSetToSQL(ModelContainer model) {
        return changeSetToSQL(new ModelContainer(), model);
    }

    /**
     * Model生Alter TABLE SQL
     *
     * @param model
     * @return
     */
    public String changeSetToSQL(ModelContainer current, ModelContainer model) {
        ModelDiff diff = new ModelDiff(current);
        diff.compareTo(model);
        return changeSetToSQL(diff, model);
    }

    public String changeSetToSQL(ModelDiff diff, ModelContainer model) {
        ChangeSet changeSet = diff.getApplyChangeSet();

        PlatformDdl ddl = PlatformDdlBuilder.create(server.databasePlatform());
        DdlHandler handler = new BaseDdlHandler(server.config(), ddl);
        DdlOptions ddlOptions = new DdlOptions();
        ddlOptions.setForeignKeySkipCheck(true);

        DdlWrite writer = new DdlWrite(new MConfiguration(), model, ddlOptions);
        handler.generateProlog(writer);
        handler.generate(writer, changeSet);
        handler.generateEpilog(writer);

        return writer.toString();
    }

    public String changeSetToXML(ModelContainer model) throws Exception {
        ModelDiff diff = new ModelDiff();
        diff.compareTo(model);

        JAXBContext jaxbContext = JAXBContext.newInstance(Migration.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

        StringWriter buf = new StringWriter();
        marshaller.marshal(diff.getMigration(), buf);
        return buf.toString();
    }
}
