/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package cn.lzgabel.jaxb.runtime.v2.model.runtime;

import cn.lzgabel.jaxb.core.v2.model.core.TypeRef;

import java.lang.reflect.Type;

/**
 * @author Kohsuke Kawaguchi
 */
public interface RuntimeTypeRef extends TypeRef<Type,Class>, RuntimeNonElementRef {
    @Override
    RuntimeNonElement getTarget();
    @Override
    RuntimePropertyInfo getSource();
}
