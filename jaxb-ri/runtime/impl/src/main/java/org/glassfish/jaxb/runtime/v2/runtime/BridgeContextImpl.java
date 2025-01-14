/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package cn.glassfish.jaxb.runtime.v2.runtime;

import cn.glassfish.jaxb.runtime.api.BridgeContext;
import cn.glassfish.jaxb.runtime.v2.runtime.unmarshaller.UnmarshallerImpl;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.ValidationEventHandler;
import jakarta.xml.bind.attachment.AttachmentMarshaller;
import jakarta.xml.bind.attachment.AttachmentUnmarshaller;

/**
 * {@link BridgeContext} implementation.
 *
 * @author Kohsuke Kawaguchi
 */
public final class BridgeContextImpl extends BridgeContext {

    public final UnmarshallerImpl unmarshaller;
    public final MarshallerImpl marshaller;

    BridgeContextImpl(JAXBContextImpl context) {
        unmarshaller = context.createUnmarshaller();
        marshaller = context.createMarshaller();
    }

    @Override
    public void setErrorHandler(ValidationEventHandler handler) {
        try {
            unmarshaller.setEventHandler(handler);
            marshaller.setEventHandler(handler);
        } catch (JAXBException e) {
            // impossible
            throw new Error(e);
        }
    }

    @Override
    public void setAttachmentMarshaller(AttachmentMarshaller m) {
        marshaller.setAttachmentMarshaller(m);
    }

    @Override
    public void setAttachmentUnmarshaller(AttachmentUnmarshaller u) {
        unmarshaller.setAttachmentUnmarshaller(u);
    }

    @Override
    public AttachmentMarshaller getAttachmentMarshaller() {
        return marshaller.getAttachmentMarshaller();
    }

    @Override
    public AttachmentUnmarshaller getAttachmentUnmarshaller() {
        return unmarshaller.getAttachmentUnmarshaller();
    }
}
