/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.tools.xjc.model;

import javax.xml.namespace.QName;

import com.sun.tools.xjc.model.nav.NClass;
import com.sun.tools.xjc.model.nav.NType;
import cn.glassfish.jaxb.core.v2.model.core.AttributePropertyInfo;
import cn.glassfish.jaxb.core.v2.model.core.PropertyKind;
import com.sun.xml.xsom.XSComponent;
import com.sun.istack.Nullable;

import org.xml.sax.Locator;

/**
 * {@link AttributePropertyInfo} for the compiler.
 *
 * @author Kohsuke Kawaguchi
 */
public final class CAttributePropertyInfo extends CSingleTypePropertyInfo implements AttributePropertyInfo<NType,NClass> {

    private final QName attName;
    private final boolean isRequired;

    /**
     * @param type
     *      Represents the bound type of this attribute.
     * @param typeName
     *      XML Schema type name of this attribute. Optional for other schema languages.
     */
    public CAttributePropertyInfo(String name, XSComponent source, CCustomizations customizations,
                                  Locator locator, QName attName, TypeUse type, @Nullable QName typeName,
                                  boolean required ) {
        super(name, type, typeName, source, customizations, locator);
        isRequired = required;
        this.attName = attName;
    }

    @Override
    public boolean isRequired() {
        return isRequired;
    }

    @Override
    public QName getXmlName() {
        return attName;
    }

    /**
     * An optional attribute can never be unboxable,
     * for we need null to represent the absence.
     */
    @Override
    public boolean isUnboxable() {
        if(!isRequired) return false;
        return super.isUnboxable();
    }

    @Override
    public boolean isOptionalPrimitive() {
        return !isRequired && super.isUnboxable();
    }

    @Override
    public <V> V accept(CPropertyVisitor<V> visitor) {
        return visitor.onAttribute(this);
    }
    @Override
    public <R, P> R accept(CPropertyVisitor2<R, P> visitor, P p) {
        return visitor.visit(this, p);
    }

    @Override
    public final PropertyKind kind() {
        return  PropertyKind.ATTRIBUTE;
    }
}
