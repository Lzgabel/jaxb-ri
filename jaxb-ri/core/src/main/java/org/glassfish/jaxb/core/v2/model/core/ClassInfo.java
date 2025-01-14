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

import java.util.List;

import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlValue;

/**
 * Information about JAXB-bound class.
 *
 * <p>
 * All the JAXB annotations are already reflected to the model so that
 * the caller doesn't have to worry about them. For this reason, you
 * cannot access annotations on properties.
 *
 * <h2>XML representation</h2>
 * <p>
 * A JAXB-bound class always have at least one representation
 * (called "type representation"),but it can optionally have another
 * representation ("element representation").
 *
 * <p>
 * In the type representaion, a class
 * is represented as a set of attributes and (elements or values).
 * You can inspect the details of those attributes/elements/values by {@link #getProperties()}.
 * This representation corresponds to a complex/simple type in XML Schema.
 * You can obtain the schema type name by {@link #getTypeName()}.
 *
 * <p>
 * If a class has an element representation, {@link #isElement()} returns true.
 * This representation is mostly similar to the type representation
 * except that the whoe attributes/elements/values are wrapped into
 * one element. You can obtain the name of this element through {@link #asElement()}.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public interface ClassInfo<T,C> extends MaybeElement<T,C> {

    /**
     * Obtains the information about the base class.
     *
     * @return null
     *      if this info extends from {@link Object}.
     */
    ClassInfo<T,C> getBaseClass();

    /**
     * Gets the declaration this object is wrapping.
     */
    C getClazz();

    /**
     * Gets the fully-qualified name of the class.
     */
    String getName();

    /**
     * Returns all the properties newly declared in this class.
     *
     * <p>
     * This excludes properties defined in the super class.
     *
     * <p>
     * If the properties are {@link #isOrdered() ordered},
     * it will be returned in the order that appear in XML.
     * Otherwise it will be returned in no particular order.
     *
     * <p>
     * Properties marked with {@link XmlTransient} will not show up
     * in this list. As far as JAXB is concerned, they are considered
     * non-existent.
     *
     * @return
     *      always non-null, but can be empty.
     */
    List<? extends PropertyInfo<T,C>> getProperties();

    /**
     * Returns true if this class or its ancestor has {@link XmlValue}
     * property.
     */
    boolean hasValueProperty();

    /**
     * Gets the property that has the specified name.
     *
     * <p>
     * This is just a convenience method for:
     * <pre>
     * for( PropertyInfo p : getProperties() ) {
     *   if(p.getName().equals(name))
     *     return p;
     * }
     * return null;
     * </pre>
     *
     * @return null
     *      if the property was not found.
     *
     * @see PropertyInfo#getName()
     */
    PropertyInfo<T,C> getProperty(String name);

    /**
     * If the class has properties, return true.  This is only
     * true if the Collection object returned by {@link #getProperties()}
     * is not empty.
     */
    boolean hasProperties();

    /**
     * If this class is abstract and thus shall never be directly instanciated.
     */
    boolean isAbstract();

    /**
     * Returns true if the properties of this class is ordered in XML.
     * False if it't not.
     *
     * <p>
     * In RELAX NG context, ordered properties mean {@code <group>} and
     * unordered properties mean {@code <interleave>}.
     */
    boolean isOrdered();

    /**
     * If this class is marked as final and no further extension/restriction is allowed.
     */
    boolean isFinal();

    /**
     * True if there's a known sub-type of this class in {@link TypeInfoSet}.
     */
    boolean hasSubClasses();

    /**
     * Returns true if this bean class has an attribute wildcard.
     *
     * <p>
     * This is true if the class declares an attribute wildcard,
     * or it is inherited from its super classes.
     *
     * @see #inheritsAttributeWildcard()
     */
    boolean hasAttributeWildcard();

    /**
     * Returns true iff this class inherits a wildcard attribute
     * from its ancestor classes.
     */
    boolean inheritsAttributeWildcard();

    /**
     * Returns true iff this class declares a wildcard attribute.
     */
    boolean declaresAttributeWildcard();
}
