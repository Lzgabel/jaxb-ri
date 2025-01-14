/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package cn.glassfish.jaxb.runtime.v2.runtime.unmarshaller;

import cn.glassfish.jaxb.core.v2.runtime.unmarshaller.LocatorEx;
import org.xml.sax.SAXException;

import javax.xml.namespace.NamespaceContext;

/**
 * Walks the XML document structure.
 *
 * Implemented by the unmarshaller and called by the API-specific connectors.
 *
 * <h2>Event Call Sequence</h2>
 *
 * The {@link XmlVisitor} expects the event callbacks in the following order:
 * <pre>
 * CALL SEQUENCE := startDocument ELEMENT endDocument
 * ELEMENT       := startPrefixMapping ELEMENT endPrefixMapping
 *               |  startElement BODY endElement
 * BODY          := text? (ELEMENT text?)*
 * </pre>
 * Note in particular that text events may not be called in a row;
 * consecutive characters (even those separated by PIs and comments)
 * must be reported as one event, unlike SAX.
 *
 * <p>
 * All namespace URIs, local names, and prefixes of element and attribute
 * names must be interned. qnames need not be interned.
 *
 *
 * <h2>Typed PCDATA</h2>
 * For efficiency, JAXB RI defines a few {@link CharSequence} implementations
 * that can be used as a parameter to the {@link #text(CharSequence)} method.
 * For example, see {@link Base64Data}.
 *
 * <h2>Error Handling</h2>
 * The visitor may throw {@link SAXException} to abort the unmarshalling process
 * in the middle.
 *
 * @author Kohsuke Kawaguchi
 */
public interface XmlVisitor {
    /**
     * Notifies a start of the document.
     *
     * @param locator
     *      This live object returns the location information as the parsing progresses.
     *      must not be null.
     * @param nsContext
     *      Some broken XML APIs can't iterate all the in-scope namespace bindings,
     *      which makes it impossible to emulate {@link #startPrefixMapping(String, String)} correctly
     *      when unmarshalling a subtree. Connectors that use such an API can
     *      pass in additional {@link NamespaceContext} object that knows about the
     *      in-scope namespace bindings. Otherwise (and normally) it is null.
     *
     *      <p>
     *      Ideally this object should be immutable and only represent the namespace URI bindings
     *      in the context (those done above the element that JAXB started unmarshalling),
     *      but it can also work even if it changes as the parsing progress (to include
     *      namespaces declared on the current element being parsed.)
     */
    void startDocument(LocatorEx locator, NamespaceContext nsContext) throws SAXException;
    void endDocument() throws SAXException;

    /**
     * Notifies a start tag of a new element.
     *
     * namespace URIs and local names must be interned.
     */
    void startElement(TagName tagName) throws SAXException;
    void endElement(TagName tagName) throws SAXException;

    /**
     * Called before {@link #startElement} event to notify a new namespace binding.
     */
    void startPrefixMapping( String prefix, String nsUri ) throws SAXException;
    /**
     * Called after {@link #endElement} event to notify the end of a binding.
     */
    void endPrefixMapping( String prefix ) throws SAXException;

    /**
     * Text events.
     *
     * <p>
     * The caller should consult {@link TextPredictor} to see
     * if the unmarshaller is expecting any PCDATA. If the above is returning
     * false, the caller is OK to skip any text in XML. The net effect is
     * that we can ignore whitespaces quickly.
     *
     * @param pcdata
     *      represents character data. This object can be mutable
     *      (such as {@link StringBuilder}); it only needs to be fixed
     *      while this method is executing.
     */
    void text( CharSequence pcdata ) throws SAXException;

    /**
     * Returns the {@link UnmarshallingContext} at the end of the chain.
     *
     * @return
     *      always return the same object, so caching the result is recommended.
     */
    UnmarshallingContext getContext();

    /**
     * Gets the predictor that can be used for the caller to avoid
     * calling {@link #text(CharSequence)} unnecessarily.
     */
    TextPredictor getPredictor();

    interface TextPredictor {
        /**
         * Returns true if the visitor is expecting a text event as the next event.
         *
         * <p>
         * This is primarily intended to be used for optimization to avoid buffering
         * characters unnecessarily. If this method returns false and the connector
         * sees whitespace it can safely skip it.
         *
         * <p>
         * If this method returns true, all the whitespaces are considered significant
         * and thus need to be reported as a {@link XmlVisitor#text} event. Furthermore,
         * if the element has no children (like {@code <foo/>}), then it has to be reported
         * an empty {@link XmlVisitor#text} event.
         */
        boolean expectText();
    }
}
