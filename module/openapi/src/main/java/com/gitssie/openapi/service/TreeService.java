package com.gitssie.openapi.service;

import com.gitssie.openapi.models.tree.PathTree;
import com.gitssie.openapi.ebean.PathTreeEmbed;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.ebean.Database;
import io.ebean.plugin.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class TreeService {
    @Autowired
    private Database database;
    @Autowired
    private PathTreeEmbed pathTreeEmbed;

    public <T> List<Map<String, Object>> toTree(Class<T> clazz, List<T> dataList, Function<T, PathTree> path, Function<T, Map<String, Object>> toMap) {
        Map<String, Map<String, Object>> groupedMap = Maps.newHashMapWithExpectedSize(dataList.size());
        List<Map<String, Object>> result = Lists.newLinkedList();
        dataList.sort((a, b) -> {
            PathTree pa = path.apply(a);
            PathTree pb = path.apply(b);
            if (pa == null || pb == null) {
                return -1;
            }
            if (pa.getLevel() > pb.getLevel()) {
                return 1;
            } else if (pa.getLevel() < pb.getLevel()) {
                return -1;
            } else {
                return 0;
            }
        });
        //从父级开始处理
        Map<String, Object> map, parent;
        PathTree cur;
        String p;
        for (T data : dataList) {
            cur = path.apply(data);
            if (cur == null) {
                continue;
            }
            map = toMap.apply(data);
            p = cur.parentPath();
            if (p == null) {
                result.add(map);
                groupedMap.put(cur.getPath(), map);
            } else {
                groupedMap.put(cur.getPath(), map);
                parent = groupedMap.get(p);
                if (parent != null) {
                    addChildren(parent, map);
                } else {
                    result.add(map);
                }
            }
        }
        return result;
    }


    public <T> List<Map<String, Object>> toTree(Class<T> clazz, List<T> dataList, Property parentProp, Function<T, Map<String, Object>> toMap) {
        List<T> parentList = database.filter(clazz).isNull(parentProp.name()).filter(dataList);
        Map<T, Map<String, Object>> groupedMap = Maps.newHashMapWithExpectedSize(dataList.size());
        List<Map<String, Object>> result = Lists.newLinkedList();
        Map<String, Object> map, parent;
        //父节点
        for (T data : parentList) {
            map = toMap.apply(data);
            groupedMap.put(data, map);
            result.add(map);
        }

        //子节点
        T dataParent;
        for (T data : dataList) {
            if (groupedMap.containsKey(data)) {
                continue;
            }
            map = toMap.apply(data);
            groupedMap.put(data, map);
            dataParent = (T) parentProp.value(data);
            parent = groupedMap.get(dataParent);
            if (parent == null && dataParent != null) {
                parent = toMap.apply(dataParent);
                groupedMap.put(dataParent, parent);
            }
            addChildren(parent, map);
        }
        return result;
    }

    private void addChildren(Map<String, Object> parent, Map<String, Object> child) {
        if (child == null) {
            return;
        }
        List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
        if (children == null) {
            children = new LinkedList<>();
            parent.put("children", children);
        }
        children.add(child);
    }

    public <T> List<Map<String, Object>> toPlanTree(List<T> roles, Function<T, Map<String, Object>> toMap) {
        List<Map<String, Object>> result = Lists.newLinkedList();
        Map<String, Object> map;
        for (T role : roles) {
            map = toMap.apply(role);
            result.add(map);
        }
        return result;
    }

    public <T> List<T> children(Class<T> clazz, String elPath, PathTree parent) {
        return pathTreeEmbed.children(database, clazz, elPath, parent);
    }
}
