/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package cn.glassfish.jaxb.runtime.v2.runtime.reflect.opt;

import cn.glassfish.jaxb.runtime.DatatypeConverterImpl;
import cn.glassfish.jaxb.runtime.v2.runtime.reflect.DefaultTransducedAccessor;
import cn.glassfish.jaxb.runtime.v2.runtime.reflect.TransducedAccessor;

/**
 * Template {@link TransducedAccessor} for a byte field.
 *
 * <p>
 * All the TransducedAccessor_field are generated from <code>TransducedAccessor_field_B y t e</code>
 *
 * @author Kohsuke Kawaguchi
 *
 * @see TransducedAccessor#get
 */
public final class TransducedAccessor_method_Byte extends DefaultTransducedAccessor {
    @Override
    public String print(Object o) {
        return DatatypeConverterImpl._printByte( ((Bean)o).get_byte() );
    }

    @Override
    public void parse(Object o, CharSequence lexical) {
        ((Bean)o).set_byte(DatatypeConverterImpl._parseByte(lexical));
    }

    @Override
    public boolean hasValue(Object o) {
        return true;
    }
//
//    public void writeLeafElement(Object o, QName tagName, String fieldName, XMLSerializer w) throws SAXException, AccessorException {
//        w.leafElement(tagName, ((Bean)o).get_byte(), fieldName );
//    }
}
