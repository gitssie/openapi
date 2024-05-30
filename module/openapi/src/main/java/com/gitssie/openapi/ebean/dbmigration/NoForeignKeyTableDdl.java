package com.gitssie.openapi.ebean.dbmigration;

import io.ebean.config.DatabaseConfig;
import io.ebeaninternal.dbmigration.ddlgeneration.DdlBuffer;
import io.ebeaninternal.dbmigration.ddlgeneration.DdlWrite;
import io.ebeaninternal.dbmigration.ddlgeneration.platform.BaseTableDdl;
import io.ebeaninternal.dbmigration.ddlgeneration.platform.PlatformDdl;
import io.ebeaninternal.dbmigration.migration.*;
import org.apache.commons.lang3.StringUtils;

public class NoForeignKeyTableDdl extends BaseTableDdl {

    public NoForeignKeyTableDdl(DatabaseConfig config, PlatformDdl platformDdl) {
        super(config, platformDdl);
    }

    @Override
    protected void dropTable(DdlBuffer buffer, String tableName) {
        // no nothing
    }

    @Override
    public void generate(DdlWrite writer, AlterForeignKey alterForeignKey) {
        // do nothing
    }

    @Override
    protected void alterColumnAddForeignKey(DdlWrite writer, AlterColumn alterColumn) {
        String indexName = alterColumn.getForeignKeyIndex();
        if (StringUtils.isEmpty(indexName)) {
            return;
        }
        CreateIndex index = new CreateIndex();
        index.setIndexName(indexName);
        index.setTableName(alterColumn.getTableName());
        index.setColumns(alterColumn.getColumnName());
        generate(writer, index);
    }

    @Override
    protected void alterColumnDropForeignKey(DdlWrite writer, AlterColumn alter) {
        String indexName = alter.getForeignKeyIndex();
        if (StringUtils.isEmpty(indexName)) {
            return;
        }
        DropIndex index = new DropIndex();
        index.setIndexName(indexName);
        index.setTableName(alter.getTableName());
        generate(writer, index);
    }

    @Override
    protected void writeAddCompoundForeignKeys(DdlWrite writer, CreateTable createTable) {
        // do nothing
    }

    @Override
    protected void writeForeignKey(DdlWrite writer, String tableName, Column column) {
        String indexName = column.getForeignKeyIndex();
        if (StringUtils.isEmpty(indexName)) {
            return;
        }
        CreateIndex index = new CreateIndex();
        index.setIndexName(indexName);
        index.setTableName(tableName);
        index.setColumns(column.getName());
        generate(writer, index);
    }
}
