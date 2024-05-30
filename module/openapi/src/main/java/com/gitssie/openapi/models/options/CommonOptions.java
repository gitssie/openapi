package com.gitssie.openapi.models.options;

import com.gitssie.openapi.models.BasicDomain;
import io.ebean.annotation.DbJson;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "common_options")
public class CommonOptions extends BasicDomain {
    private String name;
    private String code;

    @DbJson
    private List<Map<String,Object>> options;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<Map<String, Object>> getOptions() {
        return options;
    }

    public void setOptions(List<Map<String, Object>> options) {
        this.options = options;
    }
}
