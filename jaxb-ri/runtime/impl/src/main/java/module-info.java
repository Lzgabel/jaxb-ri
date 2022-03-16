/*
 * Copyright (c) 2017, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

/**
 * The XML Binding (JAXB) RI modularization implementation.
 *
 */
module cn.glassfish.jaxb.runtime {
    requires transitive jakarta.xml.bind;
    requires java.compiler;
    requires java.desktop;
    requires java.logging;

    requires transitive jakarta.activation;
    requires transitive java.xml;

    requires transitive cn.glassfish.jaxb.core;
    requires static com.sun.xml.fastinfoset;
    requires static org.jvnet.staxex;

    exports cn.glassfish.jaxb.runtime;
    exports cn.glassfish.jaxb.runtime.api;
    exports cn.glassfish.jaxb.runtime.marshaller;
    exports cn.glassfish.jaxb.runtime.unmarshaller;
    exports cn.glassfish.jaxb.runtime.util;
    exports cn.glassfish.jaxb.runtime.v2;
    exports cn.glassfish.jaxb.runtime.v2.model.annotation;
    exports cn.glassfish.jaxb.runtime.v2.model.impl;
    exports cn.glassfish.jaxb.runtime.v2.model.runtime;
    exports cn.glassfish.jaxb.runtime.v2.runtime;
    exports cn.glassfish.jaxb.runtime.v2.runtime.reflect;
    exports cn.glassfish.jaxb.runtime.v2.runtime.unmarshaller;
    exports cn.glassfish.jaxb.runtime.v2.schemagen;
    exports cn.glassfish.jaxb.runtime.v2.schemagen.xmlschema;
    exports cn.glassfish.jaxb.runtime.v2.util;

    opens cn.glassfish.jaxb.runtime.v2.runtime.reflect.opt to jakarta.xml.bind;
    opens cn.glassfish.jaxb.runtime.v2.schemagen to jakarta.xml.bind;
    opens cn.glassfish.jaxb.runtime.v2.schemagen.xmlschema to jakarta.xml.bind;

    provides jakarta.xml.bind.JAXBContextFactory with cn.glassfish.jaxb.runtime.v2.JAXBContextFactory;

}
