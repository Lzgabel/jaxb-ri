/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.tools.xjc.reader;

import java.util.Set;
import java.util.HashSet;

import com.sun.tools.xjc.util.SubtreeCutter;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import cn.glassfish.jaxb.core.v2.util.EditDistance;

import org.xml.sax.helpers.NamespaceSupport;
import org.xml.sax.Locator;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;

import cn.glassfish.jaxb.core.v2.WellKnownNamespace;

import javax.xml.XMLConstants;

/**
 * Common code between {@code DTDExtensionBindingChecker} and {@link ExtensionBindingChecker}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractExtensionBindingChecker extends SubtreeCutter {
    /** Remembers in-scope namespace bindings. */
    protected final NamespaceSupport nsSupport = new NamespaceSupport();

    /**
     * Set of namespace URIs that designates enabled extensions.
     */
    protected final Set<String> enabledExtensions = new HashSet<>();

    private final Set<String> recognizableExtensions = new HashSet<>();

    private Locator locator;

    /**
     * Namespace URI of the target schema language. Elements in this
     * namespace are always allowed.
     */
    protected final String schemaLanguage;

    /**
     * If false, any use of extensions is reported as an error.
     */
    protected final boolean allowExtensions;

    private final Options options;

    /**
     * @param handler
     *      This error handler will receive detected errors.
     */
    public AbstractExtensionBindingChecker( String schemaLanguage, Options options, ErrorHandler handler ) {
        this.schemaLanguage = schemaLanguage;
        this.allowExtensions = options.compatibilityMode!=Options.STRICT;
        this.options = options;
        setErrorHandler(handler);

        for (Plugin plugin : options.getAllPlugins())
            recognizableExtensions.addAll(plugin.getCustomizationURIs());
        recognizableExtensions.add(Const.XJC_EXTENSION_URI);
    }

    /**
     * Verify that the given URI is indeed a valid extension namespace URI,
     * and if so enable it.
     * <p>
     * This method does all the error handling.
     */
    protected final void checkAndEnable(String uri) throws SAXException {
        if( !isRecognizableExtension(uri) ) {
            String nearest = EditDistance.findNearest(uri, recognizableExtensions);
            // not the namespace URI we know of
            error( Messages.ERR_UNSUPPORTED_EXTENSION.format(uri,nearest) );
        } else
        if( !isSupportedExtension(uri) ) {
            // recognizable but not not supported, meaning
            // the plug-in isn't enabled

            // look for plug-in that handles this URI
            Plugin owner = null;
            for( Plugin p : options.getAllPlugins() ) {
                if(p.getCustomizationURIs().contains(uri)) {
                    owner = p;
                    break;
                }
            }
            if(owner!=null)
                // we know the plug-in that supports this namespace, but it's not enabled
                error( Messages.ERR_PLUGIN_NOT_ENABLED.format(owner.getOptionName(),uri));
            else {
                // this shouldn't happen, but be defensive...
                error( Messages.ERR_UNSUPPORTED_EXTENSION.format(uri) );
            }
        }

        // as an error recovery enable this namespace URI anyway.
        enabledExtensions.add(uri);
    }

    /**
     * If the tag name belongs to a plugin namespace-wise, check its local name
     * to make sure it's correct.
     */
    protected final void verifyTagName(String namespaceURI, String localName, String qName) throws SAXException {
        if(options.pluginURIs.contains(namespaceURI)) {
            // make sure that this is a valid tag name
            boolean correct = false;
            for( Plugin p : options.activePlugins ) {
                if(p.isCustomizationTagName(namespaceURI,localName)) {
                    correct = true;
                    break;
                }
            }
            if(!correct) {
                error( Messages.ERR_ILLEGAL_CUSTOMIZATION_TAGNAME.format(qName) );
                startCutting();
            }
        }
    }

    /**
     * Checks if the given namespace URI is supported as the extension
     * bindings.
     */
    protected final boolean isSupportedExtension( String namespaceUri ) {
        return namespaceUri.equals(Const.XJC_EXTENSION_URI) || options.pluginURIs.contains(namespaceUri);
    }

    /**
     * Checks if the given namespace URI can be potentially recognized
     * by this XJC.
     */
    protected final boolean isRecognizableExtension( String namespaceUri ) {
        return recognizableExtensions.contains(namespaceUri);
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        super.setDocumentLocator(locator);
        this.locator = locator;
    }

    @Override
    public void startDocument() throws SAXException {
        super.startDocument();

        nsSupport.reset();
        enabledExtensions.clear();
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        if (XMLConstants.XML_NS_URI.equals(uri)) return;
        super.startPrefixMapping(prefix, uri); //xml prefix shall not be declared based on jdk api javado
        nsSupport.pushContext();
        nsSupport.declarePrefix(prefix,uri);
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        if ("xml".equals(prefix)) return; //xml prefix shall not be declared based on jdk api javadoc
        super.endPrefixMapping(prefix);
        nsSupport.popContext();
    }


    /**
     * Reports an error and returns the created SAXParseException
     */
    protected final SAXParseException error( String msg ) throws SAXException {
        SAXParseException spe = new SAXParseException( msg, locator );
        getErrorHandler().error(spe);
        return spe;
    }

    /**
     * Reports a warning.
     */
    protected final void warning( String msg ) throws SAXException {
        SAXParseException spe = new SAXParseException( msg, locator );
        getErrorHandler().warning(spe);
    }
}
