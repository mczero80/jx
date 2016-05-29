/* ChoiceFormat.java -- Format over a range of numbers
   Copyright (C) 1998, 1999 Free Software Foundation, Inc.

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
import java.util.StringTokenizer;

/**
  * This class allows a format to be specified based on a range of numbers.
  * To use this class, first specify two lists of formats and range terminators.
  * These lists must be arrays of equal length.  The format of index 
  * <code>i</code> will be selected for value <code>X</code> if 
  * <code>terminator[i] <= X < limit[i + 1]</code>.  If the value X is not
  * included in any range, then either the first or last format will be 
  * used depending on whether the value X falls outside the range.
  * <p>
  * This sounds complicated, but that is because I did a poor job of
  * explaining it.  Consider the following example:
  * <p>
  * <pre>
  * terminators = { 1, ChoiceFormat.nextDouble(1) }
  * formats = { "file", "files" }
  * </pre>
  * <p>
  * In this case if the actual number tested is one or less, then the word
  * "file" is used as the format value.  If the number tested is greater than
  * one, then "files" is used.  This allows plurals to be handled
  * gracefully.  Note the use of the method <code>nextDouble</code>.  This
  * method selects the next highest double number than its argument.  This
  * effectively makes any double greater than 1.0 cause the "files" string
  * to be selected.  (Note that all terminator values are specified as
  * doubles.
  * <p>
  * Note that in order for this class to work properly, the range terminator
  * array must be sorted in ascending order and the format string array
  * must be the same length as the terminator array.
  *
  * @author Aaron M. Renn (arenn@urbanophile.com)
  */
public class ChoiceFormat extends NumberFormat implements Serializable,
                                                          Cloneable
{

/*************************************************************************/

/*
 * Instance Variables
 */

/**
  * This is the list of format strings.  Note that this variable is
  * specified by the serialization spec of this class.
  */
private String[] choiceFormats;

/**
  * This is the list of range terminator values.  Note that this variable is
  * specified by the serialization spec of this class.
  */
private double[] choiceLimits;

/*************************************************************************/

/*
 * Class Methods
 */

/**
  * This method returns a double that is either the next highest double
  * or next lowest double compared to the specified double depending on the
  * value of the passed boolean parameter.  If the boolean parameter is
  * <code>true</code>, then the lowest possible double greater than the 
  * specified double will be returned.  Otherwise the highest possible
  * double less than the specified double will be returned.
  *
  * @param d The specified double
  * @param positive <code>true</code> to return the next highest double, <code>false</code> otherwise.
  *
  * @return The next highest or lowest double value.
  */
public static double
nextDouble(double d, boolean positive)
{
  if (d == Double.NaN)
    return(d);

  throw new RuntimeException("Not Implemented");
}

/*************************************************************************/

/**
  * This method returns the lowest possible double greater than the 
  * specified double.  If the specified double value is equal to
  * <code>Double.NaN</code> then that is the value returned.
  *
  * @param d The specified double
  *
  * @return The lowest double value greater than the specified double.
  */
public static final double
nextDouble(double d)
{
  return(nextDouble(d, true));
}

/*************************************************************************/

/**
  * This method returns the highest possible double less than the 
  * specified double.  If the specified double value is equal to
  * <code>Double.NaN</code> then that is the value returned.
  *
  * @param d The specified double
  *
  * @return The highest double value less than the specified double.
  */
public static final double
previousDouble(double d)
{
  return(nextDouble(d, false));
}

/*************************************************************************/

/*
 * Constructors
 */

/**
  * This method initializes a new instance of <code>ChoiceFormat</code> that
  * will use the specified range terminators and format strings.
  *
  * @param choiceLimits The array of range terminators
  * @param choiceFormats The array of format strings
  */
public
ChoiceFormat(double[] choiceLimits, String[] choiceFormats)
{
  this.choiceLimits = choiceLimits;
  this.choiceFormats = choiceFormats;
}

/*************************************************************************/

/**
  * This method initializes a new instance of <code>ChoiceFormat</code> that
  * generates its range terminator and format string arrays from the
  * specified pattern.  This pattern is of the form 
  * "term#string|term#string...".  For example "1#Sunday|2#Monday|#Tuesday".
  * This is the same pattern type used by the <code>applyPattern</code>
  * method.
  *
  * @param pattern The pattern of terminators and format strings.
  *
  * @exception IllegalArgumentException If the pattern is not valid
  */
public
ChoiceFormat(String pattern)
{
  applyPattern(pattern);
}

/*************************************************************************/

/*
 * Instance Methods
 */

/**
  * This method returns the list of range terminators in use.
  *
  * @return The list of range terminators.
  */
public double[]
getLimits()
{
  return(choiceLimits);
}

/*************************************************************************/

/**
  * This method returns the list of format strings in use.
  *
  * @return The list of format objects.
  */
public Object[]
getFormats() 
{
  return(choiceFormats);
}

/*************************************************************************/

/**
  * This method sets new range terminators and format strings for this
  * object.
  *
  * @param choiceLimits The new range terminators
  * @param choiceFormats The new choice formats
  */
public void
setChoices(double[] choiceLimits, String[] choiceFormats)
{
  this.choiceLimits = choiceLimits;
  this.choiceFormats = choiceFormats;
}

/*************************************************************************/

/**
  * This method sets new range terminators and format strings for this
  * object based on the specified pattern. This pattern is of the form 
  * "term#string|term#string...".  For example "1#Sunday|2#Monday|#Tuesday".
  *
  * @param pattern The pattern of terminators and format strings.
  *
  * @exception IllegalArgumentException If the pattern is not valid
  */
public void
applyPattern(String pattern)
{
  StringTokenizer st = new StringTokenizer(pattern, "|");
  // Hmm. If we bomb, this object is in an inconsistent state. ???
  choiceLimits = new double[st.countTokens()];
  choiceFormats = new String[st.countTokens()];

  int i = 0;
  while (st.hasMoreTokens())
    {
      StringTokenizer st2 = new StringTokenizer(st.nextToken(), "#");
      if (st2.countTokens() != 2)
        throw new IllegalArgumentException("Bad pattern: " + pattern);

      try
        {
          choiceLimits[i] = Double.valueOf(st2.nextToken()).doubleValue();
        }
      catch(NumberFormatException e)
        {
          throw new IllegalArgumentException("Bad pattern: " + pattern);
        }

      choiceFormats[i] = st2.nextToken();
      ++i;
    }
}

/*************************************************************************/

/**
  * This method returns the range terminator list and format string list
  * as a <code>String</code> suitable for using with the 
  * <code>applyPattern</code> method.
  *
  * @return A pattern string for this object
  */
public String
toPattern()
{
  StringBuffer sb = new StringBuffer("");

  for (int i = 0; i < choiceLimits.length; i++)
    {
       sb.append(choiceLimits[i] + "#" + choiceFormats[i]);
       if (i != (choiceLimits.length - 1))
         sb.append("|");      
    }

  return(sb.toString());
}

/*************************************************************************/

/**
  * This method appends the appropriate format string to the specified
  * <code>StringBuffer</code> based on the supplied <code>long</code>
  * argument.
  *
  * @param number The number used for determine (based on the range terminators) which format string to append.
  * @param sb The <code>StringBuffer</code> to append the format string to.
  * @param status Unused.
  *
  * @return The <code>StringBuffer</code> with the format string appended.
  */
public StringBuffer
format(long number, StringBuffer sb, FieldPosition status)
{
  return(format((double)number, sb, status));
}

/*************************************************************************/

/**
  * This method appends the appropriate format string to the specified
  * <code>StringBuffer</code> based on the supplied <code>double</code>
  * argument.
  *
  * @param number The number used for determine (based on the range terminators) which format string to append.
  * @param sb The <code>StringBuffer</code> to append the format string to.
  * @param status Unused.
  *
  * @return The <code>StringBuffer</code> with the format string appended.
  */
public StringBuffer
format(double number, StringBuffer sb, FieldPosition status)
{
  int i;
  for (i = 0; i < choiceLimits.length; i++)
    {
      if (number < choiceLimits[i])
        {
           if (i == 0)
             {
               sb.append(choiceFormats[i]);
               break;
             }

           sb.append(choiceFormats[i-1]);
           break;
        }
    }
  if (i == choiceLimits.length)
    sb.append(choiceFormats[choiceFormats.length - 1]);

  return(sb); 
}

/**
  * I'm not sure what this method is really supposed to do, as it is
  * not documented.
  */
public Number
parse(String text, ParsePosition status)
{
  String str = text.substring(status.getIndex());

  for (int i = 0; i < choiceFormats.length; i++)
    {
      if (str.startsWith(choiceFormats[i]))
        {
          status.setIndex(status.getIndex() + choiceFormats[i].length());

          return(new Double(choiceLimits[i]));
        }
    }

  return(new Double(Double.NaN));
}

/*************************************************************************/

/**
  * This method tests this object for equality with the specified 
  * object.  This will be true if and only if:
  * <ul>
  * <li>The specified object is not <code>null</code>.
  * <li>The specified object is an instance of <code>ChoiceFormat</code>.
  * <li>The termination ranges and format strings are identical to this object's.
  * </ul>
  *
  * @param obj The object to test for equality against.
  *
  * @return <code>true</code> if the specified object is equal to this one, <code>false</code> otherwise.
  */
public boolean
equals(Object obj)
{
  if (obj == null)
    return(false);

  if (!(obj instanceof ChoiceFormat))
    return(false);

  ChoiceFormat cf = (ChoiceFormat)obj;

  if (cf.choiceLimits.length != choiceLimits.length)
    return(false);

  for (int i = 0; i < choiceLimits.length; i++)
    {
      if (cf.choiceLimits[i] != choiceLimits[i])
        return(false);
      if (cf.choiceFormats[i] != choiceFormats[i])
        return(false);
    }

  return(true);
}

/*************************************************************************/

/**
  * This method returns a hash value for this object
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
  * This method returns a clone of this object.
  *
  * @return A clone of this object
  */

// No need to override
/*
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
*/

} // class ChoiceFormat

