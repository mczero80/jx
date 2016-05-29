/* NumberFormat.java -- Formats and parses numbers
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
import java.util.Locale;

/**
  * This is the abstract superclass of all classes which format and 
  * parse numeric values such as decimal numbers, integers, currency values,
  * and percentages.  These classes perform their parsing and formatting
  * in a locale specific manner, accounting for such items as differing
  * currency symbols and thousands separators.
  * <p>
  * To create an instance of a concrete subclass of <code>NumberFormat</code>,
  * do not call a class constructor directly.  Instead, use one of the
  * static factory methods in this class such as 
  * <code>getCurrencyInstance</code>.
  * 
  * Implements readObject/writeObject
  *
  * @version 0.0
  *
  * @author Aaron M. Renn (arenn@urbanophile.com)
  */
public abstract class NumberFormat extends Format implements Serializable,
                                                             Cloneable
{

/*************************************************************************/

/*
 * Class Variables
 */

/**
  * This is a constant used to create a <code>FieldPosition</code> object
  * that will return the integer portion of a formatted number.
  */
public static final int INTEGER_FIELD = 0;

/**
  * This is a constant used to create a <code>FieldPosition</code> object
  * that will return the fractional portion of a formatted number.
  */
public static final int FRACTION_FIELD = 1;

/*************************************************************************/

/*
 * Instance Variables
 */

boolean groupingUsed;
byte maxFractionDigits;
int maximumFractionDigits;
int maximumIntegerDigits;
int maxIntegerDigits;
int minFractionDigits;
int minimumFractionDigits;
int minIntegerDigits;
int minimumIntegerDigits;
boolean parseIntegerOnly;
int serialVersionOnStream;

/*************************************************************************/

/*
 * Class Methods
 */

/**
  * This method returns a default instance for the default locale. This
  * will be a concrete subclass of <code>NumberFormat</code>, but the 
  * actual class returned is dependent on the locale.
  *
  * @return An instance of the default <code>NumberFormat</code> class.
  */
public static final NumberFormat
getInstance()
{
  return(getInstance(Locale.getDefault()));
}

/*************************************************************************/

/**
  * This method returns a default instance for the specified locale. This
  * will be a concrete subclass of <code>NumberFormat</code>, but the 
  * actual class returned is dependent on the locale.
  *
  * @param locale The desired locale.
  *
  * @return An instance of the default <code>NumberFormat</code> class.
  */
public static NumberFormat
getInstance(Locale locale)
{
  return(getNumberInstance(locale));
}

/*************************************************************************/

/**
  * This method returns a general purpose number formatting and parsing
  * class for the default locale.  This will be a concrete subclass of
  * <code>NumberFormat</code>, but the actual class returned is dependent
  * on the locale.
  *
  * @return An instance of a generic number formatter for the default locale.
  */
public static final NumberFormat
getNumberInstance()
{
  return(getNumberInstance(Locale.getDefault()));
}

/*************************************************************************/

/**
  * This method returns a general purpose number formatting and parsing
  * class for the specified locale.  This will be a concrete subclass of
  * <code>NumberFormat</code>, but the actual class returned is dependent
  * on the locale.
  *
  * @param locale The desired locale.
  *
  * @return An instance of a generic number formatter for the specified locale.
  */
public static NumberFormat
getNumberInstance(Locale locale)
{
  ; //*** Implement me
  return(null);
}

/*************************************************************************/

/**
  * This method returns an instance of <code>NumberFormat</code> suitable
  * for formatting and parsing currency values in the default locale.
  *
  * @return An instance of <code>NumberFormat</code> for handling currencies.
  */
public static final NumberFormat
getCurrencyInstance()
{
  return(getCurrencyInstance(Locale.getDefault()));
}

/*************************************************************************/

/**
  * This method returns an instance of <code>NumberFormat</code> suitable
  * for formatting and parsing currency values in the specified locale.
  *
  * @return An instance of <code>NumberFormat</code> for handling currencies.
  */
public static NumberFormat
getCurrencyInstance(Locale locale)
{
  ; //*******Implement me
  return(null);
}

/*************************************************************************/

/**
  * This method returns an instance of <code>NumberFormat</code> suitable
  * for formatting and parsing percentage values in the default locale.
  *
  * @return An instance of <code>NumberFormat</code> for handling percentages.
  */
public static final NumberFormat
getPercentInstance()
{
  return(getPercentInstance(Locale.getDefault()));
}

/*************************************************************************/

/**
  * This method returns an instance of <code>NumberFormat</code> suitable
  * for formatting and parsing percentage values in the specified locale.
  *
  * @param locale The desired locale.
  *
  * @return An instance of <code>NumberFormat</code> for handling percentages.
  */
public static NumberFormat
getPercentInstance(Locale locale)
{
  ; //*******Implement me
  return(null);
}

/*************************************************************************/

/**
  * This method returns a list of locales for which concrete instances
  * of <code>NumberFormat</code> subclasses may be created.
  *
  * @return The list of available locales.
  */
public static Locale[]
getAvailableLocales()
{
  Locale[] list = new Locale[1];
  list[0] = Locale.ENGLISH;

  return(list);
}

/*************************************************************************/

/*
 * Constructors
 */

/**
  * This is a default constructor for use by subclasses.
  */
public
NumberFormat()
{
  ;
}
  
/*************************************************************************/

/*
 * Instace Variables
 */

/**
  * This method returns the maximum number of digits allowed in the integer
  * portion of a number.
  *
  * @return The maximum number of digits allowed in the integer portion of a number.
  */
public int
getMaximumIntegerDigits()
{
  return(maximumIntegerDigits);
}

/*************************************************************************/

/**
  * This method sets the maximum number of digits allowed in the integer
  * portion of a number to the specified value.  If this is less than the
  * current minimum allowed digits, the minimum allowed digits value will
  * be lowered to be equal to the new maximum allowed digits value.
  *
  * @param maximumIntegerDigits The new maximum integer digits value.
  */
public void
setMaximumIntegerDigits(int maximumIntegerDigits)
{
  this.maximumIntegerDigits = maximumIntegerDigits;

  if (getMinimumIntegerDigits() > maximumIntegerDigits)
    setMinimumIntegerDigits(maximumIntegerDigits);
}

/*************************************************************************/

/**
  * This method returns the minimum number of digits allowed in the integer
  * portion of a number.
  *
  * @return The minimum number of digits allowed in the integer portion of a number.
  */
public int
getMinimumIntegerDigits()
{
  return(minimumIntegerDigits);
}

/*************************************************************************/

/**
  * This method sets the minimum number of digits allowed in the integer
  * portion of a number to the specified value.  If this is greater than the
  * current maximum allowed digits, the maximum allowed digits value will
  * be raised to be equal to the new minimum allowed digits value.
  *
  * @param minimumIntegerDigits The new minimum integer digits value.
  */
public void
setMinimumIntegerDigits(int minimumIntegerDigits)
{
  this.minimumIntegerDigits = minimumIntegerDigits;

  if (getMaximumIntegerDigits() < minimumIntegerDigits)
    setMaximumIntegerDigits(minimumIntegerDigits);
}

/*************************************************************************/

/**
  * This method returns the maximum number of digits allowed in the fraction
  * portion of a number.
  *
  * @return The maximum number of digits allowed in the fraction portion of a number.
  */
public int
getMaximumFractionDigits()
{
  return(maximumFractionDigits);
}

/*************************************************************************/

/**
  * This method sets the maximum number of digits allowed in the fraction
  * portion of a number to the specified value.  If this is less than the
  * current minimum allowed digits, the minimum allowed digits value will
  * be lowered to be equal to the new maximum allowed digits value.
  *
  * @param maximumFractionDigits The new maximum fraction digits value.
  */
public void
setMaximumFractionDigits(int maximumFractionDigits)
{
  this.maximumFractionDigits = maximumFractionDigits;

  if (getMinimumFractionDigits() > maximumFractionDigits)
    setMinimumFractionDigits(maximumFractionDigits);
}

/*************************************************************************/

/**
  * This method returns the minimum number of digits allowed in the fraction
  * portion of a number.
  *
  * @return The minimum number of digits allowed in the fraction portion of a number.
  */
public int
getMinimumFractionDigits()
{
  return(minimumFractionDigits);
}

/*************************************************************************/

/**
  * This method sets the minimum number of digits allowed in the fraction
  * portion of a number to the specified value.  If this is greater than the
  * current maximum allowed digits, the maximum allowed digits value will
  * be raised to be equal to the new minimum allowed digits value.
  *
  * @param minimumFractionDigits The new minimum fraction digits value.
  */
public void
setMinimumFractionDigits(int minimumFractionDigits)
{
  this.minimumFractionDigits = minimumFractionDigits;

  if (getMaximumFractionDigits() < minimumFractionDigits)
    setMaximumFractionDigits(minimumFractionDigits);
}

/*************************************************************************/

/**
  * This method tests whether or not grouping is in use.  Grouping is
  * a method of marking separations in numbers, such as thousand separators
  * in the US English locale.  The grouping positions and symbols are all
  * locale specific.  As an example, with grouping disabled, the number one
  * million would appear as "1000000".  With grouping enabled, this number
  * might appear as "1,000,000".  (Both of these assume the US English
  * locale).
  *
  * @return <code>true</code> if grouping is enabled, <code>false</code> otherwise.
  */
public boolean
isGroupingUsed()
{
  return(groupingUsed);
}

/*************************************************************************/

/**
  * This method sets the grouping behavior of this formatter.  Grouping is
  * a method of marking separations in numbers, such as thousand separators
  * in the US English locale.  The grouping positions and symbols are all
  * locale specific.  As an example, with grouping disabled, the number one
  * million would appear as "1000000".  With grouping enabled, this number
  * might appear as "1,000,000".  (Both of these assume the US English
  * locale).
  *
  * @param groupingUsed <code>true</code> to enable grouping, <code>false</code> to disable it.
  */
public void
setGroupingUsed(boolean groupingUsed)
{
  this.groupingUsed = groupingUsed;
}

/*************************************************************************/

/**
  * This method tests whether or not only integer values should be parsed.
  * If this class is parsing only integers, parsing stops at the decimal
  * point.
  *
  * @return <code>true</code> if only integers are parsed, <code>false</code> otherwise.
  */
public boolean
isParseIntegerOnly()
{
  return(parseIntegerOnly);
}

/*************************************************************************/

/** 
  * This method sets the parsing behavior of this object to parse only 
  * integers or not.
  *
  * @param parseIntegerOnly <code>true</code> to parse only integers, <code>false</code> otherwise.
  */
public void
setParseIntegerOnly(boolean parseIntegerOnly)
{
  this.parseIntegerOnly = parseIntegerOnly;
}

/*************************************************************************/

/**
  * This method is a specialization of the format method that performs
  * a simple formatting of the specified <code>long</code> number.
  *
  * @param number The <code>long</code> to format.
  *
  * @return The formatted number
  */
public final String format(long number)
{
  StringBuffer sb = new StringBuffer("");

  format(number, sb, new FieldPosition(INTEGER_FIELD));
  return(sb.toString());
}

/*************************************************************************/

/**
  * This method is a specialization of the format method that performs
  * a simple formatting of the specified <code>double</code> number.
  *
  * @param number The <code>double</code> to format.
  *
  * @return The formatted number
  */
public final String format(double number)
{
  StringBuffer sb = new StringBuffer("");

  format(number, sb, new FieldPosition(FRACTION_FIELD));
  return(sb.toString());
}

/*************************************************************************/

/**
  * This method formats the specified <code>long</code> and appends it to
  * a <code>StringBuffer</code>.
  * 
  * @param number The <code>long</code> to format.
  * @param sb The <code>StringBuffer</code> to append the formatted number to.
  * @param pos The desired <code>FieldPosition</code>.
  *
  * @return The <code>StringBuffer</code> with the appended number.
  */
public abstract StringBuffer
format(long number, StringBuffer sb, FieldPosition pos);

/*************************************************************************/

/**
  * This method formats the specified <code>double</code> and appends it to
  * a <code>StringBuffer</code>.
  * 
  * @param number The <code>double</code> to format.
  * @param sb The <code>StringBuffer</code> to append the formatted number to.
  * @param pos The desired <code>FieldPosition</code>.
  *
  * @return The <code>StringBuffer</code> with the appended number.
  */
public abstract StringBuffer
format(double number, StringBuffer sb, FieldPosition pos);


public final StringBuffer format(Object number,
                                 StringBuffer toAppendTo,
                                 FieldPosition pos) {
	throw new Error("NOT IMPLEMENTED");	
    }

/*************************************************************************/

/**
  * This method parses the specified string into a <code>Number</code>.  This
  * will be a <code>Long</code> if possible, otherwise it will be a
  * <code>Double</code>.  If no number can be parsed, an exception will be
  * thrown.
  *
  * @param str The string to parse.
  *
  * @return The parsed <code>Number</code>
  *
  * @exception ParseException If no number can be parsed.
  */
public Number
parse(String str) throws ParseException
{
  ParsePosition pp = new ParsePosition(0);

  Number n = parse(str, pp);
  if (pp.getIndex() == 0)
    throw new ParseException("Unable to parse string into Number", 0);

  return(n);
}

/*************************************************************************/

/**
  * This method parses the specified string into a <code>Number</code>.  This
  * will be a <code>Long</code> if possible, otherwise it will be a
  * <code>Double</code>.    If no number can be parsed, no exception is
  * thrown.  Instead, the parse position remains at its initial index.
  *
  * @param str The string to parse.
  * @param pp The desired <code>ParsePosition</code>.
  *
  * @return The parsed <code>Number</code>
  */
public abstract Number
parse(String str, ParsePosition pp);

/*************************************************************************/

/**
  * This method parses the specified string into an <code>Object</code>.  This
  * will be a <code>Long</code> if possible, otherwise it will be a
  * <code>Double</code>.    If no number can be parsed, no exception is
  * thrown.  Instead, the parse position remains at its initial index.
  *
  * @param str The string to parse.
  * @param pp The desired <code>ParsePosition</code>.
  *
  * @return The parsed <code>Object</code>
  */
public final Object
parseObject(String str, ParsePosition pp)
{
  return(parse(str, pp));
}

/*************************************************************************/

/**
  * This method returns a hash value for this object.
  *
  * @return The hash code.
  */
public int
hashCode()
{
  return(System.identityHashCode(this));
}

/*************************************************************************/

/**
  * This method tests the specified object for equality against this object.
  * This will be <code>true</code> if the following conditions are met:
  * <p>
  * <ul>
  * <li>The specified object is not <code>null</code>.
  * <li>The specified object is an instance of <code>NumberFormat</code>.
  * </ul>
  * <p>
  * Since this method does not test much, it is highly advised that 
  * concrete subclasses override this method.
  *
  * @param obj The <code>Object</code> to test against equality with this object.
  * 
  * @return <code>true</code> if the specified object is equal to this object, <code>false</code> otherwise.
  */
public boolean
equals(Object obj)
{
  if (obj == null)
    return(false);

  if (!(obj instanceof NumberFormat))
    return(false);

  return(true);
}

} // class NumberFormat

