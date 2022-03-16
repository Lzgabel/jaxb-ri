/*
 * Copyright (c) 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

module cn.lzgabel.jaxb.xml.bind.core {
    requires transitive jakarta.xml.bind;
    requires java.compiler;
    requires java.logging;

    requires transitive jakarta.activation;
    requires transitive java.xml;

    exports com.sun.istack;
    exports com.sun.istack.localization;
    exports com.sun.istack.logging;

    exports cn.lzgabel.jaxb.core;
    exports cn.lzgabel.jaxb.core.annotation;
    exports cn.lzgabel.jaxb.core.api;
    exports cn.lzgabel.jaxb.core.api.impl;
    exports cn.lzgabel.jaxb.core.marshaller;
    exports cn.lzgabel.jaxb.core.unmarshaller;
    exports cn.lzgabel.jaxb.core.util;
    exports cn.lzgabel.jaxb.core.v2;
    exports cn.lzgabel.jaxb.core.v2.model.annotation;
    exports cn.lzgabel.jaxb.core.v2.model.core;
    exports cn.lzgabel.jaxb.core.v2.model.impl;
    exports cn.lzgabel.jaxb.core.v2.model.nav;
    exports cn.lzgabel.jaxb.core.v2.model.util;
    exports cn.lzgabel.jaxb.core.v2.runtime;
    exports cn.lzgabel.jaxb.core.v2.runtime.unmarshaller;
    exports cn.lzgabel.jaxb.core.v2.schemagen.episode;
    exports cn.lzgabel.jaxb.core.v2.util;

    exports com.sun.xml.txw2;
    exports com.sun.xml.txw2.annotation;
    exports com.sun.xml.txw2.output;

    opens cn.lzgabel.jaxb.core.v2.model.nav to
            cn.lzgabel.jaxb.xml.bind,
            com.sun.tools.xjc,
            com.sun.xml.ws,
            com.sun.tools.ws;

}
