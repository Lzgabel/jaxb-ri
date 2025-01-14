/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package cn.glassfish.jaxb.runtime.v2.runtime.property;

import cn.glassfish.jaxb.runtime.api.AccessorException;
import cn.glassfish.jaxb.runtime.v2.model.runtime.RuntimePropertyInfo;
import cn.glassfish.jaxb.runtime.v2.runtime.JAXBContextImpl;
import cn.glassfish.jaxb.runtime.v2.runtime.Name;
import cn.glassfish.jaxb.runtime.v2.runtime.XMLSerializer;
import cn.glassfish.jaxb.runtime.v2.runtime.reflect.Accessor;
import cn.glassfish.jaxb.runtime.v2.runtime.reflect.Lister;
import cn.glassfish.jaxb.runtime.v2.util.QNameMap;
import cn.glassfish.jaxb.runtime.v2.runtime.unmarshaller.*;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Collection;

/**
 * Commonality between {@link ArrayElementProperty} and {@link ArrayReferenceNodeProperty}.
 *
 * Mostly handles the unmarshalling of the wrapper element.
 *
 * @author Kohsuke Kawaguchi
 */
abstract class ArrayERProperty<BeanT,ListT,ItemT> extends ArrayProperty<BeanT,ListT,ItemT> {

    /**
     * Wrapper tag name if any, or null.
     */
    protected final Name wrapperTagName;

    /**
     * True if the wrapper tag name is nillable.
     * Always false if {@link #wrapperTagName}==null.
     */
    protected final boolean isWrapperNillable;

    protected ArrayERProperty(JAXBContextImpl grammar, RuntimePropertyInfo prop, QName tagName, boolean isWrapperNillable) {
        super(grammar,prop);
        if(tagName==null)
            this.wrapperTagName = null;
        else
            this.wrapperTagName = grammar.nameBuilder.createElementName(tagName);
        this.isWrapperNillable = isWrapperNillable;
    }

    /**
     * Used to handle the collection wrapper element.
     */
    private static final class ItemsLoader extends Loader {

        private final Accessor acc;
        private final Lister lister;

        public ItemsLoader(Accessor acc, Lister lister, QNameMap<ChildLoader> children) {
            super(false);
            this.acc = acc;
            this.lister = lister;
            this.children = children;
        }

        @Override
        public void startElement(UnmarshallingContext.State state, TagName ea) throws SAXException {
            UnmarshallingContext context = state.getContext();
            context.startScope(1);
            // inherit the target so that our children can access its target
            state.setTarget(state.getPrev().getTarget());

            // start it now, so that even if there's no children we can still return empty collection
            context.getScope(0).start(acc,lister);
        }

        private final QNameMap<ChildLoader> children;

        @Override
        public void childElement(UnmarshallingContext.State state, TagName ea) throws SAXException {
            ChildLoader child = children.get(ea.uri,ea.local);
            if (child == null) {
                child = children.get(CATCH_ALL);
            }
            if (child == null) {
                super.childElement(state,ea);
                return;
            }
            state.setLoader(child.loader);
            state.setReceiver(child.receiver);
        }

        @Override
        public void leaveElement(UnmarshallingContext.State state, TagName ea) throws SAXException {
            state.getContext().endScope(1);
        }

        @Override
        public Collection<QName> getExpectedChildElements() {
            return children.keySet();
        }
    }

    @Override
    public final void serializeBody(BeanT o, XMLSerializer w, Object outerPeer) throws SAXException, AccessorException, IOException, XMLStreamException {
        ListT list = acc.get(o);

        if(list!=null) {
            if(wrapperTagName!=null) {
                w.startElement(wrapperTagName,null);
                w.endNamespaceDecls(list);
                w.endAttributes();
            }

            serializeListBody(o,w,list);

            if(wrapperTagName!=null)
                w.endElement();
        } else {
            // list is null
            if(isWrapperNillable) {
                w.startElement(wrapperTagName,null);
                w.writeXsiNilTrue();
                w.endElement();
            } // otherwise don't print the wrapper tag name
        }
    }

    /**
     * Serializes the items of the list.
     * This method is invoked after the necessary wrapper tag is produced (if necessary.)
     *
     * @param list
     *      always non-null.
     */
    protected abstract void serializeListBody(BeanT o, XMLSerializer w, ListT list) throws IOException, XMLStreamException, SAXException, AccessorException;

    /**
     * Creates the unmarshaler to unmarshal the body.
     */
    protected abstract void createBodyUnmarshaller(UnmarshallerChain chain, QNameMap<ChildLoader> loaders);


    @Override
    public final void buildChildElementUnmarshallers(UnmarshallerChain chain, QNameMap<ChildLoader> loaders) {
        if(wrapperTagName!=null) {
            UnmarshallerChain c = new UnmarshallerChain(chain.context);
            QNameMap<ChildLoader> m = new QNameMap<>();
            createBodyUnmarshaller(c,m);
            Loader loader = new ItemsLoader(acc, lister, m);
            if(isWrapperNillable || chain.context.allNillable)
                loader = new XsiNilLoader(loader);
            loaders.put(wrapperTagName,new ChildLoader(loader,null));
        } else {
            createBodyUnmarshaller(chain,loaders);
        }
    }

    /**
     * {@link Receiver} that puts the child object into the {@link Scope} object.
     */
    protected final class ReceiverImpl implements Receiver {
        private final int offset;

        protected ReceiverImpl(int offset) {
            this.offset = offset;
        }

        @Override
        public void receive(UnmarshallingContext.State state, Object o) throws SAXException {
            state.getContext().getScope(offset).add(acc,lister,o);
        }
    }}
