/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package cn.glassfish.jaxb.runtime.v2.model.annotation;

import cn.glassfish.jaxb.core.v2.model.annotation.Locatable;
import jakarta.xml.bind.annotation.XmlTransient;

import java.lang.annotation.Annotation;


/**
 * <p><b>Auto-generated, do not edit.</b></p>
 *
 */
final class XmlTransientQuick
    extends Quick
    implements XmlTransient
{

    private final XmlTransient core;

    public XmlTransientQuick(Locatable upstream, XmlTransient core) {
        super(upstream);
        this.core = core;
    }

    @Override
    protected Annotation getAnnotation() {
        return core;
    }

    @Override
    protected Quick newInstance(Locatable upstream, Annotation core) {
        return new XmlTransientQuick(upstream, ((XmlTransient) core));
    }

    @Override
    public Class<XmlTransient> annotationType() {
        return XmlTransient.class;
    }

}
