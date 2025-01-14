/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.tools.xjc.reader.xmlschema.bindinfo;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlMixed;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import javax.xml.XMLConstants;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.sun.codemodel.JDocComment;
import cn.glassfish.jaxb.core.v2.WellKnownNamespace;
import com.sun.tools.xjc.SchemaCache;
import com.sun.tools.xjc.model.CCustomizations;
import com.sun.tools.xjc.model.CPluginCustomization;
import com.sun.tools.xjc.model.Model;
import com.sun.tools.xjc.reader.Ring;
import com.sun.tools.xjc.reader.xmlschema.BGMBuilder;
import cn.glassfish.jaxb.core.annotation.XmlLocation;
import cn.glassfish.jaxb.core.marshaller.MinimumEscapeHandler;
import com.sun.xml.xsom.XSComponent;

import org.w3c.dom.Element;
import org.xml.sax.Locator;

/**
 * Container for customization declarations.
 *
 * We use JAXB ourselves and parse this object from "xs:annotation".
 *
 * @author
 *     Kohsuke Kawaguchi (kohsuke,kawaguchi@sun.com)
 */
@XmlRootElement(namespace= XMLConstants.W3C_XML_SCHEMA_NS_URI,name="annotation")
@XmlType(namespace=XMLConstants.W3C_XML_SCHEMA_NS_URI,name="foobar")
public final class BindInfo implements Iterable<BIDeclaration> {

    private BGMBuilder builder;

    @XmlLocation
    private Locator location;

    /**
     * Documentation taken from {@code <xs:documentation>s}.
     */
    @XmlElement(namespace=XMLConstants.W3C_XML_SCHEMA_NS_URI)
    private Documentation documentation;

    /**
     * Returns true if this {@link BindInfo} doesn't contain any useful
     * information.
     *
     * This flag is used to discard unused {@link BindInfo}s early to save memory footprint.
     */
    public boolean isPointless() {
        if(size()>0)     return false;
        if(documentation!=null && !documentation.contents.isEmpty())
            return false;

        return true;
    }

    private static final class Documentation {
        @XmlAnyElement
        @XmlMixed
        List<Object> contents = new ArrayList<>();

        void addAll(Documentation rhs) {
            if(rhs==null)   return;

            if(contents==null)
                contents = new ArrayList<>();
            if(!contents.isEmpty())
                contents.add("\n\n");
            contents.addAll(rhs.contents);
        }
    }

    /** list of individual declarations. */
    private final List<BIDeclaration> decls = new ArrayList<>();

    private static final class AppInfo {
        /**
         * Receives {@link BIDeclaration}s and other DOMs.
         */
        @XmlAnyElement(lax=true,value=DomHandlerEx.class)
        List<Object> contents = new ArrayList<>();

        public void addTo(BindInfo bi) {
            if(contents==null)  return;

            for (Object o : contents) {
                if(o instanceof BIDeclaration)
                    bi.addDecl((BIDeclaration)o);
                // this is really PITA! I can't get the source location
                if(o instanceof DomHandlerEx.DomAndLocation) {
                    DomHandlerEx.DomAndLocation e = (DomHandlerEx.DomAndLocation)o;
                    String nsUri = e.element.getNamespaceURI();
                    if(nsUri==null || nsUri.equals("")
                    || XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(nsUri))
                        continue;   // this is definitely not a customization
                    bi.addDecl(new BIXPluginCustomization(e.element,e.loc));
                }
            }
        }
    }


    // only used by JAXB
    @XmlElement(namespace=XMLConstants.W3C_XML_SCHEMA_NS_URI)
    void setAppinfo(AppInfo aib) {
        aib.addTo(this);
    }



    /**
     * Gets the location of this annotation in the source file.
     *
     * @return
     *      If the declarations are in fact specified in the source
     *      code, a non-null valid object will be returned.
     *      If this BindInfo is generated internally by XJC, then
     *      null will be returned.
     */
    public Locator getSourceLocation() { return location; }


    private XSComponent owner;
    /**
     * Sets the owner schema component and a reference to BGMBuilder.
     * This method is called from the BGMBuilder before
     * any BIDeclaration inside it is used.
     */
    public void setOwner( BGMBuilder _builder, XSComponent _owner ) {
        this.owner = _owner;
        this.builder = _builder;
        for (BIDeclaration d : decls)
            d.onSetOwner();
    }
    public XSComponent getOwner() { return owner; }

    /**
     * Back pointer to the BGMBuilder which is building
     * a BGM from schema components including this customization.
     */
    public BGMBuilder getBuilder() { return builder; }

    /** Adds a new declaration. */
    public void addDecl( BIDeclaration decl ) {
        if(decl==null)  throw new IllegalArgumentException();
        decl.setParent(this);
        decls.add(decl);
    }

    /**
     * Gets the first declaration with a given name, or null
     * if none is found.
     */
    public <T extends BIDeclaration>
    T get( Class<T> kind ) {
        for( BIDeclaration decl : decls ) {
            if( kind.isInstance(decl) )
                return kind.cast(decl);
        }
        return null; // not found
    }

    /**
     * Gets all the declarations
     */
    public BIDeclaration[] getDecls() {
        return decls.toArray(new BIDeclaration[decls.size()]);
    }

    /**
     * Gets the documentation parsed from {@code <xs:documentation>}s.
     * The returned collection is to be added to {@link JDocComment#append(Object)}.
     * @return  maybe null.
     */
    public String getDocumentation() {
        // TODO: FIXME: correctly turn individual items to String including DOM
        if(documentation==null || documentation.contents==null) return null;

        StringBuilder buf = new StringBuilder();
        for (Object c : documentation.contents) {
            if(c instanceof String) {
                buf.append(c.toString());
            }
            if(c instanceof Element) {
                Transformer t = builder.getIdentityTransformer();
                StringWriter w = new StringWriter();
                try {
                    Writer fw = new FilterWriter(w) {
                        char[] buf = new char[1];

                        @Override
                        public void write(int c) throws IOException {
                            buf[0] = (char)c;
                            write(buf,0,1);
                        }

                        @Override
                        public void write(char[] cbuf, int off, int len) throws IOException {
                            MinimumEscapeHandler.theInstance.escape(cbuf,off,len,false,out);
                        }

                        @Override
                        public void write(String str, int off, int len) throws IOException {
                            write(str.toCharArray(),off,len);
                        }
                    };
                    t.transform(new DOMSource((Element)c),new StreamResult(fw));
                } catch (TransformerException e) {
                    throw new Error(e); // impossible
                }
                buf.append("\n<pre>\n");
                buf.append(w);
                buf.append("\n</pre>\n");
            }
        }
        return buf.toString();
    }

    /**
     * Merges all the declarations inside the given BindInfo
     * to this BindInfo.
     */
    public void absorb( BindInfo bi ) {
        for( BIDeclaration d : bi )
            d.setParent(this);
        this.decls.addAll( bi.decls );

        if(this.documentation==null)
            this.documentation = bi.documentation;
        else
            this.documentation.addAll(bi.documentation);
    }

    /** Gets the number of declarations. */
    public int size() { return decls.size(); }

    public BIDeclaration get( int idx ) { return decls.get(idx); }

    @Override
    public Iterator<BIDeclaration> iterator() {
        return decls.iterator();
    }

    /**
     * Gets the list of {@link CPluginCustomization}s from this.
     *
     * <p>
     * Note that calling this method marks all those plug-in customizations
     * as 'used'. So call it only when it's really necessary.
     */
    public CCustomizations toCustomizationList() {
        CCustomizations r=null;
        for( BIDeclaration d : this ) {
            if(d instanceof BIXPluginCustomization) {
                BIXPluginCustomization pc = (BIXPluginCustomization) d;
                pc.markAsAcknowledged();
                if(!Ring.get(Model.class).options.pluginURIs.contains(pc.getName().getNamespaceURI()))
                    continue;   // this isn't a plugin customization
                if(r==null)
                    r = new CCustomizations();
                r.add(new CPluginCustomization(pc.element,pc.getLocation()));
            }
        }

        if(r==null)     r = CCustomizations.EMPTY;
        return new CCustomizations(r);
    }
    /** An instance with the empty contents. */
    public final static BindInfo empty = new BindInfo();

    /**
     * Lazily prepared {@link JAXBContext}.
     */
    private static volatile JAXBContext customizationContext;

    public static JAXBContext getCustomizationContext() {
        try {
            if (customizationContext == null) {
                synchronized (BindInfo.class) {
                    if (customizationContext == null) {
                        customizationContext = JAXBContext.newInstance(
                                BindInfo.class, // for xs:annotation
                                BIClass.class,
                                BIConversion.User.class,
                                BIConversion.UserAdapter.class,
                                BIDom.class,
                                BIFactoryMethod.class,
                                BIInlineBinaryData.class,
                                BIXDom.class,
                                BIXSubstitutable.class,
                                BIEnum.class,
                                BIEnumMember.class,
                                BIGlobalBinding.class,
                                BIProperty.class,
                                BISchemaBinding.class);
                    }
                }
            }
            return customizationContext;
        } catch (JAXBException e) {
            throw new AssertionError(e);
        }
    }

    public static Unmarshaller getCustomizationUnmarshaller() {
        try {
            return getCustomizationContext().createUnmarshaller();
        } catch (JAXBException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Lazily parsed schema for the binding file.
     */
    public static final SchemaCache bindingFileSchema = new SchemaCache("binding.xsd", BindInfo.class, true);
}

