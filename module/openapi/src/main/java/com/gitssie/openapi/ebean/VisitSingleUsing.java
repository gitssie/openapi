package com.gitssie.openapi.ebean;

import io.ebeaninternal.api.SpiEbeanServer;
import io.ebeaninternal.dbmigration.model.visitor.BeanVisitor;
import io.ebeaninternal.server.deploy.BeanDescriptor;
import io.ebeaninternal.server.deploy.visitor.BeanPropertyVisitor;
import io.ebeaninternal.server.deploy.visitor.VisitProperties;

import java.util.List;

/**
 * Makes use of BeanVisitor and PropertyVisitor to navigate BeanDescriptors
 * and their properties.
 */
public final class VisitSingleUsing extends VisitProperties {

    private final BeanVisitor visitor;

    private final List<BeanDescriptor<?>> descriptors;

    /**
     * Visit all the descriptors for a given server.
     */
    public VisitSingleUsing(BeanVisitor visitor, SpiEbeanServer server) {
        this(visitor, server.descriptors());
    }

    /**
     * Visit all the descriptors in the list.
     */
    public VisitSingleUsing(BeanVisitor visitor, List<BeanDescriptor<?>> descriptors) {
        this.visitor = visitor;
        this.descriptors = descriptors;
    }

    public void visitBean(BeanDescriptor<?> desc) {
        visitBean(desc, visitor);
    }

    /**
     * Visit the bean using a visitor.
     */
    protected void visitBean(BeanDescriptor<?> desc, BeanVisitor visitor) {
        BeanPropertyVisitor propertyVisitor = visitor.visitBean(desc);
        if (propertyVisitor != null) {
            visitProperties(desc, propertyVisitor);
        }
    }

}
