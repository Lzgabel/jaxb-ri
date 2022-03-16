/*
 * Copyright (c) 2019, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

module cn.lzgabel.jaxb.xml.bind.osgi {

    requires transitive jakarta.xml.bind;
    requires transitive jakarta.activation;
    requires transitive java.xml;
    requires java.compiler;
    requires java.desktop;
    requires java.logging;
    requires static jdk.compiler;

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
    exports cn.lzgabel.jaxb.runtime.v2.runtime.unmarshaller;
    exports cn.lzgabel.jaxb.runtime.v2.schemagen;
    exports cn.lzgabel.jaxb.runtime.v2.schemagen.xmlschema;
    exports cn.lzgabel.jaxb.runtime.v2.util;

    exports com.sun.tools.xjc;
    exports com.sun.tools.xjc.reader;
    exports com.sun.tools.xjc.reader.internalizer;
    exports com.sun.tools.xjc.api;
    exports com.sun.tools.xjc.util;

    exports com.sun.tools.jxc;
    exports com.sun.tools.jxc.ap;
    exports com.sun.tools.jxc.model.nav;
    exports com.sun.tools.jxc.api;


    exports com.sun.xml.txw2;
    exports com.sun.xml.txw2.annotation;
    exports com.sun.xml.txw2.output;

    exports com.sun.xml.fastinfoset;
    exports com.sun.xml.fastinfoset.algorithm;
    exports com.sun.xml.fastinfoset.alphabet;
    exports com.sun.xml.fastinfoset.dom;
    exports com.sun.xml.fastinfoset.sax;
    exports com.sun.xml.fastinfoset.stax;
    exports com.sun.xml.fastinfoset.stax.events;
    exports com.sun.xml.fastinfoset.stax.factory;
    exports com.sun.xml.fastinfoset.stax.util;
    exports com.sun.xml.fastinfoset.tools;
    exports com.sun.xml.fastinfoset.util;
    exports com.sun.xml.fastinfoset.vocab;
    exports org.jvnet.fastinfoset;
    exports org.jvnet.fastinfoset.sax;
    exports org.jvnet.fastinfoset.sax.helpers;
    exports org.jvnet.fastinfoset.stax;

    exports org.jvnet.staxex;
    exports org.jvnet.staxex.util;


    exports com.sun.xml.xsom;
    exports com.sun.xml.xsom.util;
    exports com.sun.xml.xsom.visitor;
    exports com.sun.xml.xsom.impl.util;
    exports com.sun.xml.xsom.parser;

    exports com.sun.tools.rngom.parse;
    exports com.sun.tools.rngom.parse.compact;
    exports com.sun.tools.rngom.parse.xml;
    exports com.sun.tools.rngom.digested;
    exports com.sun.tools.rngom.nc;
    exports com.sun.tools.rngom.xml.sax;
    exports com.sun.tools.rngom.xml.util;
    exports com.sun.tools.rngom.ast.builder;
    exports com.sun.tools.rngom.ast.om;
    exports com.sun.tools.rngom.ast.util;
    exports com.sun.tools.rngom.dt;
    exports com.sun.tools.rngom.dt.builtin;

    exports com.sun.codemodel;
    exports com.sun.codemodel.util;
    exports com.sun.codemodel.writer;
    exports com.sun.codemodel.fmt;

    exports com.sun.xml.dtdparser;

    exports com.sun.istack.tools;

    exports com.sun.tools.rngdatatype;
    exports com.sun.tools.rngdatatype.helpers;

    opens cn.lzgabel.jaxb.runtime.v2.runtime.reflect.opt to jakarta.xml.bind;
    opens cn.lzgabel.jaxb.runtime.v2.schemagen to jakarta.xml.bind;
    opens cn.lzgabel.jaxb.runtime.v2.schemagen.xmlschema to jakarta.xml.bind;
    opens cn.lzgabel.jaxb.runtime.v2 to jakarta.xml.bind;
    opens com.sun.tools.xjc.reader.xmlschema.bindinfo to jakarta.xml.bind;

    uses com.sun.tools.xjc.Plugin;

    provides com.sun.tools.xjc.Plugin with
        com.sun.tools.xjc.addon.accessors.PluginImpl,
        com.sun.tools.xjc.addon.at_generated.PluginImpl,
        com.sun.tools.xjc.addon.code_injector.PluginImpl,
        com.sun.tools.xjc.addon.episode.PluginImpl,
        com.sun.tools.xjc.addon.locator.SourceLocationAddOn,
        com.sun.tools.xjc.addon.sync.SynchronizedMethodAddOn;
}
