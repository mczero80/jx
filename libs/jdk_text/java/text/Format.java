/* Format.java -- Abstract superclass for formatting/parsing strings.
   Copyright (C) 1998 Free Software Foundation, Inc.

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.
 
GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
02111-1307 USA.

As a special exception, if you link this library with other files to
produce an executable, this library does not by itself cause the
resulting executable to be covered by the GNU General Public License.
This exception does not however invalidate any other reasons why the
executable file might be covered by the GNU General Public License. */


package java.text;

import java.io.Serializable;

/**
  * This class is the abstract superclass of classes that format and parse
  * data to/from <code>Strings</code>.  It is guaranteed that any 
  * <code>String</code> produced by a concrete subclass of <code>Format</code>
  * will be parseable by that same subclass.
  * <p>
  * In addition to implementing the abstract methods in this class, subclasses
  * should provide static factory methods of the form 
  * <code>getInstance()</code> and <code>getInstance(Locale)</code> if the
  * subclass loads different formatting/parsing schemes based on locale.
  * These subclasses should also implement a static method called
  * <code>getAvailableLocales()</code> which returns an array of 
  * available locales in the current runtime environment.
  *
  * @version 0.0
  *
  * @author Aaron M. Renn (arenn@urbanophile.com)
  */
public abstract class Format implements Serializable, Cloneable
{

/*************************************************************************/

/*
 * Constructors
 */

/**
  * This method initializes a new instance of <code>Format</code>.  It performs
  * no actions, but acts as a default constructor for subclasses.
  */
public
Format()
{
  ;
}

/*************************************************************************/

/*
 * Instance Methods
 */

/**
  * This method formats an <code>Object</code> into a <code>String</code>.
  * 
  * @param obj The <code>Object</code> to format.
  *
  * @return The formatted <code>String</code>.
  *
  * @exception IllegalArgumentException If the <code>Object</code> cannot be formatted.
  */
public final String
format(Object obj) throws IllegalArgumentException
{
  StringBuffer sb = new StringBuffer("");
  FieldPosition fp = new FieldPosition(0);

  format(obj, sb, fp);
  return(sb.toString());
}

/*************************************************************************/

/**
  * This method formats an <code>Object</code> into a <code>String</code> and
  * appends the <code>String</code> to a <code>StringBuffer</code>.
  *
  * @param obj The <code>Object</code> to format.
  * @param sb The <code>StringBuffer</code> to append to.
  * @param pos The desired <code>FieldPosition</code>, which is also updated by this call.
  *
  * @return The updated <code>StringBuffer</code>.
  *
  * @exception IllegalArgumentException If the <code>Object</code> cannot be formatted.
  */
public abstract StringBuffer
format(Object obj, StringBuffer sb, FieldPosition pos) 
       throws IllegalArgumentException;

/*************************************************************************/

/**
  * This method parses a <code>String</code> and converts the parsed 
  * contents into an <code>Object</code>.
  *
  * @param str The <code>String to parse.
  *
  * @return The resulting <code>Object</code>.
  *
  * @exception ParseException If the <code>String</code> cannot be parsed.
  */
public Object
parseObject(String str) throws ParseException
{
  return(parseObject(str, new ParsePosition(0)));
}

/*************************************************************************/

/**
  * This method parses a <code>String</code> and converts the parsed
  * contents into an <code>Object</code>. 
  *
  * @param str The <code>String</code> to parse.
  * @param pos The starting parse index on input, the ending parse index on output.
  *
  * @return The parsed <code>Object</code>.
  *
  * @exception ParseException If the <code>String</code> cannot be parsed.
  */
public abstract Object
parseObject(String str, ParsePosition pos) throws ParseException;

/*************************************************************************/

/**
  * Creates a copy of this object.
  *
  * @return The copied <code>Object</code>.
  */
public Object
clone()
{
  try
    {
      return(super.clone());
    }
  catch(CloneNotSupportedException e)
    {
      return(null);
    }
}

} // class Format

