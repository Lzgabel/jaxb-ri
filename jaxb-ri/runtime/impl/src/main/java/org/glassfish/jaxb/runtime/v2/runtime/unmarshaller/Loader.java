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

import cn.glassfish.jaxb.runtime.v2.runtime.JaxBeanInfo;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.helpers.ValidationEventImpl;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class Loader {

    // allow derived classes to change it later
    protected boolean expectText;

    protected Loader(boolean expectText) {
        this.expectText = expectText;
    }

    protected Loader() {
    }

//
//
//
// Contract
//
//
//
    /**
     * Called when the loader is activated, which is when a new start tag is seen
     * and when the parent designated this loader as the child loader.
     *
     * <p>
     * The callee may change {@code state.loader} to designate another {@link Loader}
     * for the processing. It's the responsibility of the callee to forward the startElement
     * event in such a case.
     *
     * @param ea
     *      info about the start tag. never null.
     */
    public void startElement(UnmarshallingContext.State state,TagName ea) throws SAXException {
    }

    /**
     * Called when this loaderis an active loaderand we see a new child start tag.
     *
     * <p>
     * The callee is expected to designate another loaderas a loaderthat processes
     * this element, then it should also register a {@link Receiver}.
     * The designated loaderwill become an active loader.
     *
     * <p>
     * The default implementation reports an error saying an element is unexpected.
     */
    public void childElement(UnmarshallingContext.State state, TagName ea) throws SAXException {
        // notify the error, then recover by ignoring the whole element.
        reportUnexpectedChildElement(ea, true);
        state.setLoader(Discarder.INSTANCE);
        state.setReceiver(null);
    }

    @SuppressWarnings({"StringEquality"})
    protected final void reportUnexpectedChildElement(TagName ea, boolean canRecover) throws SAXException {
        if (canRecover) {
            // this error happens particurly often (when input documents contain a lot of unexpected elements to be ignored),
            // so don't bother computing all the messages and etc if we know that
            // there's no event handler to receive the error in the end. See #286
            UnmarshallingContext context = UnmarshallingContext.getInstance();
            if (!context.parent.hasEventHandler() // is somebody listening?
                    || !context.shouldErrorBeReported()) // should we report error?
                return;
        }
        if(ea.uri!=ea.uri.intern() || ea.local!=ea.local.intern())
            reportError(Messages.UNINTERNED_STRINGS.format(), canRecover );
        else
            reportError(Messages.UNEXPECTED_ELEMENT.format(ea.uri,ea.local,computeExpectedElements()), canRecover );
    }

    /**
     * Returns a set of tag names expected as possible child elements in this context.
     */
    public Collection<QName> getExpectedChildElements() {
        return Collections.emptyList();
    }

    /**
     * Returns a set of tag names expected as possible child elements in this context.
     */
    public Collection<QName> getExpectedAttributes() {
        return Collections.emptyList();
    }

    /**
     * Called when this loaderis an active loaderand we see a chunk of text.
     *
     * The runtime makes sure that adjacent characters (even those separated
     * by comments, PIs, etc) are reported as one event.
     * IOW, you won't see two text event calls in a row.
     */
    public void text(UnmarshallingContext.State state, CharSequence text) throws SAXException {
        // make str printable
        text = text.toString().replace('\r',' ').replace('\n',' ').replace('\t',' ').trim();
        reportError(Messages.UNEXPECTED_TEXT.format(text), true );
    }

    /**
     * True if this loader expects the {@link #text(UnmarshallingContext.State, CharSequence)} method
     * to be called. False otherwise.
     */
    public final boolean expectText() {
        return expectText;
    }


    /**
     * Called when this loaderis an active loaderand we see an end tag.
     */
    public void leaveElement(UnmarshallingContext.State state, TagName ea) throws SAXException {
    }










//
//
//
// utility methods
//
//
//
    /**
     * Computes the names of possible root elements for a better error diagnosis.
     */
    private String computeExpectedElements() {
        StringBuilder r = new StringBuilder();

        for( QName n : getExpectedChildElements() ) {
            if(r.length()!=0)   r.append(',');
            r.append("<{").append(n.getNamespaceURI()).append('}').append(n.getLocalPart()).append('>');
        }
        if(r.length()==0) {
            return "(none)";
        }

        return r.toString();
    }

    /**
     * Fires the beforeUnmarshal event if necessary.
     *
     * @param state
     *      state of the newly create child object.
     */
    protected final void fireBeforeUnmarshal(JaxBeanInfo beanInfo, Object child, UnmarshallingContext.State state) throws SAXException {
        if(beanInfo.lookForLifecycleMethods()) {
            UnmarshallingContext context = state.getContext();
            Unmarshaller.Listener listener = context.parent.getListener();
            if(beanInfo.hasBeforeUnmarshalMethod()) {
                beanInfo.invokeBeforeUnmarshalMethod(context.parent, child, state.getPrev().getTarget());
            }
            if(listener!=null) {
                listener.beforeUnmarshal(child, state.getPrev().getTarget());
            }
        }
    }

    /**
     * Fires the afterUnmarshal event if necessary.
     *
     * @param state
     *      state of the parent object
     */
    protected final void fireAfterUnmarshal(JaxBeanInfo beanInfo, Object child, UnmarshallingContext.State state) throws SAXException {
        // fire the event callback
        if(beanInfo.lookForLifecycleMethods()) {
            UnmarshallingContext context = state.getContext();
            Unmarshaller.Listener listener = context.parent.getListener();
            if(beanInfo.hasAfterUnmarshalMethod()) {
                beanInfo.invokeAfterUnmarshalMethod(context.parent, child, state.getTarget());
            }
            if(listener!=null)
                listener.afterUnmarshal(child, state.getTarget());
        }
    }


    /**
     * Last resort when something goes terribly wrong within the unmarshaller.
     */
    protected static void handleGenericException(Exception e) throws SAXException {
        handleGenericException(e,false);
    }

    public static void handleGenericException(Exception e, boolean canRecover) throws SAXException {
        reportError(e.getMessage(), e, canRecover );
    }

    public static void handleGenericError(Error e) throws SAXException {
        reportError(e.getMessage(), false);
    }

    protected static void reportError(String msg, boolean canRecover) throws SAXException {
        reportError(msg, null, canRecover );
    }

    public static void reportError(String msg, Exception nested, boolean canRecover) throws SAXException {
        UnmarshallingContext context = UnmarshallingContext.getInstance();
        context.handleEvent( new ValidationEventImpl(
            canRecover? ValidationEvent.ERROR : ValidationEvent.FATAL_ERROR,
            msg,
            context.getLocator().getLocation(),
            nested ), canRecover );
    }

    /**
     * This method is called by the generated derived class
     * when a datatype parse method throws an exception.
     */
    protected static void handleParseConversionException(UnmarshallingContext.State state, Exception e) throws SAXException {
        // wrap it into a ParseConversionEvent and report it
        state.getContext().handleError(e);
    }
}
