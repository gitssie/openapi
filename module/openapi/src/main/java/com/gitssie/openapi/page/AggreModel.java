package com.gitssie.openapi.page;

import com.gitssie.openapi.models.layout.Component;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.Function;

import java.util.List;
import java.util.Map;

/**
 * @author: Awesome
 * @create: 2024-03-29 10:53
 */
public class AggreModel {
    private Class<?> dtoClass = Map.class;
    private List<String> aggre;
    private String type = "map"; //map/list/page
    private Component component;
    private Function rawSql; //原始SQL

    public AggreModel(List<String> aggre) {
        this.aggre = aggre;
    }

    public AggreModel(List<String> aggre, Component component) {
        this.aggre = aggre;
        this.component = component;
    }

    public Class<?> getDtoClass() {
        return dtoClass;
    }

    public void setDtoClass(Class<?> dtoClass) {
        this.dtoClass = dtoClass;
    }

    public List<String> getAggre() {
        return aggre;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Function getRawSql() {
        return rawSql;
    }

    public void setRawSql(Function rawSql) {
        this.rawSql = rawSql;
    }

    public boolean isMap() {
        return StringUtils.startsWith(type,"map");
    }


    public boolean isMapOne(){
        return StringUtils.equals("mapOne",type);
    }

    public boolean isList() {
        return StringUtils.equals("list", type);
    }

    public boolean isPage() {
        return StringUtils.equals("page", type);
    }

    public Component getComponent() {
        return component;
    }

    public boolean isRawSql() {
        return rawSql != null;
    }

    public int timeout(){
        return 0;
    }
}
