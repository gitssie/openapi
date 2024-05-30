package com.gitssie.openapi.models.layout;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Component {
    private String id;
    private String type;
    private String classes;
    private Map<String, Object> map;

    private List<Object> columns;
    private List<Component> components;

    public Component(Map<String, Object> map) {
        this.map = map;
    }

    public Component() {
        map = new HashMap<>();
    }

    public int size() {
        return map.size();
    }

    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }


    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }


    public Object get(Object key) {
        return map.get(key);
    }


    public Object put(String key, Object value) {
        return map.put(key, value);
    }


    public Object remove(Object key) {
        return map.remove(key);
    }


    public void putAll(Map<? extends String, ?> m) {
        map.putAll(m);
    }


    public void clear() {
        map.clear();
    }


    public Set<String> keySet() {
        return map.keySet();
    }


    public Collection<Object> values() {
        return map.values();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getClasses() {
        return classes;
    }

    public void setClasses(String classes) {
        this.classes = classes;
    }

    public List<Object> getColumns() {
        return columns;
    }

    public void setColumns(List<Object> columns) {
        this.columns = columns;
    }

    public List<Component> getComponents() {
        return components;
    }

    public void setComponents(List<Component> components) {
        this.components = components;
    }

    @JsonAnySetter
    void setOthers(String key, Object value) {
        map.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getOthers() {
        return map;
    }
}
