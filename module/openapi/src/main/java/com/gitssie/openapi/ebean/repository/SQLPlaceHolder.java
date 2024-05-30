package com.gitssie.openapi.ebean.repository;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.TemplateCharacters;
import org.mozilla.javascript.ast.TemplateLiteral;

public class SQLPlaceHolder implements NodeVisitor {

    private final StringBuilder buf = new StringBuilder();
    private String scope = "this";
    private String funcName = SQLParameterFunction.PARAMETER_FUNC_NAME;


    public SQLPlaceHolder() {
    }

    public SQLPlaceHolder(String scope) {
        this.scope = scope;
    }

    public SQLPlaceHolder(String scope, String funcName) {
        this.scope = scope;
        this.funcName = funcName;
    }

    private String replace(String raw) {
        buf.setLength(0);
        replace(0, buf, raw);
        return buf.toString();
    }

    private void replace(int s, StringBuilder buf, String raw) {
        int i = raw.indexOf('#', s);
        int j = raw.indexOf('{', i);
        int k = raw.indexOf('}', j);
        if (i >= s && j > i && k > j) {
            append(buf, raw, s, i);
            buf.append("${").append(scope).append(".").append(funcName).append("(");
            append(buf, raw, j + 1, k);
            buf.append(")}");
            replace(k + 1, buf, raw);
        } else {
            append(buf, raw, s, raw.length());
        }
    }

    private void append(StringBuilder buf, String raw, int i, int j) {
        for (; i < j; i++) {
            if (raw.charAt(i) != '\n') {
                buf.append(raw.charAt(i));
            }
        }
    }

    @Override
    public boolean visit(AstNode node) {
        if (node instanceof TemplateLiteral) {
            TemplateLiteral tp = (TemplateLiteral) node;
            for (AstNode element : tp.getElements()) {
                if (element instanceof TemplateCharacters) {
                    TemplateCharacters cp = (TemplateCharacters) element;
                    cp.setRawValue(replace(cp.getRawValue()));
                }
            }
        }
        return true;
    }
}
