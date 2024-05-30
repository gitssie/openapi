package com.gitssie.openapi.ebean.dbmigration;

import io.ebean.config.DatabaseConfig;
import io.ebeaninternal.dbmigration.ddlgeneration.platform.PlatformDdl;

public class BaseDdlHandler extends io.ebeaninternal.dbmigration.ddlgeneration.BaseDdlHandler {

    public BaseDdlHandler(DatabaseConfig config, PlatformDdl platformDdl) {
        super(config, platformDdl, new NoForeignKeyTableDdl(config, platformDdl));
    }
}
