/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.tools.xjc.reader.dtd;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import com.sun.tools.xjc.model.CBuiltinLeafInfo;
import com.sun.tools.xjc.model.CClassInfo;
import com.sun.tools.xjc.model.CElementPropertyInfo;
import static com.sun.tools.xjc.model.CElementPropertyInfo.CollectionMode.*;
import com.sun.tools.xjc.model.CPropertyInfo;
import com.sun.tools.xjc.model.CReferencePropertyInfo;
import com.sun.tools.xjc.model.CTypeRef;
import com.sun.tools.xjc.model.CValuePropertyInfo;
import com.sun.tools.xjc.model.TypeUse;
import com.sun.tools.xjc.reader.dtd.bindinfo.BIConversion;
import com.sun.tools.xjc.reader.dtd.bindinfo.BIElement;
import cn.glassfish.jaxb.core.v2.model.core.ID;
import cn.glassfish.jaxb.core.v2.model.core.WildcardMode;
import com.sun.xml.dtdparser.DTDEventListener;

import org.xml.sax.Locator;

/**
 * DTD Element.
 *
 * <p>
 * This class extends {@link Term} to participate in the content model tree.
 *
 * <p>
 * This class is repsonsible for binding the element.
 *
 * @author Kohsuke Kawaguchi
 */
final class Element extends Term implements Comparable<Element> {

    /**
     * Name of the element.
     */
    final String name;

    private final TDTDReader owner;

    /**
     * @see DTDEventListener#endContentModel(String, short)
     */
    private short contentModelType;

    private Term contentModel;

    /**
     * True if this element is referenced from another element.
     */
    boolean isReferenced;

    /**
     * If this element maps to a class, that class representation.
     * Otherwise null.
     */
    private CClassInfo classInfo;

    /**
     * True if {@link #classInfo} field is computed.
     */
    private boolean classInfoComputed;

    /**
     * List of attribute properties on this element
     */
    final List<CPropertyInfo> attributes = new ArrayList<>();

    /**
     * Normalized blocks of the content model.
     */
    private final List<Block> normalizedBlocks = new ArrayList<>();

    /**
     * True if this element needs to be a class.
     *
     * Currently, if an element is referenced from a construct like (A|B|C),
     * we require those A,B, and C to be a class.
     */
    private boolean mustBeClass;

    /**
     * The source location where this element is defined.
     */
    private Locator locator;

    public Element(TDTDReader owner,String name) {
        this.owner = owner;
        this.name = name;
    }

    @Override
    void normalize(List<Block> r, boolean optional) {
        Block o = new Block(optional,false);
        o.elements.add(this);
        r.add(o);
    }

    @Override
    void addAllElements(Block b) {
        b.elements.add(this);
    }

    @Override
    boolean isOptional() {
        return false;
    }

    @Override
    boolean isRepeated() {
        return false;
    }


    /**
     * Define its content model.
     */
    void define(short contentModelType, Term contentModel, Locator locator) {
        assert this.contentModel==null; // may not be called twice
        this.contentModelType = contentModelType;
        this.contentModel = contentModel;
        this.locator = locator;
        contentModel.normalize(normalizedBlocks,false);

        for( Block b : normalizedBlocks ) {
            if(b.isRepeated || b.elements.size()>1) {
                for( Element e : b.elements ) {
                    owner.getOrCreateElement(e.name).mustBeClass = true;
                }
            }
        }
    }

    /**
     * When this element is an PCDATA-only content model,
     * returns the conversion for it. Otherwise the behavior is undefined.
     */
    private TypeUse getConversion() {
        assert contentModel == Term.EMPTY; // this is PCDATA-only element

        BIElement e = owner.bindInfo.element(name);
        if(e!=null) {
            BIConversion conv = e.getConversion();
            if(conv!=null)
                return conv.getTransducer();
        }
        return CBuiltinLeafInfo.STRING;
    }

    /**
     * Return null if this class is not bound to a class.
     */
    CClassInfo getClassInfo() {
        if(!classInfoComputed) {
            classInfoComputed = true;
            classInfo = calcClass();
        }
        return classInfo;
    }

    private CClassInfo calcClass() {
        BIElement e = owner.bindInfo.element(name);
        if(e==null) {
            if(contentModelType!=DTDEventListener.CONTENT_MODEL_MIXED
            || !attributes.isEmpty()
            || mustBeClass)
                return createDefaultClass();
            if(contentModel!=Term.EMPTY) {
                throw new UnsupportedOperationException("mixed content model not supported");
            } else {
                // just #PCDATA
                if(isReferenced)
                    return null;
                else
                    // if no one else is referencing, assumed to be the root.
                    return createDefaultClass();
            }
        } else {
            return e.clazz;
        }
    }

    private CClassInfo createDefaultClass() {
        String className = owner.model.getNameConverter().toClassName(name);
        QName tagName = new QName("",name);

        return new CClassInfo(owner.model,owner.getTargetPackage(),className,locator,null,tagName,null,null/*TODO*/);
    }

    void bind() {
        CClassInfo  ci = getClassInfo();
        assert ci!=null || attributes.isEmpty();
        for( CPropertyInfo p : attributes )
            ci.addProperty(p);

        switch(contentModelType) {
        case DTDEventListener.CONTENT_MODEL_ANY:
            CReferencePropertyInfo rp = new CReferencePropertyInfo("Content",true,false,true,null,null/*TODO*/,locator, false, false, false);
            rp.setWildcard(WildcardMode.SKIP);
            ci.addProperty(rp);
            return;
        case DTDEventListener.CONTENT_MODEL_CHILDREN:
            break;  // handling follows
        case DTDEventListener.CONTENT_MODEL_MIXED:
            if(contentModel!=Term.EMPTY)
                throw new UnsupportedOperationException("mixed content model unsupported yet");

            if(ci!=null) {
                // if this element is mapped to a class, just put one property
                CValuePropertyInfo p = new CValuePropertyInfo("value", null,null/*TODO*/,locator,getConversion(),null);
                ci.addProperty(p);
            }
            return;
        case DTDEventListener.CONTENT_MODEL_EMPTY:
            // no content model
            assert ci!=null;
            return;
        }

        // normalize
        List<Block> n = new ArrayList<>();
        contentModel.normalize(n,false);

        {// check collision among Blocks
            Set<String> names = new HashSet<>();
            boolean collision = false;

            OUTER:
            for( Block b : n )
                for( Element e : b.elements )
                    if(!names.add(e.name)) {
                        collision = true;
                        break OUTER;
                    }

            if(collision) {
                // collapse all blocks into one
                Block all = new Block(true,true);
                for( Block b : n )
                    all.elements.addAll(b.elements);
                n.clear();
                n.add(all);
            }
        }

        for( Block b : n ) {
            CElementPropertyInfo p;
            if(b.isRepeated || b.elements.size()>1) {
                // collection
                StringBuilder name = new StringBuilder();
                for( Element e : b.elements ) {
                    if(name.length()>0)
                        name.append("Or");
                    name.append(owner.model.getNameConverter().toPropertyName(e.name));
                }
                p = new CElementPropertyInfo(name.toString(), REPEATED_ELEMENT, ID.NONE, null, null,null/*TODO*/, locator, !b.isOptional );
                for( Element e : b.elements ) {
                    CClassInfo child = owner.getOrCreateElement(e.name).getClassInfo();
                    assert child!=null; // we are requiring them to be classes.
                    p.getTypes().add(new CTypeRef(child,new QName("",e.name),null,false,null));
                }
            } else {
                // single property
                String name = b.elements.iterator().next().name;
                String propName = owner.model.getNameConverter().toPropertyName(name);

                TypeUse refType;
                Element ref = owner.getOrCreateElement(name);
                if(ref.getClassInfo()!=null)
                    refType = ref.getClassInfo();
                else {
                    refType = ref.getConversion().getInfo();
                }

                p = new CElementPropertyInfo(propName,
                    refType.isCollection()?REPEATED_VALUE:NOT_REPEATED, ID.NONE, null, null,null/*TODO*/, locator, !b.isOptional );

                p.getTypes().add(new CTypeRef(refType.getInfo(),new QName("",name),null,false,null));
            }
            ci.addProperty(p);
        }
    }

    @Override
    public int compareTo(Element that) {
        return this.name.compareTo(that.name);
    }
}
