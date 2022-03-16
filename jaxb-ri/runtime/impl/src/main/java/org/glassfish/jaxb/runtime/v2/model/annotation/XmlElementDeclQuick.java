/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package cn.lzgabel.jaxb.runtime.v2.model.annotation;

import cn.lzgabel.jaxb.core.v2.model.annotation.Locatable;
import jakarta.xml.bind.annotation.XmlElementDecl;

import java.lang.annotation.Annotation;


/**
 * <p><b>Auto-generated, do not edit.</b></p>
 *
 */
final class XmlElementDeclQuick
    extends Quick
    implements XmlElementDecl
{

    private final XmlElementDecl core;

    public XmlElementDeclQuick(Locatable upstream, XmlElementDecl core) {
        super(upstream);
        this.core = core;
    }

    @Override
    protected Annotation getAnnotation() {
        return core;
    }

    @Override
    protected Quick newInstance(Locatable upstream, Annotation core) {
        return new XmlElementDeclQuick(upstream, ((XmlElementDecl) core));
    }

    @Override
    public Class<XmlElementDecl> annotationType() {
        return XmlElementDecl.class;
    }

    @Override
    public String name() {
        return core.name();
    }

    @Override
    public Class scope() {
        return core.scope();
    }

    @Override
    public String namespace() {
        return core.namespace();
    }

    @Override
    public String defaultValue() {
        return core.defaultValue();
    }

    @Override
    public String substitutionHeadNamespace() {
        return core.substitutionHeadNamespace();
    }

    @Override
    public String substitutionHeadName() {
        return core.substitutionHeadName();
    }

}
