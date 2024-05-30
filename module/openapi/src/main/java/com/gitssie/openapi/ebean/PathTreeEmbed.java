package com.gitssie.openapi.ebean;

import com.gitssie.openapi.models.tree.PathTree;
import io.ebean.Database;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PathTreeEmbed {

    public PathTree createNode(Database database, Class<?> clazz, String elPath) {
        return createNode(database, clazz, elPath, null);
    }

    public PathTree createNode(Database database, Class<?> clazz, String elPath, PathTree parent) {
        int level = 1;
        String path;
        if (parent == null) {
            path = findDeeplyRootPath(database, clazz, elPath);
        } else {
            path = findDeeplyChildPath(database, clazz, elPath, parent);
            level = parent.getLevel() + 1;
        }
        path = nextPath(path);
        PathTree node = new PathTree(path, level);
        return node;
    }

    private String nextPath(String root) {
        String prefix = "";
        String path = root;
        int i = path.lastIndexOf('.');
        if (i > 0) {
            prefix = path.substring(0, i + 1);
            path = path.substring(i + 1);
        }
        int pathAsInt = Integer.parseInt(path, 16);
        pathAsInt++;
        if (pathAsInt > 0xFF) { //超过最大值
            throw new IllegalStateException("too many children path at root " + root);
        }
        return prefix + String.format("%02X", pathAsInt);
    }

    public <T> List<T> children(Database database, Class<T> clazz, String elPath, PathTree parent) {
        String prefix = parent.getPath() + '.';
        return database.find(clazz).select(elPath).where().startsWith(elPath, prefix).findList();
    }

    public boolean hasChildren(Database database, Class<?> clazz, String elPath, PathTree parent) {
        String prefix = parent.getPath() + '.';
        return database.find(clazz).select(elPath).where().startsWith(elPath, prefix).orderBy(elPath + " desc").setMaxRows(1).exists();
    }

    public boolean exists(Database database, Class<?> clazz, String elPath, String path) {
        return database.find(clazz).select(elPath).where().eq(elPath, path).exists();
    }

    public String findDeeplyChildPath(Database database, Class<?> clazz, String elPath, PathTree parent) {
        String prefix = parent.getPath() + '.';
        Object node = database.find(clazz).select(elPath).where().startsWith(elPath, prefix).orderBy(elPath + " desc").setMaxRows(1).findSingleAttribute();
        if (node == null) {
            return prefix + "00";
        } else {
            String path;
            if (node instanceof PathTree) {
                path = ((PathTree) node).getPath();
            } else {
                path = (String) node;
            }
            //这里可能会查询出子级下来,所以需要进行截取
            int i = path.indexOf('.', parent.getPath().length() + 1);
            if (i > 0) {
                return path.substring(0, i);
            } else {
                return path;
            }
        }
    }

    public String findDeeplyRootPath(Database database, Class<?> clazz, String elPath) {
        Object node = database.find(clazz).select(elPath).orderBy(elPath + " desc").setMaxRows(1).findSingleAttribute();
        if (node == null) {
            return "00";
        } else {
            String path;
            if (node instanceof PathTree) {
                path = ((PathTree) node).getPath();
            } else {
                path = (String) node;
            }
            int i = path.indexOf('.');
            if (i > 0) {
                return path.substring(0, i);
            } else {
                return path;
            }
        }
    }

}
