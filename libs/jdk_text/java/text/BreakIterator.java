/* BreakIterator.java -- Breaks text into elements
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
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.ListResourceBundle;
import java.util.MissingResourceException;

/**
  * This class iterates over text elements such as words, lines, sentences,
  * and characters.  It can only iterate over one of these text elements at
  * a time.  An instance of this class configured for the desired iteration
  * type is created by calling one of the static factory methods, not
  * by directly calling a constructor.
  *
  * @author Aaron M. Renn (arenn@urbanophile.com)
  */
public abstract class BreakIterator implements Cloneable, Serializable
{

/*************************************************************************/

/*
 * Static Variables
 */

/**
  * This value is returned by the <code>next()</code> and <code>previous</code>
  * in order to indicate that the end of the text has been reached.
  */
public static final int DONE = 0xFFFFFFFF;

/*************************************************************************/

/*
 * Static Methods
 */

/**
  * This method returns an instance of <code>BreakIterator</code> that will
  * iterate over characters as defined in the default locale.
  *
  * @return A <code>BreakIterator</code> instance for the default locale.
  */
public static BreakIterator
getCharacterInstance()
{
  return(getCharacterInstance(Locale.getDefault()));
}

/*************************************************************************/

/**
  * This method returns an instance of <code>BreakIterator</code> that will
  * iterate over characters as defined in the specified locale.  If the
  * desired locale is not available, the default locale is used.
  *
  * @param locale The desired locale.
  *
  * @return A <code>BreakIterator</code> instance for the default locale.
  */
public static BreakIterator
getCharacterInstance(Locale locale)
{
  return(new DefaultBreakIterator());
}

/*************************************************************************/

/**
  * This method returns an instance of <code>BreakIterator</code> that will
  * iterate over words as defined in the default locale.
  *
  * @return A <code>BreakIterator</code> instance for the default locale.
  */
public static BreakIterator
getWordInstance()
{
  return(getWordInstance(Locale.getDefault()));
}

/*************************************************************************/

/**
  * This method returns an instance of <code>BreakIterator</code> that will
  * iterate over words as defined in the specified locale.  If the
  * desired locale is not available, the default locale is used.
  *
  * @param locale The desired locale.
  *
  * @return A <code>BreakIterator</code> instance for the default locale.
  */
public static BreakIterator
getWordInstance(Locale locale)
{
  String[] word_breaks;
  try
    {
      ResourceBundle rb = ListResourceBundle.getBundle(
                            "gnu/java/locale/LocaleInformation", locale);

      Object obj = rb.getObject("word_breaks");
      if ((obj == null) || !(obj instanceof String[]))
         throw new RuntimeException("Cannot load word break information");

      word_breaks = (String[])obj;
    }
  catch(MissingResourceException e)
    {
      throw new RuntimeException("Cannot load word break information: " +
                                  e.getMessage());
    }

  return(new DefaultBreakIterator(word_breaks, true));
}

/*************************************************************************/

/**
  * This method returns an instance of <code>BreakIterator</code> that will
  * iterate over sentences as defined in the default locale.
  *
  * @return A <code>BreakIterator</code> instance for the default locale.
  */
public static BreakIterator
getSentenceInstance()
{
  return(getSentenceInstance(Locale.getDefault()));
}

/*************************************************************************/

/**
  * This method returns an instance of <code>BreakIterator</code> that will
  * iterate over sentences as defined in the specified locale.  If the
  * desired locale is not available, the default locale is used.
  *
  * @param locale The desired locale.
  *
  * @return A <code>BreakIterator</code> instance for the default locale.
  */
public static BreakIterator
getSentenceInstance(Locale locale)
{
  String[] sentence_breaks;
  try
    {
      ResourceBundle rb = ListResourceBundle.getBundle(
                            "gnu/java/locale/LocaleInformation", locale);

      Object obj = rb.getObject("sentence_breaks");
      if ((obj == null) || !(obj instanceof String[]))
         throw new RuntimeException("Cannot load sentence break information");

      sentence_breaks = (String[])obj;
    }
  catch(MissingResourceException e)
    {
      throw new RuntimeException("Cannot load sentence break information: " +
                                  e.getMessage());
    }

  return(new DefaultBreakIterator(sentence_breaks, false));
}

/*************************************************************************/

/**
  * This method returns an instance of <code>BreakIterator</code> that will
  * iterate over line breaks as defined in the default locale.
  *
  * @return A <code>BreakIterator</code> instance for the default locale.
  */
public static BreakIterator
getLineInstance()
{
  return(getLineInstance(Locale.getDefault()));
}

/*************************************************************************/

/**
  * This method returns an instance of <code>BreakIterator</code> that will
  * iterate over line breaks as defined in the specified locale.  If the
  * desired locale is not available, the default locale is used.
  *
  * @param locale The desired locale.
  *
  * @return A <code>BreakIterator</code> instance for the default locale.
  */
public static BreakIterator
getLineInstance(Locale locale)
{
  String[] line_breaks;
  try
    {
      ResourceBundle rb = ListResourceBundle.getBundle(
                            "gnu/java/locale/LocaleInformation", locale);

      Object obj = rb.getObject("line_breaks");
      if ((obj == null) || !(obj instanceof String[]))
         throw new RuntimeException("Cannot load sentence break information");

      line_breaks = (String[])obj;
    }
  catch(MissingResourceException e)
    {
      throw new RuntimeException("Cannot load line break information: " +
                                  e.getMessage());
    }

  return(new DefaultBreakIterator(line_breaks, false));
}

/*************************************************************************/

/**
  * This method returns a list of locales for which instances of
  * <code>BreakIterator</code> are available.
  *
  * @return A list of available locales
  */
public static Locale[]
getAvailableLocales()
{
  //******Do this for now
  Locale[] l = new Locale[1];
  l[0] = Locale.getDefault();

  return(l);
}

/*************************************************************************/

/*
 * Constructors
 */

/**
  * This method initializes a new instance of <code>BreakIterator</code>.
  * This protected constructor is available to subclasses as a default
  * no-arg superclass constructor.
  */
protected 
BreakIterator()
{
  ;
}

/*************************************************************************/

/*
 * Instance Methods
 */

/**
  * This method returns the index of the current text element boundary.
  *
  * @return The current text boundary.
  */
public abstract int
current();

/*************************************************************************/

/**
  * This method returns the first text element boundary in the text being
  * iterated over.
  *
  * @return The first text boundary.
  */
public abstract int
first();

/*************************************************************************/

/**
  * This method returns the last text element boundary in the text being
  * iterated over.
  *
  * @return The last text boundary.
  */
public abstract int
last();

/*************************************************************************/

/**
  * This method returns the text element boundary following the current
  * text position.
  *
  * @return The next text boundary.
  */
public abstract int
next(); 

/*************************************************************************/

/**
  * This method returns the n'th text element boundary following the current
  * text position.
  *
  * @param n The number of text element boundaries to skip.
  *
  * @return The next text boundary.
  */
public abstract int
next(int n);

/*************************************************************************/

/**
  * This method returns the text element boundary preceding the current
  * text position.
  *
  * @return The previous text boundary.
  */
public abstract int
previous(); 

/*************************************************************************/

/**
  * This method returns the n'th text element boundary preceding the current
  * text position.
  *
  * @param n The number of text element boundaries to skip.
  *
  * @return The previous text boundary.
  */
public abstract int
previous(int n);

/*************************************************************************/

/**
  * This methdod returns the offset of the text element boundary following
  * the specified offset.
  *
  * @param offset The text index from which to find the next text boundary.
  *
  * @param The next text boundary following the specified index.
  */
public abstract int
following(int offset);

/*************************************************************************/

/**
  * This methdod returns the offset of the text element boundary preceding
  * the specified offset.
  *
  * @param offset The text index from which to find the preceding text boundary.
  *
  * @param The next text boundary preceding the specified index.
  */
public abstract int
preceding(int offset);

/*************************************************************************/

/**
  * This method returns the text this object is iterating over as a
  * <code>CharacterIterator</code>.
  *
  * @param The text being iterated over.
  */
public abstract CharacterIterator
getText();

/*************************************************************************/

/**
  * This method sets the text string to iterate over.
  *
  * @param str The <code>String</code> to iterate over.
  */
public void
setText(String str)
{
  setText(new StringCharacterIterator(str));
} 

/*************************************************************************/

/**
  * This method sets the text to iterate over from the specified
  * <code>CharacterIterator</code>.
  * 
  * @param ci The desired <code>CharacterIterator</code>.
  */
public abstract void
setText(CharacterIterator ci);

/*************************************************************************/

/**
  * This method tests whether or not the specified position is a text
  * element boundary.
  *
  * @param offset The text position to test.
  *
  * @return <code>true</code> if the position is a boundary, <code>false</code> otherwise.
  */
public abstract boolean
isBoundary(int offset);

} // class BreakIterator

