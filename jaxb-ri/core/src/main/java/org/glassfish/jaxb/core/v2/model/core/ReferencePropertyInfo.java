/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package cn.glassfish.jaxb.core.v2.model.core;

import java.util.Collection;
import java.util.Set;

import javax.xml.namespace.QName;

/**
 * {@link PropertyInfo} that holds references to other {@link Element}s.
 *
 * @author Kohsuke Kawaguchi
 */
public interface ReferencePropertyInfo<T,C> extends PropertyInfo<T,C> {
    /**
     * Returns the information about the possible elements in this property.
     *
     * <p>
     * As of 2004/08/17, the spec only allows you to use different element names
     * when a property is a collection, but I think there's really no reason
     * to limit it there --- if the user wants to use a different tag name
     * for different objects, I don't see why this can be limited to collections.
     *
     * <p>
     * So this is a generalization of the spec. We always allow a property to have
     * multiple types and use different tag names for it, depending on the actual type.
     *
     * <p>
     * In most of the cases, this collection only contains 1 item. So the runtime system
     * is encouraged to provide a faster code-path that is optimized toward such cases.
     *
     * @return
     *      Always non-null. Contains at least one entry.
     */
    Set<? extends Element<T,C>> getElements();

    /**
     * {@inheritDoc}.
     *
     * If this {@link ReferencePropertyInfo} has a wildcard in it,
     * then the returned list will contain {@link WildcardTypeInfo}.
     */
    @Override
    Collection<? extends TypeInfo<T,C>> ref();

    /**
     * Gets the wrapper element name.
     *
     * @return
     *      must be null if not collection. If the property is a collection,
     *      this can be null (in which case there'll be no wrapper),
     *      or it can be non-null (in which case there'll be a wrapper)
     */
    QName getXmlName();

    /**
     * Returns true if this property is nillable
     * (meaning the absence of the value is treated as nil='true')
     *
     * <p>
     * This method is only used when this property is a collection.
     */
    boolean isCollectionNillable();

    /**
     * Checks if the wrapper element is required.
     *
     * @return
     *      Always false if {@link #getXmlName()}==null.
     */
    boolean isCollectionRequired();

    /**
     * Returns true if this property can hold {@link String}s to represent
     * mixed content model.
     */
    boolean isMixed();

    /**
     * If this property supports the wildcard, returns its mode.
     *
     * @return null
     *      if the wildcard is not allowed on this element.
     */
    WildcardMode getWildcard();

    /**
     * If this property supports the wildcard, returns its DOM handler.
     *
     * @return null
     *      if the wildcard is not allowed on this element.
     */
    C getDOMHandler();

    /**
     * Returns true if this element is mandatory.
     */
    boolean isRequired();

    @Override
    Adapter<T,C> getAdapter();
}
