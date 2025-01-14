/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.tools.xjc.model;

import java.awt.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import jakarta.activation.DataHandler;
import jakarta.activation.MimeType;
import jakarta.xml.bind.DatatypeConverter;
import jakarta.xml.bind.annotation.XmlIDREF;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.HexBinaryAdapter;
import jakarta.xml.bind.annotation.adapters.NormalizedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import javax.xml.XMLConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JType;
import com.sun.tools.xjc.model.nav.NClass;
import com.sun.tools.xjc.outline.Aspect;
import cn.glassfish.jaxb.core.v2.model.annotation.Locatable;
import cn.glassfish.jaxb.core.v2.model.core.BuiltinLeafInfo;
import cn.glassfish.jaxb.core.v2.model.core.Element;
import cn.glassfish.jaxb.core.v2.model.core.LeafInfo;
import cn.glassfish.jaxb.core.v2.runtime.Location;
import com.sun.tools.xjc.model.nav.NType;
import com.sun.tools.xjc.model.nav.NavigatorImpl;
import com.sun.tools.xjc.outline.Outline;
import com.sun.tools.xjc.runtime.ZeroOneBooleanAdapter;
import com.sun.tools.xjc.util.NamespaceContextAdapter;
import cn.glassfish.jaxb.core.v2.WellKnownNamespace;
import cn.glassfish.jaxb.core.v2.model.core.ID;
import com.sun.xml.xsom.XSComponent;
import com.sun.xml.xsom.XmlString;

import org.xml.sax.Locator;

/**
 * Encapsulates the default handling for leaf classes (which are bound
 * to text in XML.) In particular this class knows how to convert
 * the lexical value into the Java class according to this default rule.
 *
 * <p>
 * This represents the spec-defined default handling for the Java
 * type ({@link #getType()}.
 *
 * <p>
 * For those Java classes (such as {@link String} or {@link Boolean})
 * where the spec designates a specific default handling, there are
 * constants in this class (such as {@link #STRING} or {@link #BOOLEAN}.)
 *
 * <p>
 * The generated type-safe enum classes are also a leaf class,
 * and as such there are {@link CEnumLeafInfo} that represents it
 * as {@link CBuiltinLeafInfo}.
 *
 * <p>
 * This class represents the <b>default handling</b>, and therefore
 * we can only have one instance per one {@link NType}. Handling of
 * other XML Schema types (such as xs:token) are represented as
 * a general {@link TypeUse} objects.
 *
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class CBuiltinLeafInfo implements CNonElement, BuiltinLeafInfo<NType,NClass>, LeafInfo<NType,NClass>, Location {

    private final NType type;
    /**
     * Can be null for anonymous types.
     */
    private final QName typeName;

    private final QName[] typeNames;

    private final ID id;

    // no derived class other than the spec-defined ones. definitely not for enum.
    private CBuiltinLeafInfo(NType typeToken, ID id, QName... typeNames) {
        this.type = typeToken;
        this.typeName = typeNames.length>0?typeNames[0]:null;
        this.typeNames = typeNames;
        this.id = id;
    }

    /**
     * Gets the code model representation of this type.
     */
    @Override
    public JType toType(Outline o, Aspect aspect) {
        return getType().toType(o,aspect);
    }

    /**
     * Since {@link CBuiltinLeafInfo} represents a default binding,
     * it is never a collection.
     */
    @Deprecated
    @Override
    public final boolean isCollection() {
        return false;
    }

    /**
     * Guaranteed to return this.
     */
    @Deprecated
    @Override
    public CNonElement getInfo() {
        return this;
    }

    @Override
    public ID idUse() {
        return id;
    }

    /**
     * {@link CBuiltinLeafInfo} never has a default associated MIME type.
     */
    @Override
    public MimeType getExpectedMimeType() {
        return null;
    }

    @Deprecated
    @Override
    public final CAdapter getAdapterUse() {
        return null;
    }

    @Override
    public Locator getLocator() {
        return Model.EMPTY_LOCATOR;
    }

    @Override
    public final XSComponent getSchemaComponent() {
        throw new UnsupportedOperationException("TODO. If you hit this, let us know.");
    }

    /**
     * Creates a {@link TypeUse} that represents a collection of this {@link CBuiltinLeafInfo}.
     */
    public final TypeUse makeCollection() {
        return TypeUseFactory.makeCollection(this);
    }

    /**
     * Creates a {@link TypeUse} that represents an adapted use of this {@link CBuiltinLeafInfo}.
     */
    public final TypeUse makeAdapted( Class<? extends XmlAdapter> adapter, boolean copy ) {
        return TypeUseFactory.adapt(this,adapter,copy);
    }

    /**
     * Creates a {@link TypeUse} that represents a MIME-type assocaited version of this {@link CBuiltinLeafInfo}.
     */
    public final TypeUse makeMimeTyped( MimeType mt ) {
        return TypeUseFactory.makeMimeTyped(this,mt);
    }

    /**
     * @deprecated always return false at this level.
     */
    @Deprecated
    @Override
    public final boolean isElement() {
        return false;
    }

    /**
     * @deprecated always return null at this level.
     */
    @Deprecated
    @Override
    public final QName getElementName() {
        return null;
    }

    /**
     * @deprecated always return null at this level.
     */
    @Deprecated
    @Override
    public final Element<NType,NClass> asElement() {
        return null;
    }

    /**
     * A reference to the representation of the type.
     */
    @Override
    public NType getType() {
        return type;
    }

    /**
     * Returns all the type names recognized by this bean info.
     *
     * @return
     *      do not modify the returned array.
     */
    public final QName[] getTypeNames() {
        return typeNames.clone();
    }

    /**
     * Leaf-type cannot be referenced from IDREF.
     *
     * @deprecated
     *      why are you calling a method whose return value is always known?
     */
    @Deprecated
    @Override
    public final boolean canBeReferencedByIDREF() {
        return false;
    }

    @Override
    public QName getTypeName() {
        return typeName;
    }

    @Override
    public Locatable getUpstream() {
        return null;
    }

    @Override
    public Location getLocation() {
        // this isn't very accurate, but it's not too bad
        // doing it correctly need leaves to hold navigator.
        // otherwise revisit the design so that we take navigator as a parameter
        return this;
    }

    @Override
    public boolean isSimpleType() {
        return true;
    }

    /**
     * {@link CBuiltinLeafInfo} for Java classes that have
     * the spec defined built-in binding semantics.
     */
    private static abstract class Builtin extends CBuiltinLeafInfo {
        protected Builtin(Class<?> c, String typeName) {
            this(c,typeName,cn.glassfish.jaxb.core.v2.model.core.ID.NONE);
        }
        protected Builtin(Class<?> c, String typeName, ID id) {
            super(NavigatorImpl.theInstance.ref(c), id, new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI,typeName));
            LEAVES.put(getType(),this);
        }

        /**
         * No vendor customization in the built-in classes.
         */
        @Override
        public CCustomizations getCustomizations() {
            return CCustomizations.EMPTY;
        }
    }

    private static final class NoConstantBuiltin extends Builtin {
        public NoConstantBuiltin(Class<?> c, String typeName) {
            super(c, typeName);
        }
        @Override
        public JExpression createConstant(Outline outline, XmlString lexical) {
            return null;
        }
    }

    /**
     * All built-in leaves.
     */
    public static final Map<NType,CBuiltinLeafInfo> LEAVES = new HashMap<>();


    public static final CBuiltinLeafInfo ANYTYPE = new NoConstantBuiltin(Object.class,"anyType");
    public static final CBuiltinLeafInfo STRING = new Builtin(String.class,"string") {
            @Override
            public JExpression createConstant(Outline outline, XmlString lexical) {
                return JExpr.lit(lexical.value);
            }
    };
    public static final CBuiltinLeafInfo BOOLEAN = new Builtin(Boolean.class,"boolean") {
            @Override
            public JExpression createConstant(Outline outline, XmlString lexical) {
                return JExpr.lit(DatatypeConverter.parseBoolean(lexical.value));
            }
    };
    public static final CBuiltinLeafInfo INT = new Builtin(Integer.class,"int") {
        @Override
        public JExpression createConstant(Outline outline, XmlString lexical) {
            return JExpr.lit(DatatypeConverter.parseInt(lexical.value));
        }
    };
    public static final CBuiltinLeafInfo LONG = new Builtin(Long.class,"long") {
        @Override
        public JExpression createConstant(Outline outline, XmlString lexical) {
            return JExpr.lit(DatatypeConverter.parseLong(lexical.value));
        }
    };
    public static final CBuiltinLeafInfo BYTE = new Builtin(Byte.class,"byte") {
        @Override
        public JExpression createConstant(Outline outline, XmlString lexical) {
            return JExpr.cast(
                    outline.getCodeModel().BYTE,
                    JExpr.lit(DatatypeConverter.parseByte(lexical.value)));
        }
    };
    public static final CBuiltinLeafInfo SHORT = new Builtin(Short.class,"short") {
        @Override
        public JExpression createConstant(Outline outline, XmlString lexical) {
            return JExpr.cast(
                    outline.getCodeModel().SHORT,
                    JExpr.lit(DatatypeConverter.parseShort(lexical.value)));
        }
    };
    public static final CBuiltinLeafInfo FLOAT = new Builtin(Float.class,"float") {
        @Override
        public JExpression createConstant(Outline outline, XmlString lexical) {
            return JExpr.lit(DatatypeConverter.parseFloat(lexical.value));
        }
    };
    public static final CBuiltinLeafInfo DOUBLE = new Builtin(Double.class,"double") {
        @Override
        public JExpression createConstant(Outline outline, XmlString lexical) {
            return JExpr.lit(DatatypeConverter.parseDouble(lexical.value));
        }
    };
    public static final CBuiltinLeafInfo QNAME = new Builtin(QName.class,"QName") {
        @Override
        public JExpression createConstant(Outline outline, XmlString lexical) {
            QName qn = DatatypeConverter.parseQName(lexical.value,new NamespaceContextAdapter(lexical));
            return JExpr._new(outline.getCodeModel().ref(QName.class))
                .arg(qn.getNamespaceURI())
                .arg(qn.getLocalPart())
                .arg(qn.getPrefix());
        }
    };
    // XMLGregorianCalendar is mutable, so we can't support default values anyhow.
	// For CALENAR we are uses a most unlikely name so as to avoid potential name
	// conflicts in the furture.
	public static final CBuiltinLeafInfo CALENDAR = new NoConstantBuiltin(XMLGregorianCalendar.class,"\u0000");
    public static final CBuiltinLeafInfo DURATION = new NoConstantBuiltin(Duration.class,"duration");

    public static final CBuiltinLeafInfo BIG_INTEGER = new Builtin(BigInteger.class,"integer") {
        @Override
        public JExpression createConstant(Outline outline, XmlString lexical) {
            return JExpr._new(outline.getCodeModel().ref(BigInteger.class)).arg(lexical.value.trim());
        }
    };

    public static final CBuiltinLeafInfo BIG_DECIMAL = new Builtin(BigDecimal.class,"decimal") {
        @Override
        public JExpression createConstant(Outline outline, XmlString lexical) {
            return JExpr._new(outline.getCodeModel().ref(BigDecimal.class)).arg(lexical.value.trim());
        }
    };

    public static final CBuiltinLeafInfo BASE64_BYTE_ARRAY = new Builtin(byte[].class,"base64Binary") {
        @Override
        public JExpression createConstant(Outline outline, XmlString lexical) {
            return outline.getCodeModel().ref(DatatypeConverter.class).staticInvoke("parseBase64Binary").arg(lexical.value);
        }
    };

    public static final CBuiltinLeafInfo DATA_HANDLER = new NoConstantBuiltin(DataHandler.class,"base64Binary");
    public static final CBuiltinLeafInfo IMAGE = new NoConstantBuiltin(Image.class,"base64Binary");
    public static final CBuiltinLeafInfo XML_SOURCE = new NoConstantBuiltin(Source.class,"base64Binary");

    public static final TypeUse HEXBIN_BYTE_ARRAY =
        STRING.makeAdapted(HexBinaryAdapter.class,false);


    // TODO: not sure if they should belong here,
    // but I couldn't find other places that fit.
    public static final TypeUse TOKEN =
            STRING.makeAdapted(CollapsedStringAdapter.class,false);

    public static final TypeUse NORMALIZED_STRING =
            STRING.makeAdapted(NormalizedStringAdapter.class,false);

    public static final TypeUse ID = TypeUseFactory.makeID(TOKEN,cn.glassfish.jaxb.core.v2.model.core.ID.ID);

    /**
     * boolean restricted to 0 or 1.
     */
    public static final TypeUse BOOLEAN_ZERO_OR_ONE =
            STRING.makeAdapted(ZeroOneBooleanAdapter.class,true);

    /**
     * IDREF.
     *
     * IDREF is has a whitespace normalization semantics of token, but
     * we don't want {@link XmlJavaTypeAdapter} and {@link XmlIDREF} to interact.
     */
    public static final TypeUse IDREF = TypeUseFactory.makeID(ANYTYPE,cn.glassfish.jaxb.core.v2.model.core.ID.IDREF);

    /**
     * For all list of strings, such as NMTOKENS, ENTITIES.
     */
    public static final TypeUse STRING_LIST =
            STRING.makeCollection();
}
