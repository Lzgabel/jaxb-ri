/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package cn.glassfish.jaxb.core.marshaller;

import java.util.Stack;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import cn.glassfish.jaxb.core.util.Which;
import com.sun.istack.FinalArrayList;

import cn.glassfish.jaxb.core.v2.util.XmlFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;

/**
 * Builds a DOM tree from SAX2 events.
 *
 * @author  Vivek Pandey
 * @since 1.0
 */
public class SAX2DOMEx implements ContentHandler {

    private Node node = null;
    private boolean isConsolidate;
    protected final Stack<Node> nodeStack = new Stack<>();
    private final FinalArrayList<String> unprocessedNamespaces = new FinalArrayList<>();
    /**
     * Document object that owns the specified node.
     */
    protected final Document document;

    /**
     * @param   node
     *      Nodes will be created and added under this object.
     */
    public SAX2DOMEx(Node node) {
        this(node, false);
    }

    /**
     * @param   node
     *      Nodes will be created and added under this object.
     */
    public SAX2DOMEx(Node node, boolean isConsolidate) {
        this.node = node;
        this.isConsolidate = isConsolidate;
        nodeStack.push(this.node);

        if (node instanceof Document) {
            this.document = (Document) node;
        } else {
            this.document = node.getOwnerDocument();
        }
    }

    /**
     * Creates a fresh empty DOM document and adds nodes under this document.
     */
    public SAX2DOMEx(DocumentBuilderFactory f) throws ParserConfigurationException {
        f.setValidating(false);
        document = f.newDocumentBuilder().newDocument();
        node = document;
        nodeStack.push(document);
    }

    /**
     * Creates a fresh empty DOM document and adds nodes under this document.
     * @deprecated
     */
    @Deprecated
    public SAX2DOMEx() throws ParserConfigurationException {
        DocumentBuilderFactory factory = XmlFactory.createDocumentBuilderFactory(false);
        factory.setValidating(false);

        document = factory.newDocumentBuilder().newDocument();
        node = document;
        nodeStack.push(document);
    }

    public final Element getCurrentElement() {
        return (Element) nodeStack.peek();
    }

    public Node getDOM() {
        return node;
    }

    @Override
    public void startDocument() {
    }

    @Override
    public void endDocument() {
    }

    protected void namespace(Element element, String prefix, String uri) {
        String qname;
        if ("".equals(prefix) || prefix == null) {
            qname = "xmlns";
        } else {
            qname = "xmlns:" + prefix;
        }

        // older version of Xerces (I confirmed that the bug is gone with Xerces 2.4.0)
        // have a problem of re-setting the same namespace attribute twice.
        // work around this bug removing it first.
        if (element.hasAttributeNS("http://www.w3.org/2000/xmlns/", qname)) {
            // further workaround for an old Crimson bug where the removeAttribtueNS
            // method throws NPE when the element doesn't have any attribute.
            // to be on the safe side, check the existence of attributes before
            // attempting to remove it.
            // for details about this bug, see org.apache.crimson.tree.ElementNode2
            // line 540 or the following message:
            // https://jaxb.dev.java.net/servlets/ReadMsg?list=users&msgNo=2767
            element.removeAttributeNS("http://www.w3.org/2000/xmlns/", qname);
        }
        // workaround until here

        element.setAttributeNS("http://www.w3.org/2000/xmlns/", qname, uri);
    }

    @Override
    public void startElement(String namespace, String localName, String qName, Attributes attrs) {
        Node parent = nodeStack.peek();

        // some broken DOM implementation (we confirmed it with SAXON)
        // return null from this method.
        Element element = document.createElementNS(namespace, qName);

        if (element == null) {
            // if so, report an user-friendly error message,
            // rather than dying mysteriously with NPE.
            throw new AssertionError(
                    Messages.format(Messages.DOM_IMPL_DOESNT_SUPPORT_CREATELEMENTNS,
                    document.getClass().getName(),
                    Which.which(document.getClass())));
        }

        // process namespace bindings
        for (int i = 0; i < unprocessedNamespaces.size(); i += 2) {
            String prefix = unprocessedNamespaces.get(i);
            String uri = unprocessedNamespaces.get(i + 1);

            namespace(element, prefix, uri);
        }
        unprocessedNamespaces.clear();


        if (attrs != null) {
            int length = attrs.getLength();
            for (int i = 0; i < length; i++) {
                String namespaceuri = attrs.getURI(i);
                String value = attrs.getValue(i);
                String qname = attrs.getQName(i);
                element.setAttributeNS(namespaceuri, qname, value);
            }
        }
        // append this new node onto current stack node
        parent.appendChild(element);
        // push this node onto stack
        nodeStack.push(element);
    }

    @Override
    public void endElement(String namespace, String localName, String qName) {
        nodeStack.pop();
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        characters(new String(ch, start, length));
    }

    protected Text characters(String s) {
        Node parent = nodeStack.peek();
        Node lastChild = parent.getLastChild();
        Text text;
        if (isConsolidate && lastChild != null && lastChild.getNodeType() == Node.TEXT_NODE) {
            text = (Text) lastChild;
            text.appendData(s);
        } else {
            text = document.createTextNode(s);
            parent.appendChild(text);
        }
        return text;
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) {
    }

    @Override
    public void processingInstruction(String target, String data) throws org.xml.sax.SAXException {
        Node parent = nodeStack.peek();
        Node n = document.createProcessingInstruction(target, data);
        parent.appendChild(n);
    }

    @Override
    public void setDocumentLocator(Locator locator) {
    }

    @Override
    public void skippedEntity(String name) {
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) {
        unprocessedNamespaces.add(prefix);
        unprocessedNamespaces.add(uri);
    }

    @Override
    public void endPrefixMapping(String prefix) {
    }
}
