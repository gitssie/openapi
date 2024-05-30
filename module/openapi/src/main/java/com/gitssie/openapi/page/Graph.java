package com.gitssie.openapi.page;

import com.gitssie.openapi.service.Provider;
import io.ebean.plugin.BeanType;
import io.ebeaninternal.server.deploy.BeanDescriptor;
import io.ebeaninternal.server.deploy.BeanProperty;
import io.ebeaninternal.server.deploy.BeanPropertyAssoc;
import io.vavr.Function3;
import io.vavr.control.Option;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public class Graph<T extends Graph> {
    protected final Model model;
    protected final BeanType<?> desc;
    protected final Provider provider;
    protected final AssocType assocType;
    protected List<T> next;
    protected BeanProperty mapped;
    protected BeanProperty mappedBy;
    protected String mapProperty;

    public Graph(Model model, BeanType<?> desc, Provider provider, AssocType assocType) {
        this.model = model;
        this.desc = desc;
        this.provider = provider;
        this.assocType = assocType;
    }

    public void setMapped(BeanProperty mapped) {
        this.mapped = mapped;
    }

    public void setMappedBy(BeanProperty mappedBy) {
        this.mappedBy = mappedBy;
    }

    protected void setMapProperty(String mapProperty) {
        this.mapProperty = mapProperty;
    }

    protected T addJoin(String name, Model model, BeanType<?> childDesc, Provider provider, Function<AssocType, T> createNode) {
        BeanDescriptor desc = (BeanDescriptor) childDesc;
        AssocType assocType = AssocType.MANY;
        ModelAssoc anno = model.assocAnnotation;
        BeanProperty mapped = null;
        BeanProperty mappedBy = null;
        if (anno != null) {
            if (anno.isOne()) {
                assocType = AssocType.ONE;
            }
            if (StringUtils.isNotEmpty(anno.mappedBy)) {
                mappedBy = (BeanProperty) desc.property(anno.mappedBy);
            }
            if (StringUtils.isNotEmpty(anno.name)) {
                mapped = (BeanProperty) this.desc.property(anno.name);
            }
        }
        if (mappedBy == null) {
            for (BeanProperty beanProperty : desc.propertiesOne()) {
                if (beanProperty.type().equals(this.desc.type())) {
                    if (mappedBy != null) {
                        throw new IllegalArgumentException("join fetch property " + name + ",mappedBy have multiple");
                    }
                    mappedBy = beanProperty;
                }
            }
        }
        if (mappedBy == null) {
            throw new IllegalArgumentException("join fetch property " + name + ",mappedBy is empty");
        }
        if (mapped == null) {
            mapped = (BeanProperty) this.desc.idProperty();
        }
        T node = createNode.apply(assocType);
        node.setMapped(mapped);
        node.setMappedBy(mappedBy);
        node.setMapProperty(StringUtils.defaultString(name, mappedBy.name()));
        addNode(node);
        return node;
    }

    protected void addNode(T node) {
        if (next == null) {
            next = new LinkedList<>();
        }
        next.add(node);
    }
}
