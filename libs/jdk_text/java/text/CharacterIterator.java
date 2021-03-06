/* CharacterIterator.java -- Iterate over a character range
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

/**
  * This interface defines a mechanism for iterating over a range of
  * characters.  For a given range of text, a beginning and ending index,
  * as well as a current index are defined.  These values can be queried
  * by the methods in this interface.  Additionally, various methods allow
  * the index to be set. 
  *
  * @version 0.0
  *
  * @author Aaron M. Renn (arenn@urbanophile.com)
  */
public interface CharacterIterator extends Cloneable
{

/*************************************************************************/

/*
 * Static Variables
 */

/**
  * This is a special constant value that is returned when the beginning or
  * end of the character range has been reached.
  */
public static final char DONE = '\uFFFF';

/*************************************************************************/

/*
 * Instance Methods
 */

/**
  * This method returns the character at the current index position
  *
  * @return The character at the current index position.
  */
public abstract char
current();

/*************************************************************************/

/**
  * This method increments the current index and then returns the character
  * at the new index value.  If the index is already at <code>getEndIndex() - 1</code>,
  * it will not be incremented.
  *
  * @return The character at the position of the incremented index value, or <code>DONE</code> if the index has reached getEndIndex() - 1
  */
public abstract char
next();

/*************************************************************************/

/**
  * This method decrements the current index and then returns the character
  * at the new index value.  If the index value is already at the beginning
  * index, it will not be decremented.
  *
  * @return The character at the position of the decremented index value, or <code>DONE</code> if index was already equal to the beginning index value.
  */
public abstract char
previous();

/*************************************************************************/

/**
  * This method sets the index value to the beginning of the range and returns
  * the character there.
  *
  * @return The character at the beginning of the range, or <code>DONE</code> if the range is empty.
  */
public abstract char
first();

/*************************************************************************/

/**
  * This method sets the index value to <code>getEndIndex() - 1</code> and
  * returns the character there.  If the range is empty, then the index value
  * will be set equal to the beginning index.
  *
  * @return The character at the end of the range, or <code>DONE</code> if the range is empty.
  */
public abstract char
last();  

/*************************************************************************/

/**
  * This method returns the current value of the index.
  *
  * @return The current index value
  */
public abstract int
getIndex();

/*************************************************************************/

/**
  * This method sets the value of the index to the specified value, then
  * returns the character at that position.
  *
  * @param index The new index value.
  *
  * @return The character at the new index value or <code>DONE</code> if the index value is equal to <code>getEndIndex</code>.
  */
public abstract char
setIndex(int index) throws IllegalArgumentException;

/*************************************************************************/

/**
  * This method returns the character position of the first character in the
  * range.
  *
  * @return The index of the first character in the range.
  */
public abstract int
getBeginIndex();

/*************************************************************************/

/**
  * This method returns the character position of the end of the text range.
  * This will actually be the index of the first character following the
  * end of the range.  In the event the text range is empty, this will be
  * equal to the first character in the range.
  *
  * @return The index of the end of the range.
  */
public abstract int
getEndIndex();

/*************************************************************************/

/**
  * This method creates a copy of this <code>CharacterIterator</code>.
  *
  * @return A copy of this <code>CharacterIterator</code>.
  */
public abstract Object
clone();

} // interface CharacterIterator

