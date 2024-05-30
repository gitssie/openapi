package com.gitssie.openapi.xentity;

import io.ebeaninternal.server.deploy.parse.tenant.XEntity;
import io.ebeaninternal.server.deploy.parse.tenant.XField;
import io.vavr.Lazy;
import io.vavr.control.Option;

import java.util.function.Consumer;

/**
 * @author: Awesome
 * @create: 2024-03-07 15:02
 */
public class XEntityCache {
    public final XEntity desc;
    public final Long entityId;
    private Option<Lazy<Boolean>> loadClassFunction = Option.none();
    private Option<Lazy<Boolean>> loadAssocFunction = Option.none();

    public XEntityCache(XEntity desc, Long entityId) {
        this.desc = desc;
        this.entityId = entityId;
    }

    public Class<?> getBeanType() {
        loadClass();
        return desc.getBeanType();
    }

    public String getName() {
        return desc.getName();
    }


    public XField getNameable() {
        return desc.getNameable();
    }

    protected void setLoadClass(Consumer<XEntityCache> loadClassFunction) {
        this.loadClassFunction = Option.of(Lazy.of(() -> {
            loadClassFunction.accept(this);
            return true;
        }));
    }

    protected void setLoadAssoc(Consumer<XEntityCache> loadAssocFunction) {
        this.loadAssocFunction = Option.of(Lazy.of(() -> {
            loadAssocFunction.accept(this);
            return true;
        }));
    }

    protected void loadClass() {
        loadClassFunction.forEach(e -> e.get());
    }

    protected void loadAssoc() {
        loadAssocFunction.forEach(e -> e.get());
    }

    public void loadAll(){
        loadClass();
        loadAssoc();
    }
}
