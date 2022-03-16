/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

/**
 * The XML Binding implementation.
 *
 * @uses jakarta.xml.bind.JAXBContextFactory
 *
 */
module cn.glassfish.jaxb.core {
    requires transitive jakarta.xml.bind;
    requires java.compiler;
    requires java.desktop;
    requires java.logging;

    requires transitive jakarta.activation;
    requires transitive java.xml;

    requires transitive com.sun.xml.txw2;
    requires transitive com.sun.istack.runtime;

    exports cn.glassfish.jaxb.core;
    exports cn.glassfish.jaxb.core.annotation;
    exports cn.glassfish.jaxb.core.api;
    exports cn.glassfish.jaxb.core.api.impl;
    exports cn.glassfish.jaxb.core.marshaller;
    exports cn.glassfish.jaxb.core.unmarshaller;
    exports cn.glassfish.jaxb.core.util;
    exports cn.glassfish.jaxb.core.v2;
    exports cn.glassfish.jaxb.core.v2.model.annotation;
    exports cn.glassfish.jaxb.core.v2.model.core;
    exports cn.glassfish.jaxb.core.v2.model.impl;
    exports cn.glassfish.jaxb.core.v2.model.nav;
    exports cn.glassfish.jaxb.core.v2.model.util;
    exports cn.glassfish.jaxb.core.v2.runtime;
    exports cn.glassfish.jaxb.core.v2.runtime.unmarshaller;
    exports cn.glassfish.jaxb.core.v2.schemagen.episode;
    exports cn.glassfish.jaxb.core.v2.util;

    opens cn.glassfish.jaxb.core.v2.model.nav to
            cn.glassfish.jaxb.runtime,
            cn.glassfish.jaxb.xjc,
            com.sun.xml.ws.rt,
            com.sun.tools.ws.wscompile;

}
