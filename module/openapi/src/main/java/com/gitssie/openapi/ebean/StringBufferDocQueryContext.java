package com.gitssie.openapi.ebean;

import io.ebean.Junction;
import io.ebean.LikeType;
import io.ebean.plugin.ExpressionPath;
import io.ebean.search.*;
import io.ebeaninternal.server.expression.DocQueryContext;
import io.ebeaninternal.server.expression.Op;

import java.io.IOException;
import java.util.*;

public class StringBufferDocQueryContext implements DocQueryContext {
    private StringBuilder buf = new StringBuilder();
    private StringBuilder temp = new StringBuilder();
    private List<Object> bindData = new LinkedList<>();
    private LinkedNode head;

    /**
     * AND OR 逻辑表达式的起始
     *
     * @param logicType
     * @throws IOException
     */
    @Override
    public void startBool(Junction.Type logicType) {
        LinkedNode node = new LinkedNode(logicType);
        node.next = head;
        head = node;
    }

    /**
     * AND OR 逻辑表达式的结束
     *
     * @throws IOException
     */
    @Override
    public void endBool() {
        if (head == null) {
            return;
        }
        LinkedNode next = head.next;
        if (next != null) {
            temp.append("(");
        }
        temp.append(head.left)
                .append(head.logicType.literal())
                .append(head.right);
        if (next != null) {
            temp.append(')');
        }

        //swap head.left temp
        head.right = head.left;
        head.left = temp;
        temp = head.right;
        temp.setLength(0);

        if (next != null) {
            if (next.left == null) {
                next.left = head.left;
            } else {
                next.right = head.left;
            }
            head = next;
        } else {
            buf.append(head.left);
        }
    }

    protected StringBuilder obtainBuffer() {
        if (head == null) {
            return buf;
        } else if (head.left == null) {
            head.left = new StringBuilder();
            return head.left;
        } else {
            head.right = new StringBuilder();
            return head.right;
        }
    }

    @Override
    public void writeSimple(Op type, String propertyName, Object value) throws IOException {
        obtainBuffer().append(propertyName).append(type.bind());
        addBindData(value);
    }

    private void addBindData(Object value){
        if (value instanceof String) {
            bindData.add("'" + value + "'");
        } else if (value instanceof Date) {
            bindData.add(((Date) value).getTime());
        } else {
            bindData.add(value);
        }
    }

    /**
     * between and 启始语句
     *
     * @throws IOException
     */
    @Override
    public void startBoolMust() throws IOException {
        //this.logicType = null;
        throw new UnsupportedOperationException();
    }

    @Override
    public void startBoolMustNot() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeEqualTo(String propertyName, Object value) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeIEqualTo(String propName, String value) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRange(String propertyName, String rangeType, Object value) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRange(String propertyName, Op lowOp, Object valueLow, Op highOp, Object valueHigh) throws IOException {
        StringBuilder buf = obtainBuffer();
        buf.append(propertyName).append(lowOp.bind());
        buf.append(Junction.Type.AND.literal());
        buf.append(propertyName).append(highOp.bind());

        addBindData(valueLow);
        addBindData(valueHigh);
    }

    @Override
    public void writeIn(String propertyName, Object[] values, boolean not) throws IOException {
        if(not) {
            throw new UnsupportedOperationException();
        }
        StringBuilder buf = obtainBuffer();
        buf.append(propertyName).append(" in(");
        for (Object value : values) {
            buf.append("?,");
            addBindData(value);
        }
        if(values.length > 0){
            buf.setLength(buf.length() - 1);
        }
        buf.append(')');
    }

    @Override
    public void writeIds(Collection<?> idCollection) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeId(Object value) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRaw(String raw, Object[] values) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeExists(boolean notNull, String propertyName) throws IOException {
        obtainBuffer().append(propertyName).append(" is not null ");
    }

    @Override
    public void writeAllEquals(Map<String, Object> propMap) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeLike(String propertyName, String val, LikeType type, boolean caseInsensitive) throws IOException {
        StringBuilder buf = obtainBuffer();
        buf.append(propertyName).append(" like '").append(val).append("%'");
    }

    @Override
    public void writeMatch(String propName, String search, Match options) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeMultiMatch(String search, MultiMatch options) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeTextSimple(String search, TextSimple options) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeTextCommonTerms(String search, TextCommonTerms options) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeTextQueryString(String search, TextQueryString options) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void startBoolGroup() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void startBoolGroupList(Junction.Type type) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void endBoolGroupList() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void endBoolGroup() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ExpressionPath getExpressionPath(String propName) {
        return null;
    }

    @Override
    public void startNested(String nestedPath) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void endNested() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void startNot() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void endNot() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        int j = 0;
        for (int i = 0; i < buf.length(); i++) {
            if (buf.charAt(i) == '?') {
                res.append(bindData.get(j++));
            } else {
                res.append(buf.charAt(i));
            }
        }
        return res.toString();
    }

    private static class LinkedNode {
        protected Junction.Type logicType;
        protected int level = 1;
        protected LinkedNode next;

        protected StringBuilder left;
        protected StringBuilder right;

        public LinkedNode(Junction.Type logicType) {
            this.logicType = logicType;
        }

    }
}
