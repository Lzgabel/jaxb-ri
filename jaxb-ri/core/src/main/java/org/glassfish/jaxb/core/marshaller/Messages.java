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

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Formats error messages.
 *
 * @since JAXB 1.0
 */
public class Messages {

    private Messages() {}

    public static String format( String property ) {
        return format( property, null );
    }

    public static String format( String property, Object arg1 ) {
        return format( property, new Object[]{arg1} );
    }

    public static String format( String property, Object arg1, Object arg2 ) {
        return format( property, new Object[]{arg1,arg2} );
    }

    public static String format( String property, Object arg1, Object arg2, Object arg3 ) {
        return format( property, new Object[]{arg1,arg2,arg3} );
    }

    // add more if necessary.

    /** Loads a string resource and formats it with specified arguments. */
    static String format( String property, Object[] args ) {
        String text = ResourceBundle.getBundle(Messages.class.getName()).getString(property);
        return MessageFormat.format(text,args);
    }

//
//
// Message resources
//
//
    public static final String NOT_MARSHALLABLE = // 0 args
        "MarshallerImpl.NotMarshallable";

    public static final String UNSUPPORTED_RESULT = // 0 args
        "MarshallerImpl.UnsupportedResult";

    public static final String UNSUPPORTED_ENCODING = // 1 arg
        "MarshallerImpl.UnsupportedEncoding";

    public static final String NULL_WRITER = // 0 args
        "MarshallerImpl.NullWriterParam";

    public static final String ASSERT_FAILED = // 0 args
        "SAXMarshaller.AssertFailed";

    /**
     * @deprecated use ERR_MISSING_OBJECT2
     */
    @Deprecated
    public static final String ERR_MISSING_OBJECT = // 0 args
        "SAXMarshaller.MissingObject";

    /**
     * @deprecated
     *  use {@link cn.glassfish.jaxb.core.v2.runtime.XMLSerializer#reportMissingObjectError(String)}
     * Usage not found. TODO Remove
     */
    // public static final String ERR_MISSING_OBJECT2 = // 1 arg
    //    "SAXMarshaller.MissingObject2";

    /**
     * @deprecated only used from 1.0
     */
    @Deprecated
    public static final String ERR_DANGLING_IDREF = // 1 arg
        "SAXMarshaller.DanglingIDREF";

    /**
     * @deprecated only used from 1.0
     */
    @Deprecated
    public static final String ERR_NOT_IDENTIFIABLE = // 0 args
        "SAXMarshaller.NotIdentifiable";

    public static final String DOM_IMPL_DOESNT_SUPPORT_CREATELEMENTNS = // 2 args
        "SAX2DOMEx.DomImplDoesntSupportCreateElementNs";
}
