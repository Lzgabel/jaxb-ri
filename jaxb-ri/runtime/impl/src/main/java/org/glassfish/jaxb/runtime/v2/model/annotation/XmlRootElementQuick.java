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
import jakarta.xml.bind.annotation.XmlRootElement;

import java.lang.annotation.Annotation;


/**
 * <p><b>Auto-generated, do not edit.</b></p>
 *
 */
final class XmlRootElementQuick
    extends Quick
    implements XmlRootElement
{

    private final XmlRootElement core;

    public XmlRootElementQuick(Locatable upstream, XmlRootElement core) {
        super(upstream);
        this.core = core;
    }

    @Override
    protected Annotation getAnnotation() {
        return core;
    }

    @Override
    protected Quick newInstance(Locatable upstream, Annotation core) {
        return new XmlRootElementQuick(upstream, ((XmlRootElement) core));
    }

    @Override
    public Class<XmlRootElement> annotationType() {
        return XmlRootElement.class;
    }

    @Override
    public String name() {
        return core.name();
    }

    @Override
    public String namespace() {
        return core.namespace();
    }

}
