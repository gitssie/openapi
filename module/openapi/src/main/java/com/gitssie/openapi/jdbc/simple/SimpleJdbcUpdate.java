/*
 * Copyright 2002-2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gitssie.openapi.jdbc.simple;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Map;

/**
 * A SimpleJdbcUpdate is a multi-threaded, reusable object providing easy update
 * capabilities for a table. It provides meta data processing to simplify the
 * code needed to construct a basic update statement. All you need to provide is
 * the name of the table and a Map containing the column names and the column
 * values.
 * 
 * <p>
 * The meta data processing is based on the DatabaseMetaData provided by the
 * JDBC driver. As long as the JBDC driver can provide the names of the columns
 * for a specified table than we can rely on this auto-detection feature. If
 * that is not the case then the column names must be specified explicitly.
 * 
 * <p>
 * The actual update is being handled using Spring's
 * {@link JdbcTemplate}.
 * 
 * <p>
 * Many of the configuration methods return the current instance of the
 * SimpleJdbcUpdate to provide the ability to string multiple ones together in a
 * "fluid" interface style.
 * 
 * @author Thomas Risberg
 * @author Florent Paillard
 * @since 3.0.5
 * @see java.sql.DatabaseMetaData
 * @see JdbcTemplate
 */
public class SimpleJdbcUpdate extends AbstractJdbcUpdate implements SimpleJdbcUpdateOperations {

	/**
	 * Constructor that takes one parameter with the JDBC DataSource to use when
	 * creating the JdbcTemplate.
	 * 
	 * @param dataSource
	 *            the <code>DataSource</code> to use
	 * @see JdbcTemplate#setDataSource
	 */
	public SimpleJdbcUpdate(DataSource dataSource) {
		super(dataSource);
	}

	/**
	 * Alternative Constructor that takes one parameter with the JdbcTemplate to
	 * be used.
	 * 
	 * @param jdbcTemplate
	 *            the <code>JdbcTemplate</code> to use
	 * @see JdbcTemplate#setDataSource
	 */
	public SimpleJdbcUpdate(JdbcTemplate jdbcTemplate) {
		super(jdbcTemplate);
	}

	public SimpleJdbcUpdate withTableName(String tableName) {
		setTableName(tableName);
		return this;
	}

	public SimpleJdbcUpdate withSchemaName(String schemaName) {
		setSchemaName(schemaName);
		return this;
	}

	public SimpleJdbcUpdate withCatalogName(String catalogName) {
		setCatalogName(catalogName);
		return this;
	}

	public SimpleJdbcUpdate updatingColumns(String... columnNames) {
		setDeclaredUpdatingColumns(Arrays.asList(columnNames));
		return this;
	}

	public SimpleJdbcUpdate restrictingColumns(String... columnNames) {
		setRestrictingColumns(Arrays.asList(columnNames));
		return this;
	}
	
   public SimpleJdbcUpdate restrictingColumns(Map<String, Operator> columnsToOps) {
        setRestrictingColumns(columnsToOps);
        return this;
    }

	public SimpleJdbcUpdateOperations withoutTableColumnMetaDataAccess() {
		setAccessTableColumnMetaData(false);
		return this;
	}

	public SimpleJdbcUpdateOperations includeSynonymsForTableColumnMetaData() {
		setOverrideIncludeSynonymsDefault(true);
		return this;
	}

	public int execute(Map<String, Object> updatingValues, Map<String, Object> restrictingValues) {
		return doExecute(updatingValues, restrictingValues);
	}

	public int execute(SqlParameterSource updatingValues, SqlParameterSource restrictingValues) {
		return doExecute(updatingValues, restrictingValues);
	}

}
