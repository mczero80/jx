/*************************************************************************
/* MessageFormat.java -- Formats text in a language-neutral way
/*
/* Copyright (c) 1999 Free Software Foundation, Inc.
/* Written by Jorge Aliss (jaliss@hotmail.com)
/*
/* This program is free software; you can redistribute it and/or modify
/* it under the terms of the GNU General Public License as published 
/* by the Free Software Foundation, either version 2 of the License, or
/* (at your option) any later version.
/*
/* This program is distributed in the hope that it will be useful, but
/* WITHOUT ANY WARRANTY; without even the implied warranty of
/* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
/* GNU General Public License for more details.
/*
/* You should have received a copy of the GNU General Public License
/* along with this program; if not, write to the Free Software Foundation
/* Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
/*************************************************************************/

package java.text;

import java.text.ParseException;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.Format;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.text.ChoiceFormat;
import java.util.Locale;
import java.util.Vector;

public class MessageFormat extends java.text.Format
{
    public static final int TIME = 1;
    public static final int DATE = 2;
    public static final int NUMBER = 3;
    public static final int CHOICE = 4;
    public static final int STRING = 5;

    /** The pattern */
    private String pattern_d;

    /** The locale */
    private Locale locale_d;

    /** The Format objects for the arguments.*/
    private Vector formats_d;

    /** The elements of the pattern that must be formatted.*/
    private Vector elements_d;

    /** The parts of the pattern that DON'T have to be formatted.*/
    private Vector text_d;

    /**
     * Creates a new MessageFormat object with
     * the specified pattern
     *
     * @param aPattern The Pattern
     */
    public MessageFormat(String aPattern) {
        System.err.println("-- MessageFormat replacement");
        locale_d = Locale.getDefault();
        formats_d = new Vector();
        text_d = new Vector();
        elements_d = new Vector();
        applyPattern(aPattern);
    }

    /**
     * Applies the specified pattern to this MessageFormat.
     *
     * @param aPattern The Pattern
     */
    public void applyPattern(String aPattern) {
        int from = 0;
        int to = 0;
        int depth = 0;
        int textIndex = 0;

        synchronized( formats_d ) {
            // remove the formats of the previous pattern
            formats_d.removeAllElements();
        }

        // remove the text & elements of the previous pattern
        text_d.removeAllElements();
        elements_d.removeAllElements();
        
        // Save the pattern
        pattern_d = aPattern;

        // Look for the elements to be formatted.
        for(int i = 0 ; i < pattern_d.length() ; i++) {
            char c = pattern_d.charAt(i);
            if ( c == '\\' || c == '\'') {
                i++;
                continue;
            }
            if( c == '{' ) {
                depth++;
                if ( depth == 1 ) {
                    from = i;
                }
            } else {
                if( c == '}' ) {
                    depth--;
                    if ( depth == 0 ) {
                        String tmp = removeSingleQuotes(pattern_d.substring(textIndex, from));
                        text_d.addElement( tmp );
                        elements_d.addElement(new MessageElement(pattern_d.substring(from + 1, i)));
                        textIndex = i + 1;
                    }
                }
            }
        }
        // Save the text at the end of the pattern.
        if ( textIndex < pattern_d.length() ) {
            text_d.addElement( removeSingleQuotes(pattern_d.substring( textIndex )) );
        }
    }

    /**
     * Remove single coutes when needed.
     */
    private String removeSingleQuotes(String s) {
        StringBuffer str = new StringBuffer();
        int length = s.length();
        char c;
        
        for(int i = 0 ; i < length ; i++) {
            c = s.charAt(i);
            if ( c == '\'' ) {
                try {
                    if ( s.charAt(i + 1) == '\'' )
                        i++;
                    else
                        continue;
                } catch( IndexOutOfBoundsException e ) {
                    //it's ok to ignore
                }
            }
            str.append( c );
        }
        return str.toString();
    }
            
    /**
     * Overrides Format.clone()
     */
    public Object clone() {
        MessageFormat myClone = (MessageFormat) super.clone();
        myClone.pattern_d = pattern_d;
        myClone.locale_d = (Locale)locale_d.clone();
        myClone.formats_d = (Vector)formats_d.clone();
        myClone.elements_d = (Vector)elements_d.clone();
        myClone.text_d = (Vector)text_d.clone();
        return myClone;
    }

    /**
     * Overrides Format.equals(Object obj)
     */
    public boolean equals(Object obj) {
        if ( obj == this ) {
            return true;
        }

        if ( ! (obj instanceof MessageFormat) ) {
            return false;
        }

        MessageFormat theOther = (MessageFormat)obj;
        if ( pattern_d.equals( theOther.pattern_d ) ) {
            Format f[] = theOther.getFormats();
            synchronized( formats_d ) {
                if ( f.length != formats_d.size() ) {
                    return false;
                }
                for(int i = 0 ; i < f.length ; i++) {
                    /* To do:
                     if ( ! f[i].equals(formats_d.elementAt(i)) )
                     return false;
                     */
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Overrides Format.hashCode()
     */
    public int hashCode() {
        // To do: find a good way to produce a hash code.
        return super.hashCode();
    }

    /**
     * Returns the pattern with the formatted objects.
     *
     * @param source The object to be formatted.
     * @param result The StringBuffer where the text is appened.
     * @param fp A FieldPosition object (it is ignored).
     */
    public final StringBuffer format(Object source, StringBuffer result, FieldPosition fp) {
        //  is this OK?
        return format((Object[])source, result, fp);
    }

    /**
     * Returns the pattern with the formatted objects.
     *
     * @param source The array containing the objects to be formatted.
     * @param result The StringBuffer where the text is appened.
     * @param fp A FieldPosition object (it is ignored).
     */
    public final StringBuffer format(Object source[], StringBuffer result, FieldPosition fp) {
        int size = elements_d.size();
        for(int i = 0; i < size ; i++ ) {
            MessageElement el = (MessageElement) elements_d.elementAt(i);
            Format format = null;
            Object toFormat = source[el.argumentIndex];
            String formatted = null;

            result.append( text_d.elementAt(i) );
            // Get the format for this element
            try {
                format = (Format)formats_d.elementAt( i );
            } catch( ArrayIndexOutOfBoundsException e ) {
                // no format was specified, it's ok.
                // will print the obj's string representation.
            }
            if ( format == null ) {
                formatted = toFormat.toString();
            } else {
                formatted = format.format( toFormat );
                // If the format is a ChoiceFormat
                // it's possible that the string
                // has subformats.
                if ( format instanceof ChoiceFormat ) {
                    // A '{' indicates a subformat so format
                    // has to be called again.
                    if ( formatted.indexOf('{') != -1 ) {
                        // Format the subformat.
                        formatted = MessageFormat.format(formatted, source);
                    }
                }
            }
            result.append( formatted );
        }
        try {
            result.append( text_d.elementAt(size) );
        } catch( ArrayIndexOutOfBoundsException e ) {
            // ok to ignore
        }
        return result;
    }
    
    /**
     * A convinience method to format patterns.
     *
     * @param aPattern The pattern used when formatting.
     * @param arguments The array containing the objects to be formatted.
     */
    public static String format(String aPattern, Object arguments[]) {
        MessageFormat mf = new MessageFormat(aPattern);
        StringBuffer result = new StringBuffer();
        mf.format(arguments, result, new FieldPosition(0));
        return result.toString();
    }

    /**
     * Sets the formats for the arguments.
     *
     * @param formats An array of Format objects.
     */
    public void setFormats(Format formats[]) {
        synchronized ( formats_d ) {
            if ( formats_d.size() != 0 ) {
                formats_d.removeAllElements();
            }
            for(int i = 0 ; i < formats.length ; i++)
                formats_d.addElement( formats[i] );
        }
    }
    
    /**
     * Sets the format for the argument at an specified
     * index.
     *
     * @param index The index.
     * @format The Format object.
     */
    public void setFormat(int index, Format format) {
        synchronized( formats_d ) {
            if ( formats_d.size() < index ) {
                formats_d.setSize(index+1);
            }
            formats_d.setElementAt(format, index);
        }
    }

    /**
     * Returns an array with the Formats for
     * the arguments.
     */
    public Format[] getFormats() {
        Format result[] = null;
        
        synchronized (formats_d) {
            result = new Format[formats_d.size()];
            formats_d.copyInto(result);
        }
        return result;
    }

    /**
     * Sets the locale.
     *
     * @author locale A Locale
     */
    public void setLocale(Locale aLocale) {
        locale_d = aLocale;
    }

    /**
     * Returns the locale.
     *
     */
    public Locale getLocale() {
        return locale_d;
    }

    /**
     * Returns the pattern.
     */
    public String toPattern() {
        return pattern_d;
    }


    public Object[] parse(String source, ParsePosition status) {
        Object result[] = null;
        if ( true ) {
            throw new RuntimeException("public Object[] parse(String source, ParsePosition status) is not implemented yet.");
        }
        return result;
    }

    public Object[] parse(String source) throws ParseException {
        return parse(source, new ParsePosition(0));
    }

    public Object parseObject(String text, ParsePosition status) {
        Object result = null;
        if (true) {
            throw new RuntimeException("public Object parseObject(String text, ParsePosition status) is not implemented yet.");
        }
        return result;
    }

    /**
     * This is a helper class.
     * Represents a MessageElement that must be formatted.
     */
    private class MessageElement {
        /**
         * Creates a new MessageElement and the
         * appropiate Format object for it.
         */
        public MessageElement(String s) {
            int from = 0;
            int to = s.indexOf(',');
            if ( to == -1 ) {
                argumentIndex = Integer.valueOf( s ).intValue();
                type = STRING;
            } else {
                argumentIndex = Integer.valueOf( s.substring(from, to) ).intValue();
                from = to + 1;
                to = s.indexOf(',', from);
                String str = null;
                if ( to != -1 ) {
                    str = s.substring(from, to);
                    style = s.substring(to + 1);
                } else {
                    str = s.substring(from);
                }
                if ( str.equals("time") ) {
                    type = TIME;
                } else if ( str.equals("date") ) {
                    type = DATE;
                } else if ( str.equals("number") ) {
                    type = NUMBER;
                } else if ( str.equals("choice") ) {
                    type = CHOICE;
                } else {
                    type = STRING;
                }
                createFormat();
            }
            // Add the format for this MessageElement
            // to the list of Formats
            formats_d.addElement(format);
        }

        /**
         * Creates a string representation of this object.
         */
        public String toString() {
            return "index = " + argumentIndex + ", type = " + type +
                ", style = " + style;
        }

        /**
         * Creates the format appropiate for this object.
         */
        private void createFormat() {
            switch ( type ) {
            case TIME:
                /* falls through */
            case DATE:
                createDateTimeFormat();
                break;
            case NUMBER:
                createNumberFormat();
                break;
            case CHOICE:
                createChoiceFormat();
                break;
            case STRING:
                // Strings don't need a format class
                format = null;
                break;
            default:
                // Should NOT happen
                throw new RuntimeException("Unreachable reached.");
            }
        }

        /**
         * Creates a DateFormat object
         * with the appropiate style.
         */
        private void createDateTimeFormat() {
            int s = DateFormat.DEFAULT;

            if ( style != null ) {
                if ( style.equals("short") ) {
                    s = DateFormat.SHORT;
                } else if ( style.equals("medium") ) {
                    s = DateFormat.MEDIUM;
                } else if ( style.equals("long") ) {
                    s = DateFormat.LONG;
                } else if ( style.equals("full") ) {
                    s = DateFormat.FULL;
                }
            }
            if ( s == DateFormat.DEFAULT && style != null ) {
                format = new SimpleDateFormat(style, locale_d);
            } else {
                if ( type == TIME ) {
                    format = DateFormat.getTimeInstance(s, locale_d);
                } else {
                    format = DateFormat.getDateInstance(s, locale_d);
                }
            }
        }

        /**
         * Creates a NumberFormat object with
         * the appropiate style.
         */
        private void createNumberFormat() {
            if ( style != null ) {
                if ( style.equals("currency") ) {
                    format = NumberFormat.getCurrencyInstance(locale_d);
                } else if ( style.equals("percent") ) {
                    format = NumberFormat.getPercentInstance(locale_d);
                } else if ( style.equals("integer") ) {
                    NumberFormat f = NumberFormat.getInstance(locale_d);
                    f.setMaximumFractionDigits(0);
                    format = f;
                } else {
                    DecimalFormat f = (DecimalFormat) NumberFormat.getInstance(locale_d);
                    f.applyPattern(style);
                    format = f;
                }
            } else {
                // No style specified, just get a default format
                // for the current locale
                format = NumberFormat.getInstance(locale_d);
            }
        }

        /**
         * Creates a ChoiceFormat object with
         * the appropiate pattern.
         */
        private void createChoiceFormat() {
            format = new ChoiceFormat(style);
        }

        public int argumentIndex;
        public int type;
        public String style;
        private Format format;
    }
}

