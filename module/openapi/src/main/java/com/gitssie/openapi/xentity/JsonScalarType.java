package com.gitssie.openapi.xentity;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import io.ebean.core.type.DataBinder;
import io.ebean.core.type.DataReader;
import io.ebean.core.type.DocPropertyType;
import io.ebean.core.type.ScalarType;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.List;

public class JsonScalarType implements ScalarType {
    private Class<?> type = List.class;
    private Type genericType = Object.class;

    public JsonScalarType() {}

    public JsonScalarType(Class<?> type, Type genericType) {
        this.type = type;
        this.genericType = genericType;
    }

    @Override
    public boolean isBinaryType() {
        return false;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public boolean isDirty(Object value) {
        return false;
    }

    @Override
    public int getLength() {
        return 0;
    }

    @Override
    public boolean isJdbcNative() {
        return false;
    }

    @Override
    public int getJdbcType() {
        return 0;
    }

    @Override
    public Class getType() {
        return type;
    }

    public Type getGenericType() {
        return genericType;
    }

    @Override
    public Object read(DataReader reader) throws SQLException {
        return null;
    }

    @Override
    public void loadIgnore(DataReader reader) {

    }

    @Override
    public void bind(DataBinder binder, Object value) throws SQLException {

    }

    @Override
    public Object toJdbcType(Object value) {
        return null;
    }

    @Override
    public Object toBeanType(Object value) {
        return null;
    }

    @Override
    public String formatValue(Object value) {
        return null;
    }

    @Override
    public String format(Object value) {
        return null;
    }

    @Override
    public Object parse(String value) {
        return null;
    }

    @Override
    public DocPropertyType getDocType() {
        return null;
    }

    @Override
    public boolean isDateTimeCapable() {
        return false;
    }

    @Override
    public long asVersion(Object value) {
        return 0;
    }

    @Override
    public Object convertFromMillis(long dateTime) {
        return null;
    }

    @Override
    public Object readData(DataInput dataInput) throws IOException {
        return null;
    }

    @Override
    public void writeData(DataOutput dataOutput, Object v) throws IOException {

    }

    @Override
    public Object jsonRead(JsonParser parser) throws IOException {
        return null;
    }

    @Override
    public void jsonWrite(JsonGenerator writer, Object value) throws IOException {

    }
}
