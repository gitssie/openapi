package com.gitssie.openapi.rule;

import org.mozilla.javascript.Scriptable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author: Awesome
 * @create: 2024-05-17 13:38
 */
public class RuleProxyMap implements Scriptable, Map<String, Object> {
    protected Map<String, Object> map;

    public RuleProxyMap(Map<String, Object> map) {
        this.map = map;
    }

    public RuleProxyMap() {
        this(new HashMap<>());
    }

    @Override
    public String getClassName() {
        return "JavaProxyMap";
    }

    @Override
    public Object get(String name, Scriptable start) {
        return map.get(name);
    }

    @Override
    public Object get(int index, Scriptable start) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean has(String name, Scriptable start) {
        return true;
    }

    @Override
    public boolean has(int index, Scriptable start) {
        return true;
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        map.put(name, value);
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        System.out.println("xx");
    }

    @Override
    public void delete(String name) {
        map.remove(name);
    }

    @Override
    public void delete(int index) {

    }

    @Override
    public Scriptable getPrototype() {
        return this;
    }

    @Override
    public void setPrototype(Scriptable prototype) {
    }

    @Override
    public Scriptable getParentScope() {
        return this;
    }

    @Override
    public void setParentScope(Scriptable parent) {
    }

    @Override
    public Object[] getIds() {
        return map.keySet().toArray();
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        return null;
    }

    @Override
    public boolean hasInstance(Scriptable instance) {
        return instance instanceof RuleProxyMap;
    }

    // ------------------------------------
    // Normal map methods...

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return map.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return map.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return map.remove(key);
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        return map.entrySet();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public Set<String> keySet() {
        return map.keySet();
    }

    @Override
    public void putAll(Map<? extends String, ?> t) {
        map.putAll(t);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public Collection<Object> values() {
        return map.values();
    }
}
