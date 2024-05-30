package com.gitssie.openapi.ebean.repository;

import com.gitssie.openapi.ebean.SqlRowMap;
import com.gitssie.openapi.page.FetchContext;
import com.gitssie.openapi.utils.NamingUtils;
import com.google.common.collect.Maps;
import io.ebeaninternal.server.query.DefaultSqlRow;
import io.ebeaninternal.server.type.ScalarTypeString;
import io.vavr.control.Option;
import org.springframework.data.jdbc.support.JdbcUtil;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.lang.Nullable;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

public class ColumnMapRowMapper implements RowMapper<Map<String, Object>> {
    private String[] columnNames;
    private boolean camelCase;

    public ColumnMapRowMapper(boolean camelCase) {
        this.camelCase = camelCase;
    }

    @Override
    public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
        if (columnNames == null) {
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            columnNames = new String[columnCount];
            StringBuilder buf = camelCase ? new StringBuilder() : null;
            for (int i = 1; i <= columnCount; i++) {
                String column = JdbcUtils.lookupColumnName(rsmd, i);
                if (camelCase) {
                    column = NamingUtils.toCamelCase(buf, column);
                }
                columnNames[i - 1] = column;
            }

        }
        Map<String, Object> mapOfColumnValues = createColumnMap(columnNames.length);
        for (int i = 1; i <= columnNames.length; i++) {
            mapOfColumnValues.put(columnNames[i - 1], getColumnValue(rs, i));
        }
        return mapOfColumnValues;
    }

    protected Map<String, Object> createColumnMap(int columnCount) {
        int estCap = (int) (columnCount / 0.7f) + 1;
        SqlRowMap ret = new SqlRowMap(estCap, 0.75f, "true", true);
        return ret;
    }

    @Nullable
    protected Object getColumnValue(ResultSet rs, int index) throws SQLException {
        return JdbcUtils.getResultSetValue(rs, index);
    }

}
