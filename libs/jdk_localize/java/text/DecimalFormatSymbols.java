/* DecimalFormatSymbols.java -- Format symbols used by DecimalFormat
   Copyright (C) 1999 Free Software Foundation, Inc.

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
import java.util.ResourceBundle;
import java.util.ListResourceBundle;

/**
  * This class is a container for the symbols used by 
  * <code>DecimalFormat</code> to format numbers and currency.  These are
  * normally handled automatically, but an application can override
  * values as desired using this class.
  *
  * @author Aaron M. Renn (arenn@urbanophile.com)
  */
public final class DecimalFormatSymbols implements Serializable, Cloneable
{

/*************************************************************************/

/*
 * Instance Variables
 */

/**
  * @serial A string used for the local currency
  */
private String currencySymbol;

/**
  * @serial This string represents the local currency in an international
  * context, eg, "C$" for Canadian dollars.
  */
private String intlCurrencySymbol;

/**
  * @serial The <code>char</code> used to separate decimals in a number.
  */
private char decimalSeparator;

/**
  * @serial This is the <code>char</code> used to represent a digit in
  * a format specification.
  */
private char digit;

/**
  * @serial This is the <code>char</code> used to represent the exponent
  * separator in exponential notation.
  */
private char exponential;

/**
  * @serial This separates groups of thousands in numbers.
  */
private char groupingSeparator;

/**
  * @serial This string represents infinity.
  */
private String infinity;

/**
  * @serial This string is used the represent the Java NaN value for
  * "not a number".
  */
private String NaN;

/**
  * @serial This is the character used to represent the minus sign.
  */
private char minusSign;

/**
  * @serial This character is used to separate decimals when formatting
  * currency values.
  */
private char monetarySeparator;

/**
  * @serial This is the character used to separate positive and negative
  * subpatterns in a format pattern.
  */
private char patternSeparator;

/**
  * @serial This is the percent symbols
  */
private char percent;

/**
  * @serial This character is used for the mille percent sign.
  */
private char perMill;

/**
  * @serial This is the character used to represent 0.
  */
private char zeroDigit;

/**
  * @serial This value represents the type of object being de-serialized.
  * 0 indicates a pre-Java 1.1.6 version, 1 indicates 1.1.6 or later.
  */
private int serialVersionOnStream = 1;

/*************************************************************************/

/*
 * Constructors
 */

/**
  * This method initializes a new instance of <code>DecimalFormatSymbols</code>
  * for the default locale.
  */
public
DecimalFormatSymbols()
{
  this(Locale.getDefault());
}

/*************************************************************************/

/**
  * This method initializes a new instance of <code>DecimalFormatSymbols</code>
  * for the specified locale. 
  *
  * @param locale The local to load symbols for.
  */
public
DecimalFormatSymbols(Locale locale)
{
  ResourceBundle rb = ListResourceBundle.getBundle(
                      "gnu/java/locale/LocaleInformation", locale);

  currencySymbol = rb.getString("currencySymbol");           
  intlCurrencySymbol = rb.getString("intlCurrencySymbol");
  decimalSeparator = rb.getString("decimalSeparator").charAt(0);  
  digit = rb.getString("digit").charAt(0);
  exponential = rb.getString("exponential").charAt(0);
  groupingSeparator = rb.getString("groupingSeparator").charAt(0);
  infinity = rb.getString("infinity");
  NaN = rb.getString("NaN");
  minusSign = rb.getString("minusSign").charAt(0);
  monetarySeparator = rb.getString("monetarySeparator").charAt(0);
  patternSeparator = rb.getString("patternSeparator").charAt(0);
  percent = rb.getString("percent").charAt(0);
  perMill = rb.getString("perMill").charAt(0); 
  zeroDigit = rb.getString("zeroDigit").charAt(0);
}

/*************************************************************************/

/*
 * Instance Methods
 */

/**
  * This method returns the currency symbol in local format.  For example,
  * "$" for Canadian dollars.
  *
  * @return The currency symbol in local format.
  */
public String
getCurrencySymbol()
{
  return(currencySymbol);
}

/*************************************************************************/

/**
  * This method sets the currency symbol to the specified value.
  *
  * @param currencySymbol The new currency symbol
  */
public void
setCurrencySymbol(String currencySymbol)
{
  this.currencySymbol = currencySymbol;
}

/*************************************************************************/

/**
  * This method returns the currency symbol in international format.  For
  * example, "C$" for Canadian dollars.
  *
  * @return The currency symbol in international format.
  */
public String
getInternationalCurrencySymbol()
{
  return(intlCurrencySymbol);
}

/*************************************************************************/

/**
  * This method sets the international currency symbols to the specified value.
  *
  * @param intlCurrencySymbol The new international currency symbol.
  */
public void
setInternationalCurrencySymbol(String intlCurrencySymbol)
{
  this.intlCurrencySymbol = intlCurrencySymbol;
}

/*************************************************************************/

/**
  * This method returns the character used as the decimal point.
  *
  * @return The character used as the decimal point.
  */
public char
getDecimalSeparator()
{
  return(decimalSeparator);
}

/*************************************************************************/

/**
  * This method sets the decimal point character to the specified value.
  *
  * @param decimalSeparator The new decimal point character
  */
public void
setDecimalSeparator(char decimalSeparator)
{
  this.decimalSeparator = decimalSeparator;
}

/*************************************************************************/

/**
  * This method returns the character used to represent a digit in a
  * format pattern string.
  *
  * @return The character used to represent a digit in a format pattern string.
  */
public char
getDigit()
{
  return(digit);
}

/*************************************************************************/

/**
  * This method sets the character used to represents a digit in a format
  * string to the specified value.
  *
  * @param digit The character used to represent a digit in a format pattern.
  */
public void
setDigit(char digit)
{
  this.digit = digit;
}

/*************************************************************************/

/**
  * This method sets the character used to separate groups of digits.  For
  * example, the United States uses a comma (,) to separate thousands in
  * a number.
  *
  * @return The character used to separate groups of digits.
  */
public char
getGroupingSeparator()
{
  return(groupingSeparator);
}

/*************************************************************************/

/**
  * This method sets the character used to separate groups of digits.
  *
  * @param groupingSeparator The character used to separate groups of digits.
  */
public void
setGroupingSeparator(char groupingSeparator)
{
  this.groupingSeparator = groupingSeparator;
}

/*************************************************************************/

/**
  * This method returns the character used to represent infinity.
  *
  * @return The character used to represent infinity.
  */
public String
getInfinity()
{
  return(infinity);
}

/*************************************************************************/

/**
  * This method sets the string used to represents infinity.
  *
  * @param infinity The string used to represent infinity.
  */
public void
setInfinity(String infinity)
{
  this.infinity = infinity;
}

/*************************************************************************/

/**
  * This method returns the string used to represent the NaN (not a number)
  * value.
  *
  * @return The string used to represent NaN
  */
public String
getNaN()
{
  return(NaN);
}

/*************************************************************************/

/**
  * This method sets the string used to represent the NaN (not a number) value.
  *
  * @param NaN The string used to represent NaN
  */
public void
setNaN(String NaN)
{
  this.NaN = NaN;
}

/*************************************************************************/

/**
  * This method returns the character used to represent the minus sign.
  *
  * @return The character used to represent the minus sign.
  */
public char
getMinusSign()
{
  return(minusSign);
}

/*************************************************************************/

/**
  * This method sets the character used to represent the minus sign.
  *
  * @param minusSign The character used to represent the minus sign.
  */
public void
setMinusSign(char minusSign)
{
  this.minusSign = minusSign;
}

/*************************************************************************/

/**
  * This method returns the character used to represent the decimal
  * point for currency values.
  *
  * @return The decimal point character used in currency values.
  */
public char
getMonetaryDecimalSeparator()
{
  return(monetarySeparator);
}

/*************************************************************************/

/**
  * This method sets the character used for the decimal point in currency
  * values.
  *
  * @param monetarySeparator The decimal point character used in currency values.
  */
public void
setMonetaryDecimalSeparator(char monetarySeparator)
{
  this.monetarySeparator = monetarySeparator;
}

/*************************************************************************/

/**
  * This method returns the character used to separate positive and negative
  * subpatterns in a format pattern.
  *
  * @return The character used to separate positive and negative subpatterns
  * in a format pattern.
  */
public char
getPatternSeparator()
{
  return(patternSeparator);
}

/*************************************************************************/

/**
  * This method sets the character used to separate positive and negative
  * subpatterns in a format pattern.
  *
  * @param patternSeparator The character used to separate positive and
  * negative subpatterns in a format pattern.
  */
public void
setPatternSeparator(char patternSeparator)
{
  this.patternSeparator = patternSeparator;
}

/*************************************************************************/

/**
  * This method returns the character used as the percent sign.
  *
  * @return The character used as the percent sign.
  */
public char
getPercent()
{
  return(percent);
}

/*************************************************************************/

/**
  * This method sets the character used as the percent sign.
  *
  * @param percent  The character used as the percent sign.
  */
public void
setPercent(char percent)
{
  this.percent = percent;
}

/*************************************************************************/

/**
  * This method returns the character used as the per mille character.
  *
  * @return The per mille character.
  */
public char
getPerMill()
{
  return(perMill);
}

/*************************************************************************/

/**
  * This method sets the character used as the per mille character.
  *
  * @param perMill The per mille character.
  */
public void
setPerMill(char perMill)
{
  this.perMill = perMill;
}

/*************************************************************************/

/**
  * This method returns the character used to represent the digit zero.
  *
  * @return The character used to represent the digit zero.
  */
public char
getZeroDigit()
{
  return(zeroDigit);
}

/*************************************************************************/

/**
  * This method sets the charcter used to represen the digit zero.
  *
  * @param zeroDigit The character used to represent the digit zero.
  */
public void
setZeroDigit(char zeroDigit)
{
  this.zeroDigit = zeroDigit;
} 

/*************************************************************************/

/**
  * This method returns a copy of this object.
  *
  * @return A copy of this object.
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

/*************************************************************************/

/**
  * This method returns a hash value for this object.
  *
  * @return A hash value for this object.
  */
public int
hashCode()
{
  return(System.identityHashCode(this));
}

/*************************************************************************/

/**
  * This method this this object for equality against the specified object.
  * This will be true if and only if the following criteria are met with
  * regard to the specified object:
  * <p>
  * <ul>
  * <li>It is not <code>null</code>.
  * <li>It is an instance of <code>DecimalFormatSymbols</code>
  * <li>All of its symbols are identical to the symbols in this object.
  * </ul>
  *
  * @return <code>true</code> if the specified object is equal to this
  * object, <code>false</code> otherwise.
  */
public boolean
equals(Object obj)
{
  if (obj == null)
    return(false);

  if (!(obj instanceof DecimalFormatSymbols))
    return(false);

  DecimalFormatSymbols dfs = (DecimalFormatSymbols)obj;

  if (dfs.getZeroDigit() != getZeroDigit())
    return(false);

  if (dfs.getGroupingSeparator() != getGroupingSeparator())
    return(false);

  if (dfs.getDecimalSeparator() != getDecimalSeparator())
    return(false);

  if (dfs.getPerMill() != getPerMill())
    return(false);

  if (dfs.getPercent() != getPercent())
    return(false);

  if (dfs.getDigit() != getDigit())
    return(false);

  if (dfs.getPatternSeparator() != getPatternSeparator())
    return(false);

  if (!dfs.getInfinity().equals(getInfinity()))
    return(false);

  if (!dfs.getNaN().equals(getNaN()))
    return(false);

  if (dfs.getMinusSign() != getMinusSign())
    return(false);

  if (!dfs.getCurrencySymbol().equals(getCurrencySymbol()))
    return(false);

  if (!dfs.getInternationalCurrencySymbol().equals(
      getInternationalCurrencySymbol()))
    return(false);

  if (getMonetaryDecimalSeparator() != getMonetaryDecimalSeparator())
    return(false);

  return(true);
}

} // class DecimalFormatSymbols

