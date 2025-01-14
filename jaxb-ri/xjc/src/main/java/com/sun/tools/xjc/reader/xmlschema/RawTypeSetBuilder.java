/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.tools.xjc.reader.xmlschema;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.activation.MimeType;
import javax.xml.namespace.QName;

import com.sun.tools.xjc.model.CAdapter;
import com.sun.tools.xjc.model.CClass;
import com.sun.tools.xjc.model.CClassInfo;
import com.sun.tools.xjc.model.CCustomizations;
import com.sun.tools.xjc.model.CElement;
import com.sun.tools.xjc.model.CElementInfo;
import com.sun.tools.xjc.model.CElementPropertyInfo;
import com.sun.tools.xjc.model.CReferencePropertyInfo;
import com.sun.tools.xjc.model.CTypeRef;
import com.sun.tools.xjc.model.Model;
import com.sun.tools.xjc.model.Multiplicity;
import com.sun.tools.xjc.model.TypeUse;
import com.sun.tools.xjc.reader.RawTypeSet;
import com.sun.tools.xjc.reader.Ring;
import com.sun.tools.xjc.reader.xmlschema.bindinfo.BIDom;
import com.sun.tools.xjc.reader.xmlschema.bindinfo.BIGlobalBinding;
import com.sun.tools.xjc.reader.xmlschema.bindinfo.BIXSubstitutable;
import cn.glassfish.jaxb.core.v2.model.core.ID;
import cn.glassfish.jaxb.core.v2.model.core.WildcardMode;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSModelGroup;
import com.sun.xml.xsom.XSModelGroupDecl;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSWildcard;
import com.sun.xml.xsom.visitor.XSTermVisitor;

/**
 * Builds {@link RawTypeSet} for XML Schema.
 *
 * @author Kohsuke Kawaguchi
 */
public class RawTypeSetBuilder implements XSTermVisitor {
    /**
     * @param optional
     *      if this whole property is optional due to the
     *      occurrence constraints on ancestors, set this to true.
     *      this will prevent the primitive types to be generated.
     */
    public static RawTypeSet build( XSParticle p, boolean optional ) {
        RawTypeSetBuilder rtsb = new RawTypeSetBuilder();
        rtsb.particle(p);
        Multiplicity mul = MultiplicityCounter.theInstance.particle(p);

        if(optional)
            mul = mul.makeOptional();

        return new RawTypeSet(rtsb.refs,mul);
    }

    /**
     * To avoid declaring the same element twice for a content model like
     * (A,A), we keep track of element names here while we are building up
     * this instance.
     */
    private final Set<QName> elementNames = new LinkedHashSet<>();

    private final Set<RawTypeSet.Ref> refs = new LinkedHashSet<>();

    protected final BGMBuilder builder = Ring.get(BGMBuilder.class);

    public RawTypeSetBuilder() {}


    /**
     * Gets the {@link RawTypeSet.Ref}s that were built.
     */
    public Set<RawTypeSet.Ref> getRefs() {
        return refs;
    }

    /**
     * Build up {@link #refs} and compute the total multiplicity of this {@link RawTypeSet.Ref} set.
     */
    private void particle( XSParticle p ) {
        // if the DOM customization is present, bind it like a wildcard
        BIDom dom = builder.getLocalDomCustomization(p);
        if(dom!=null) {
            dom.markAsAcknowledged();
            refs.add(new WildcardRef(WildcardMode.SKIP));
        } else {
            p.getTerm().visit(this);
        }
    }

    @Override
    public void wildcard(XSWildcard wc) {
        refs.add(new WildcardRef(wc));
    }

    @Override
    public void modelGroupDecl(XSModelGroupDecl decl) {
        modelGroup(decl.getModelGroup());
    }

    @Override
    public void modelGroup(XSModelGroup group) {
        for( XSParticle p : group.getChildren())
            particle(p);
    }

    @Override
    public void elementDecl(XSElementDecl decl) {

        QName n = BGMBuilder.getName(decl);
        if(elementNames.add(n)) {
            CElement elementBean = Ring.get(ClassSelector.class).bindToType(decl,null);
            if(elementBean==null)
                refs.add(new XmlTypeRef(decl));
            else {
                // yikes!
                if(elementBean instanceof CClass)
                    refs.add(new CClassRef(decl,(CClass)elementBean));
                else
                    refs.add(new CElementInfoRef(decl,(CElementInfo)elementBean));
            }
        }
    }

    /**
     * Reference to a wildcard.
     */
    public static final class WildcardRef extends RawTypeSet.Ref {
        private final WildcardMode mode;

        WildcardRef(XSWildcard wildcard) {
            this.mode = getMode(wildcard);
        }
        WildcardRef(WildcardMode mode) {
            this.mode = mode;
        }

        private static WildcardMode getMode(XSWildcard wildcard) {
            switch(wildcard.getMode()) {
            case XSWildcard.LAX:
                return WildcardMode.LAX;
            case XSWildcard.STRTICT:
                return WildcardMode.STRICT;
            case XSWildcard.SKIP:
                return WildcardMode.SKIP;
            default:
                throw new IllegalStateException();
            }
        }

        @Override
        protected CTypeRef toTypeRef(CElementPropertyInfo ep) {
            // we don't allow a mapping to typeRef if the wildcard is present
            throw new IllegalStateException();
        }

        @Override
        protected void toElementRef(CReferencePropertyInfo prop) {
            prop.setWildcard(mode);
        }

        @Override
        protected RawTypeSet.Mode canBeType(RawTypeSet parent) {
            return RawTypeSet.Mode.MUST_BE_REFERENCE;
        }

        @Override
        protected boolean isListOfValues() {
            return false;
        }

        @Override
        protected ID id() {
            return ID.NONE;
        }
    }

    /**
     * Reference to a class that maps from an element.
     */
    public static final class CClassRef extends RawTypeSet.Ref {
        public final CClass target;
        public final XSElementDecl decl;

        CClassRef(XSElementDecl decl, CClass target) {
            this.decl = decl;
            this.target = target;
        }

        @Override
        protected CTypeRef toTypeRef(CElementPropertyInfo ep) {
            return new CTypeRef(target,decl);
        }

        @Override
        protected void toElementRef(CReferencePropertyInfo prop) {
            prop.getElements().add(target);
        }

        @Override
        protected RawTypeSet.Mode canBeType(RawTypeSet parent) {
            // if element substitution can occur, no way it can be mapped to a list of types
            if(decl.getSubstitutables().size()>1)
                return RawTypeSet.Mode.MUST_BE_REFERENCE;

            return RawTypeSet.Mode.SHOULD_BE_TYPEREF;
        }

        @Override
        protected boolean isListOfValues() {
            return false;
        }

        @Override
        protected ID id() {
            return ID.NONE;
        }
    }

    /**
     * Reference to a class that maps from an element.
     */
    public final class CElementInfoRef extends RawTypeSet.Ref {
        public final CElementInfo target;
        public final XSElementDecl decl;

        CElementInfoRef(XSElementDecl decl, CElementInfo target) {
            this.decl = decl;
            this.target = target;
        }

        @Override
        protected CTypeRef toTypeRef(CElementPropertyInfo ep) {
            assert !target.isCollection();
            CAdapter a = target.getProperty().getAdapter();
            if(a!=null && ep!=null) ep.setAdapter(a);

            return new CTypeRef(target.getContentType(),decl);
        }

        @Override
        protected void toElementRef(CReferencePropertyInfo prop) {
            prop.getElements().add(target);
        }

        @Override
        protected RawTypeSet.Mode canBeType(RawTypeSet parent) {
            // if element substitution can occur, no way it can be mapped to a list of types
            if(decl.getSubstitutables().size()>1)
                return RawTypeSet.Mode.MUST_BE_REFERENCE;
            // BIXSubstitutable also simulates this effect. Useful for separate compilation
            BIXSubstitutable subst = builder.getBindInfo(decl).get(BIXSubstitutable.class);
            if(subst!=null) {
                subst.markAsAcknowledged();
                return RawTypeSet.Mode.MUST_BE_REFERENCE;
            }

            // we have no place to put an adater if this thing maps to a type
            CElementPropertyInfo p = target.getProperty();
            // if we have an adapter or IDness, which requires special
            // annotation, and there's more than one element,
            // we have no place to put the special annotation, so we need JAXBElement.
            if((parent.refs.size()>1 || !parent.mul.isAtMostOnce()) && p.id()!=ID.NONE)
                return RawTypeSet.Mode.MUST_BE_REFERENCE;
            if(parent.refs.size() > 1 && p.getAdapter() != null)
                return RawTypeSet.Mode.MUST_BE_REFERENCE;

            if(target.hasClass())
                // if the CElementInfo was explicitly bound to a class (which happen if and only if
                // the user requested so, then map that to reference property so that the user sees a class
                return RawTypeSet.Mode.CAN_BE_TYPEREF;
            else
                return RawTypeSet.Mode.SHOULD_BE_TYPEREF;
        }

        @Override
        protected boolean isListOfValues() {
            return target.getProperty().isValueList();
        }

        @Override
        protected ID id() {
            return target.getProperty().id();
        }

        @Override
        protected MimeType getExpectedMimeType() {
            return target.getProperty().getExpectedMimeType();
        }
    }

    /**
     * References to a type. Could be global or local.
     */
    public static final class XmlTypeRef extends RawTypeSet.Ref {
        private final XSElementDecl decl;
        private final TypeUse target;

        public XmlTypeRef(XSElementDecl decl) {
            this.decl = decl;
            SimpleTypeBuilder stb = Ring.get(SimpleTypeBuilder.class);
            stb.refererStack.push(decl);
            TypeUse r = Ring.get(ClassSelector.class).bindToType(decl.getType(),decl);
            stb.refererStack.pop();
            target = r;
        }

        @Override
        protected CTypeRef toTypeRef(CElementPropertyInfo ep) {
            if(ep!=null && target.getAdapterUse()!=null)
                ep.setAdapter(target.getAdapterUse());
            return new CTypeRef(target.getInfo(),decl);
        }

        /**
         * The whole type set can be later bound to a reference property,
         * in which case we need to generate additional code to wrap this
         * type reference into an element class.
         *
         * This method generates such an element class and returns it.
         */
        @Override
        protected void toElementRef(CReferencePropertyInfo prop) {
            CClassInfo scope = Ring.get(ClassSelector.class).getCurrentBean();
            Model model = Ring.get(Model.class);

            CCustomizations custs = Ring.get(BGMBuilder.class).getBindInfo(decl).toCustomizationList();

            if(target instanceof CClassInfo && Ring.get(BIGlobalBinding.class).isSimpleMode()) {
                CClassInfo bean = new CClassInfo(model,scope,
                                model.getNameConverter().toClassName(decl.getName()),
                                decl.getLocator(), null, BGMBuilder.getName(decl), decl,
                                custs);
                bean.setBaseClass((CClassInfo)target);
                prop.getElements().add(bean);
            } else {
                CElementInfo e = new CElementInfo(model,BGMBuilder.getName(decl),scope,target,
                        decl.getDefaultValue(), decl, custs, decl.getLocator());
                prop.getElements().add(e);
            }
        }

        @Override
        protected RawTypeSet.Mode canBeType(RawTypeSet parent) {
            // if we have an adapter or IDness, which requires special
            // annotation, and there's more than one element,
            // we have no place to put the special annotation, so we need JAXBElement.
            if((parent.refs.size()>1 || !parent.mul.isAtMostOnce()) && target.idUse()!=ID.NONE)
                return RawTypeSet.Mode.MUST_BE_REFERENCE;
            if(parent.refs.size() > 1 && target.getAdapterUse() != null)
                return RawTypeSet.Mode.MUST_BE_REFERENCE;

            // nillable and optional at the same time. needs an element wrapper to distinguish those
            // two states. But this is not a hard requirement.
            if(decl.isNillable() && parent.mul.isOptional())
                return RawTypeSet.Mode.CAN_BE_TYPEREF;

            return RawTypeSet.Mode.SHOULD_BE_TYPEREF;
        }

        @Override
        protected boolean isListOfValues() {
            return target.isCollection();
        }

        @Override
        protected ID id() {
            return target.idUse();
        }

        @Override
        protected MimeType getExpectedMimeType() {
            return target.getExpectedMimeType();
        }
    }
}
