/*
 * Copyright (c) 2019, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

module cn.lzgabel.jaxb.xml.bind {
    requires java.compiler;
    requires java.desktop;
    requires java.logging;

    requires transitive cn.lzgabel.jaxb.xml.bind.core;

    requires static com.sun.xml.fastinfoset;
    requires static org.jvnet.staxex;

    exports cn.lzgabel.jaxb.runtime;
    exports cn.lzgabel.jaxb.runtime.api;
    exports cn.lzgabel.jaxb.runtime.marshaller;
    exports cn.lzgabel.jaxb.runtime.unmarshaller;
    exports cn.lzgabel.jaxb.runtime.util;
    exports cn.lzgabel.jaxb.runtime.v2;
    exports cn.lzgabel.jaxb.runtime.v2.model.annotation;
    exports cn.lzgabel.jaxb.runtime.v2.model.impl;
    exports cn.lzgabel.jaxb.runtime.v2.model.runtime;
    exports cn.lzgabel.jaxb.runtime.v2.runtime;
    exports cn.lzgabel.jaxb.runtime.v2.runtime.output;
    exports cn.lzgabel.jaxb.runtime.v2.runtime.property;
    exports cn.lzgabel.jaxb.runtime.v2.runtime.reflect;
    exports cn.lzgabel.jaxb.runtime.v2.runtime.unmarshaller;
    exports cn.lzgabel.jaxb.runtime.v2.schemagen;
    exports cn.lzgabel.jaxb.runtime.v2.schemagen.xmlschema;
    exports cn.lzgabel.jaxb.runtime.v2.util;

    opens cn.lzgabel.jaxb.runtime.v2.runtime.reflect.opt to jakarta.xml.bind;
    opens cn.lzgabel.jaxb.runtime.v2.schemagen to jakarta.xml.bind;
    opens cn.lzgabel.jaxb.runtime.v2.schemagen.xmlschema to jakarta.xml.bind;
    opens cn.lzgabel.jaxb.runtime.v2 to jakarta.xml.bind;

    provides jakarta.xml.bind.JAXBContextFactory with cn.lzgabel.jaxb.runtime.v2.JAXBContextFactory;

}
