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

import cn.glassfish.jaxb.runtime.v2.runtime.reflect.Accessor;

/**
 * Template {@link Accessor} for double fields.
 * <p><b>
 *     Auto-generated, do not edit.
 * </b></p>
 * <p>
 *     All the FieldAccessors are generated from <code>FieldAccessor_B y t e</code>
 * </p>
 * @author Kohsuke Kawaguchi
 */
public class FieldAccessor_Double extends Accessor {
    public FieldAccessor_Double() {
        super(Double.class);
    }

    @Override
    public Object get(Object bean) {
        return ((Bean)bean).f_double;
    }

    @Override
    public void set(Object bean, Object value) {
        ((Bean)bean).f_double = value==null ? Const.default_value_double : (Double)value;
    }
}
