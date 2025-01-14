/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.tools.xjc.reader.xmlschema.bindinfo;

import cn.glassfish.jaxb.core.v2.WellKnownNamespace;
import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.helpers.XMLFilterImpl;

import javax.xml.XMLConstants;

/**
 * {@link XMLFilter} that can fork an event to another {@link ContentHandler}
 * in the middle.
 *
 * <p>
 * The side handler receives SAX events before the next handler in the filter chain does.
 *
 * @author Kohsuke Kawaguchi
 */
public class ForkingFilter extends XMLFilterImpl {

    /**
     * Non-null if we are also forking events to this handler.
     */
    private ContentHandler side;

    /**
     * The depth of the current element that the {@link #side} handler
     * is seeing.
     */
    private int depth;

    /**
     * In-scope namespace mapping.
     * namespaces[2n  ] := prefix
     * namespaces[2n+1] := namespace URI
     */
    private final ArrayList<String> namespaces = new ArrayList<>();

    private Locator loc;

    public ForkingFilter() {
    }

    public ForkingFilter(ContentHandler next) {
        setContentHandler(next);
    }

    public ContentHandler getSideHandler() {
        return side;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        super.setDocumentLocator(locator);
        this.loc = locator;
    }

    public Locator getDocumentLocator() {
        return loc;
    }

    @Override
    public void startDocument() throws SAXException {
        reset();
        super.startDocument();
    }

    private void reset() {
        namespaces.clear();
        side = null;
        depth = 0;
    }

    @Override
    public void endDocument() throws SAXException {
        loc = null;
        reset();
        super.endDocument();
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        if (XMLConstants.XML_NS_URI.equals(uri)) return; //xml prefix shall not be declared based on jdk api javadoc
        if(side!=null)
            side.startPrefixMapping(prefix,uri);
        namespaces.add(prefix);
        namespaces.add(uri);
        super.startPrefixMapping(prefix,uri);
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        if ("xml".equals(prefix)) return; //xml prefix shall not be declared based on jdk api javadoc
        if(side!=null)
            side.endPrefixMapping(prefix);
        super.endPrefixMapping(prefix);
        namespaces.remove(namespaces.size()-1);
        namespaces.remove(namespaces.size()-1);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        if(side!=null) {
            side.startElement(uri,localName,qName,atts);
            depth++;
        }
        super.startElement(uri, localName, qName, atts);
    }

    /**
     * Starts the event forking.
     */
    public void startForking(String uri, String localName, String qName, Attributes atts, ContentHandler side) throws SAXException {
        if(this.side!=null)     throw new IllegalStateException();  // can't fork to two handlers

        this.side = side;
        depth = 1;
        side.setDocumentLocator(loc);
        side.startDocument();
        for( int i=0; i<namespaces.size(); i+=2 )
            side.startPrefixMapping(namespaces.get(i),namespaces.get(i+1));
        side.startElement(uri,localName,qName,atts);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if(side!=null) {
            side.endElement(uri,localName,qName);
            depth--;
            if(depth==0) {
                for( int i=namespaces.size()-2; i>=0; i-=2 )
                    side.endPrefixMapping(namespaces.get(i));
                side.endDocument();
                side = null;
            }
        }
        super.endElement(uri, localName, qName);
    }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
        if(side!=null)
            side.characters(ch, start, length);
        super.characters(ch, start, length);
    }

    @Override
    public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
        if(side!=null)
            side.ignorableWhitespace(ch, start, length);
        super.ignorableWhitespace(ch, start, length);
    }
}
