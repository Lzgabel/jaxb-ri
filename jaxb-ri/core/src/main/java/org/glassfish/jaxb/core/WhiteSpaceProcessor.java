/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package cn.glassfish.jaxb.core;

/**
 * Processes white space normalization.
 *
 * @since 1.0
 */
public abstract class WhiteSpaceProcessor {

// benchmarking (see test/src/ReplaceTest.java in the CVS Attic)
// showed that this code is slower than the current code.
//
//    public static String replace(String text) {
//        final int len = text.length();
//        StringBuffer result = new StringBuffer(len);
//
//        for (int i = 0; i < len; i++) {
//            char ch = text.charAt(i);
//            if (isWhiteSpace(ch))
//                result.append(' ');
//            else
//                result.append(ch);
//        }
//
//        return result.toString();
//    }

    /**
     * Default constructor.
     */
    protected WhiteSpaceProcessor() {
    }

    public static String replace(String text) {
        return replace( (CharSequence)text ).toString();
    }

    /**
     * @since 2.0
     */
    public static CharSequence replace(CharSequence text) {
        int i=text.length()-1;

        // look for the first whitespace char.
        while( i>=0 && !isWhiteSpaceExceptSpace(text.charAt(i)) )
            i--;

        if( i<0 )
            // no such whitespace. replace(text)==text.
            return text;

        // we now know that we need to modify the text.
        // allocate a char array to do it.
        StringBuilder buf = new StringBuilder(text);

        buf.setCharAt(i--,' ');
        for( ; i>=0; i-- )
            if( isWhiteSpaceExceptSpace(buf.charAt(i)))
                buf.setCharAt(i,' ');

        return new String(buf);
    }

    /**
     * Equivalent of {@link String#trim()}.
     * @since 2.0
     */
    public static CharSequence trim(CharSequence text) {
        int len = text.length();
        int start = 0;

        while( start<len && isWhiteSpace(text.charAt(start)) )
            start++;

        int end = len-1;

        while( end>start && isWhiteSpace(text.charAt(end)) )
            end--;

        if(start==0 && end==len-1)
            return text;    // no change
        else
            return text.subSequence(start,end+1);
    }

    public static String collapse(String text) {
        return collapse( (CharSequence)text ).toString();
    }

    /**
     * This is usually the biggest processing bottleneck.
     *
     * @since 2.0
     */
    public static CharSequence collapse(CharSequence text) {
        int len = text.length();

        // most of the texts are already in the collapsed form.
        // so look for the first whitespace in the hope that we will
        // never see it.
        int s=0;
        while(s<len) {
            if(isWhiteSpace(text.charAt(s)))
                break;
            s++;
        }
        if(s==len)
            // the input happens to be already collapsed.
            return text;

        // we now know that the input contains spaces.
        // let's sit down and do the collapsing normally.

        StringBuilder result = new StringBuilder(len /*allocate enough size to avoid re-allocation*/ );

        if(s!=0) {
            for( int i=0; i<s; i++ )
                result.append(text.charAt(i));
            result.append(' ');
        }

        boolean inStripMode = true;
        for (int i = s+1; i < len; i++) {
            char ch = text.charAt(i);
            boolean b = isWhiteSpace(ch);
            if (inStripMode && b)
                continue; // skip this character

            inStripMode = b;
            if (inStripMode)
                result.append(' ');
            else
                result.append(ch);
        }

        // remove trailing whitespaces
        len = result.length();
        if (len > 0 && result.charAt(len - 1) == ' ')
            result.setLength(len - 1);
        // whitespaces are already collapsed,
        // so all we have to do is to remove the last one character
        // if it's a whitespace.

        return result;
    }

    /**
     * Returns true if the specified string is all whitespace.
     */
    public static boolean isWhiteSpace(CharSequence s) {
        for( int i=s.length()-1; i>=0; i-- )
            if(!isWhiteSpace(s.charAt(i)))
                return false;
        return true;
    }

    /** returns true if the specified char is a white space character. */
    public static boolean isWhiteSpace(char ch) {
        // most of the characters are non-control characters.
        // so check that first to quickly return false for most of the cases.
        if( ch>0x20 )   return false;

        // other than we have to do four comparisons.
        return ch == 0x9 || ch == 0xA || ch == 0xD || ch == 0x20;
    }

    /**
     * Returns true if the specified char is a white space character
     * but not 0x20.
     */
    protected static boolean isWhiteSpaceExceptSpace(char ch) {
        // most of the characters are non-control characters.
        // so check that first to quickly return false for most of the cases.
        if( ch>=0x20 )   return false;

        // other than we have to do four comparisons.
        return ch == 0x9 || ch == 0xA || ch == 0xD;
    }
}

