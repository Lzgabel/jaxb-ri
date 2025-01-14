/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.tools.xjc.reader.xmlschema.parser;

import com.sun.tools.xjc.reader.internalizer.AbstractReferenceFinderImpl;
import com.sun.tools.xjc.reader.internalizer.DOMForest;
import com.sun.tools.xjc.reader.internalizer.InternalizationLogic;
import com.sun.tools.xjc.util.DOMUtils;
import cn.glassfish.jaxb.core.v2.WellKnownNamespace;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.XMLFilterImpl;

import javax.xml.XMLConstants;

/**
 * XML Schema specific internalization logic.
 *
 * @author
 *     Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public class XMLSchemaInternalizationLogic implements InternalizationLogic {

    /**
     * This filter looks for {@code <xs:import> and <xs:include>}
     * and parses those documents referenced by them.
     */
    private static final class ReferenceFinder extends AbstractReferenceFinderImpl {
        ReferenceFinder( DOMForest parent ) {
            super(parent);
        }

        @Override
        protected String findExternalResource( String nsURI, String localName, Attributes atts) {
            if( XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(nsURI)
            && ("import".equals(localName) || "include".equals(localName) ) )
                return atts.getValue("schemaLocation");
            else
                return null;
        }
    }

    @Override
    public XMLFilterImpl createExternalReferenceFinder(DOMForest parent) {
        return new ReferenceFinder(parent);
    }

    @Override
    public boolean checkIfValidTargetNode(DOMForest parent, Element bindings, Element target) {
        return XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(target.getNamespaceURI());
    }

    @Override
    public Element refineTarget(Element target) {
        // look for existing xs:annotation
        Element annotation = DOMUtils.getFirstChildElement(target, XMLConstants.W3C_XML_SCHEMA_NS_URI, "annotation");
        if(annotation==null)
            // none exists. need to make one
            annotation = insertXMLSchemaElement( target, "annotation" );

        // then look for appinfo
        Element appinfo = DOMUtils.getFirstChildElement(annotation, XMLConstants.W3C_XML_SCHEMA_NS_URI, "appinfo" );
        if(appinfo==null)
            // none exists. need to make one
            appinfo = insertXMLSchemaElement( annotation, "appinfo" );

        return appinfo;
    }

    /**
     * Creates a new XML Schema element of the given local name
     * and insert it as the first child of the given parent node.
     *
     * @return
     *      Newly create element.
     */
    private Element insertXMLSchemaElement( Element parent, String localName ) {
        // use the same prefix as the parent node to avoid modifying
        // the namespace binding.
        String qname = parent.getTagName();
        int idx = qname.indexOf(':');
        if(idx==-1)     qname = localName;
        else            qname = qname.substring(0,idx+1)+localName;

        Element child = parent.getOwnerDocument().createElementNS( XMLConstants.W3C_XML_SCHEMA_NS_URI, qname );

        NodeList children = parent.getChildNodes();

        if( children.getLength()==0 )
            parent.appendChild(child);
        else
            parent.insertBefore( child, children.item(0) );

        return child;
    }
}
