package com.gitssie.openapi;

import com.gitssie.openapi.ebean.DefaultCurrentTenantProvider;
import com.gitssie.openapi.ebean.VisitSingleUsing;
import com.gitssie.openapi.models.ObjectBean;
import com.gitssie.openapi.models.functree.FuncTree;
import com.gitssie.openapi.models.tree.NestedTree;
import com.gitssie.openapi.models.tree.PathTree;
import com.gitssie.openapi.models.user.Department;
import com.gitssie.openapi.models.xentity.EntityAssoc;
import com.gitssie.openapi.models.xentity.EntityField;
import com.gitssie.openapi.models.xentity.EntityMapping;
import com.gitssie.openapi.xentity.XEntityClassProvider;
import com.google.common.collect.Lists;
import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.annotation.Platform;
import io.ebean.bean.XEntityProvider;
import io.ebean.config.CurrentUserProvider;
import io.ebean.config.DatabaseConfig;
import io.ebean.dbmigration.DbMigration;
import io.ebean.migration.MigrationVersion;
import io.ebeaninternal.api.SpiEbeanServer;
import io.ebeaninternal.dbmigration.DdlGenerator;
import io.ebeaninternal.dbmigration.ddlgeneration.DdlHandler;
import io.ebeaninternal.dbmigration.ddlgeneration.DdlOptions;
import io.ebeaninternal.dbmigration.ddlgeneration.DdlWrite;
import io.ebeaninternal.dbmigration.ddlgeneration.PlatformDdlBuilder;
import io.ebeaninternal.dbmigration.migration.ChangeSet;
import io.ebeaninternal.dbmigration.migration.Migration;
import io.ebeaninternal.dbmigration.migrationreader.MigrationXmlReader;
import io.ebeaninternal.dbmigration.migrationreader.MigrationXmlWriter;
import io.ebeaninternal.dbmigration.model.CurrentModel;
import io.ebeaninternal.dbmigration.model.MConfiguration;
import io.ebeaninternal.dbmigration.model.ModelContainer;
import io.ebeaninternal.dbmigration.model.ModelDiff;
import io.ebeaninternal.dbmigration.model.build.ModelBuildBeanVisitor;
import io.ebeaninternal.dbmigration.model.build.ModelBuildContext;
import io.ebeaninternal.dbmigration.model.visitor.VisitAllUsing;
import io.ebeaninternal.server.deploy.parse.tenant.XEntityFinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.AUTO_CONFIGURED)
@ActiveProfiles("test")
public class XEntityTest {
    //@Autowired
    private Database database;
    @Autowired
    private DataSource dataSource;

    @BeforeEach
    public void setup() throws SQLException {
        DatabaseConfig config = new DatabaseConfig();
        config.setCurrentUserProvider(new CurrentUserProvider() {
            @Override
            public Object currentUser() {
                return 1L;
            }
        });
        config.setCurrentTenantProvider(new DefaultCurrentTenantProvider());
        config.setName("h2");
        config.setDataSource(dataSource);
        config.setDdlRun(true);
        config.setDdlCreateOnly(true);
        config.putServiceObject(XEntityProvider.class.getName(), new SimpleXEntityProvider());
        database = DatabaseFactory.create(config);
    }


    public Department insertDepartment(String name) {
        Department d = new Department();
        d.setDepartName(name);
        d.setTree(createNode(Department.class, "tree.path"));
        database.insert(d);
        return d;
    }

    public Department insertDepartment(Department parent, String name) {
        Department d = new Department();
        d.setDepartName(name);
        d.setTree(createNode(Department.class, "tree.path", parent.getTree()));
        database.insert(d);
        return d;
    }

    @Test
    public void testAddDepartment() {
        for (int i = 0; i < 1; i++) {
            Department d1 = insertDepartment("一级");
            for (int j = 0; j < 2; j++) {
                Department d2 = insertDepartment(d1, "二级");
                Department d3 = insertDepartment(d2, "三级");
                Department d4 = insertDepartment(d3, "四级");
            }
        }

        List<Department> nodes = database.find(Department.class).findList();
        System.out.println(nodes);
    }

    public PathTree createNode(Class<?> clazz, String elPath) {
        return createNode(clazz, elPath, null);
    }

    public PathTree createNode(Class<?> clazz, String elPath, PathTree parent) {
        int level = 1;
        String path;
        if (parent == null) {
            path = findDeeplyRootPath(clazz, elPath);
        } else {
            path = findDeeplyChildPath(clazz, elPath, parent);
            level = parent.getLevel() + 1;
        }
        path = nextPath(path);
        PathTree node = new PathTree(path, level);
        return node;
    }

    private String nextPath(String root) {
        String prefix = "";
        String path = root;
        int i = path.lastIndexOf('.');
        if (i > 0) {
            prefix = path.substring(0, i + 1);
            path = path.substring(i + 1);
        }
        int pathAsInt = Integer.parseInt(path, 16);
        pathAsInt++;
        if (pathAsInt > 0xFF) { //超过最大值
            throw new IllegalStateException("too many children path at root " + root);
        }
        return prefix + String.format("%02X", pathAsInt);
    }

    public String findDeeplyChildPath(Class<?> clazz, String elPath, PathTree parent) {
        String prefix = parent.getPath() + '.';
        PathTree node = database.find(clazz).select(elPath).where().startsWith(elPath, prefix).orderBy(elPath + " desc").setMaxRows(1).findSingleAttribute();
        if (node == null) {
            return prefix + "00";
        } else {
            //这里可能会查询出子级下来,所以需要进行截取
            String path = node.getPath();
            int i = path.indexOf('.', parent.getPath().length() + 1);
            if (i > 0) {
                return path.substring(0, i);
            } else {
                return path;
            }
        }
    }

    public String findDeeplyRootPath(Class<?> clazz, String elPath) {
        PathTree node = database.find(clazz).select(elPath).orderBy(elPath + " desc").setMaxRows(1).findSingleAttribute();
        if (node == null) {
            return "00";
        } else {
            String path = node.getPath();
            int i = path.indexOf('.');
            if (i > 0) {
                return path.substring(0, i);
            } else {
                return path;
            }
        }
    }


    /**
     * 删除节点
     *
     * @param clazz
     * @param width
     * @param leftVal
     * @param rightVal
     * @param <T>
     * @return
     */
    private <T> int updateWhenDelete(Class<T> clazz, int width, int leftVal, int rightVal) {
        String table = clazz.getSimpleName();
        String sql = String.format("update %s set rightVal=rightVal-%s,leftVal=CASE WHEN leftVal>%s THEN leftVal=leftVal-%s ELSE leftVal END where rightVal>%s", table, width, rightVal, width, leftVal);

        int rowCount = database.createUpdate(clazz, sql).execute();
        return rowCount;
    }

    /**
     * 删除节点
     *
     * @param clazz
     * @param width
     * @param parentRight
     * @param <T>
     * @return
     */
    private <T> int updateWhenInsert(Class<T> clazz, int width, int parentRight) {
        String table = clazz.getSimpleName();
        String updateRight = String.format("update %s set rightVal = rightVal + %s where rightVal >= %s", table, width, parentRight);
        String updateLeft = String.format("update %s set leftVal = leftVal + %s where leftVal > %s", table, width, parentRight);

        int rowRight = database.createUpdate(clazz, updateRight).execute();
        int rowLeft = database.createUpdate(clazz, updateLeft).execute();
        return rowRight + rowLeft;
    }

    /**
     * 新增节点
     *
     * @param parent
     * @param name
     * @return
     */
    private NestedTree insertTreeNode(NestedTree parent, String name) {
        int rightVal = parent.getRightVal();
        int width = 2;
        NestedTree node = new NestedTree();
        node.setName(name);
        node.setLeftVal(rightVal);
        node.setRightVal(rightVal + 1);
        updateWhenInsert(NestedTree.class, width, rightVal);
        parent.setRightVal(parent.getRightVal() + width);

        database.save(node);

        return node;
    }

    private void deleteNode(NestedTree node) {
        database.delete(node);
        //updateWhenDelete(node.getClass(), 2, node.getLeftVal(), node.getRightVal());
    }


    @Test
    public void testInsertTree() {
        NestedTree node = new NestedTree();
        node.setName("Root");
        node.setLeftVal(1);
        node.setRightVal(2);
        database.save(node);

        NestedTree node1 = insertTreeNode(node, "节点1");
        NestedTree node2 = insertTreeNode(node, "节点2");
        NestedTree node3 = insertTreeNode(node, "节点3");
        NestedTree node4 = insertTreeNode(node, "节点4");

        deleteNode(node2);

        List<NestedTree> data = database.find(NestedTree.class).findList();
        System.out.println(data);
    }

    @Test
    public void testDDLDiffer() {
        SpiEbeanServer server = (SpiEbeanServer) database;
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<migration xmlns=\"http://ebean-orm.github.io/xml/ns/dbmigration\">\n" +
                "    <changeSet type=\"apply\">\n" +
                "        <createTable name=\"entity_field\" pkName=\"pk_entity_field\">\n" +
                "            <column name=\"id\" type=\"bigint\" primaryKey=\"true\"/>\n" +
                "            <column name=\"name\" type=\"varchar\"/>\n" +
                "            <column name=\"label\" type=\"varchar\"/>\n" +
                "            <column name=\"type\" type=\"varchar\"/>\n" +
                "            <column name=\"field_type\" type=\"varchar\"/>\n" +
                "            <column name=\"custom\" type=\"boolean\"/>\n" +
                "            <column name=\"uniquable\" type=\"boolean\"/>\n" +
                "            <column name=\"encrypted\" type=\"boolean\"/>\n" +
                "            <column name=\"disabled\" type=\"boolean\"/>\n" +
                "            <column name=\"nullable\" type=\"boolean\"/>\n" +
                "            <column name=\"required\" type=\"boolean\"/>\n" +
                "            <column name=\"creatable\" type=\"boolean\"/>\n" +
                "            <column name=\"updatable\" type=\"boolean\"/>\n" +
                "            <column name=\"sortable\" type=\"boolean\"/>\n" +
                "            <column name=\"min_length\" type=\"integer\"/>\n" +
                "            <column name=\"max_length\" type=\"integer\"/>\n" +
                "            <column name=\"minimum\" type=\"decimal\"/>\n" +
                "            <column name=\"maximum\" type=\"int\"/>\n" +
                "       </createTable>\n" +
                "    </changeSet>\n" +
                "</migration>";
        ModelContainer oldModel = new ModelContainer();
        Migration migration = MigrationXmlReader.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        MigrationVersion version = MigrationVersion.parse("1.0");
        oldModel.apply(migration, version);

        ModelContainer model = new ModelContainer();
        ModelBuildContext context = new ModelBuildContext(model, server.databasePlatform(), server.config().getConstraintNaming(), true);
        ModelBuildBeanVisitor visitor = new ModelBuildBeanVisitor(context);
        VisitSingleUsing visit = new VisitSingleUsing(visitor, server);
        visit.visitBean(server.descriptor(EntityField.class));
        // adjust the foreign keys on the 'draft' tables
        context.adjustDraftReferences();


        ModelDiff diff = new ModelDiff(oldModel);
        diff.compareTo(model);

        ChangeSet changeSet = diff.getApplyChangeSet();

        DdlHandler handler = PlatformDdlBuilder.create(server.databasePlatform()).createDdlHandler(server.config());
        DdlOptions ddlOptions = new DdlOptions();
        ddlOptions.setForeignKeySkipCheck(true);
        DdlWrite writer = new DdlWrite(new MConfiguration(), model, new DdlOptions());
        handler.generateProlog(writer);
        handler.generate(writer, changeSet);
        handler.generateEpilog(writer);

        System.out.println(writer);
    }

    @Test
    public void testDDLMigrationAll() throws IOException {
        DbMigration migration = DbMigration.create();
        migration.setPlatform(Platform.MYSQL);
        migration.setPathToResources("src/test/resources");
        migration.generateMigration();
    }

    @Test
    public void testDDLMigration() throws Exception {
        SpiEbeanServer server = (SpiEbeanServer) database;
        ModelContainer model = new ModelContainer();
        ModelBuildContext context = new ModelBuildContext(model, server.databasePlatform(), server.config().getConstraintNaming(), true);
        ModelBuildBeanVisitor visitor = new ModelBuildBeanVisitor(context);
        VisitSingleUsing visit = new VisitSingleUsing(visitor, server);
        visit.visitBean(server.descriptor(ObjectBean.class));
        // adjust the foreign keys on the 'draft' tables
        context.adjustDraftReferences();

        ModelDiff diff = new ModelDiff();
        diff.compareTo(model);

        JAXBContext jaxbContext = JAXBContext.newInstance(Migration.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

        marshaller.marshal(diff.getMigration(), System.out);
    }

    @Test
    public void testDDLGenerate() {
        SpiEbeanServer server = (SpiEbeanServer) database;
        CurrentModel currentModel = new CurrentModel(server);
//        System.out.println(currentModel.getCreateDdl());

        ModelContainer model = new ModelContainer();
        ModelBuildContext context = new ModelBuildContext(model, server.databasePlatform(), server.config().getConstraintNaming(), true);
        ModelBuildBeanVisitor visitor = new ModelBuildBeanVisitor(context);
        VisitSingleUsing visit = new VisitSingleUsing(visitor, server);
        visit.visitBean(server.descriptor(EntityField.class));
        // adjust the foreign keys on the 'draft' tables
        context.adjustDraftReferences();


        ModelDiff diff = new ModelDiff();
        diff.compareTo(model);
        ChangeSet changeSet = diff.getApplyChangeSet();

        DdlHandler handler = PlatformDdlBuilder.create(server.databasePlatform()).createDdlHandler(server.config());
        DdlOptions ddlOptions = new DdlOptions();
        ddlOptions.setForeignKeySkipCheck(true);
        DdlWrite writer = new DdlWrite(new MConfiguration(), model, new DdlOptions());
        handler.generateProlog(writer);
        handler.generate(writer, changeSet);
        handler.generateEpilog(writer);

        System.out.println(writer);
    }

    @Test
    public void testEntityMapping() {
        EntityMapping entity = new EntityMapping();
        entity.setId(1L);
        entity.setName("OrderInfo");
        entity.setLabel("订单");

        database.save(entity);

        EntityField field = new EntityField();
        field.setName("name");
        field.setLabel("订单名称");
        field.setType("String");
        field.setEntity(entity);

        EntityAssoc assoc = new EntityAssoc();
        assoc.setEntity(entity);
        assoc.setRefer(entity);
        assoc.setField(field);
        field.setAssoc(assoc);


        database.save(field);
//        database.delete(field);
        entity = database.find(EntityMapping.class, 1);
        entity.setLabel("订单2");
        database.save(entity);

//        entity.getFields().forEach(System.out::println);
//        entity.getAssocs().forEach(System.out::println);
    }
}
