package com.gitssie.openapi.page;

import org.apache.commons.lang3.StringUtils;

/**
 * @author: Awesome
 * @create: 2024-04-09 16:52
 */
public class ModelAssoc {
    protected String apiKey;
    protected String name;
    protected String type;
    protected String mappedBy;
    protected Object filter; //filter function

    public ModelAssoc(String apiKey, String name, String type, String mappedBy,Object filter) {
        this.apiKey = apiKey;
        this.name = name;
        this.type = type;
        this.mappedBy = mappedBy;
        this.filter = filter;
    }

    public boolean isMany() {
        return StringUtils.equalsIgnoreCase("many", type);
    }

    public boolean isOne() {
        return StringUtils.equalsIgnoreCase("one", type);
    }

    public boolean isMap() {
        return StringUtils.equalsIgnoreCase("map", type);
    }
}
