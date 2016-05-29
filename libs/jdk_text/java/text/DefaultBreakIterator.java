/* DefaultBreakIterator.java -- Default BreakIterator implementation
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

/**
  * This class provides a concrete subclass implementation of
  * <code>BreakIterator</code> that handles all four types of iteration
  * methods.  Warning!!!  This class really only works for simple
  * English like locales.  It also doesn't handle things like merging
  * consecutive spaces into a single break character.
  *
  * @author Aaron M. Renn (arenn@urbanophile.com)
  */
class DefaultBreakIterator extends BreakIterator
{

/*************************************************************************/

/*
 * Instance Variables
 */

/**
  * This is the <code>CharacterIterator</code> that holds the text we
  * are iterating over.
  */
private CharacterIterator ci;

/**
  * This is the list of break sequences we are using.
  */
private String[] breaks;

/**
  * This variable indicates whether or not to break both before and
  * after the break sequence.
  */
private boolean before_and_after;

/**
  * Index of the break we broke before when doing a next().
  */
private int broke_before_next;

/**
  * Index of the break we broke before when doing a previous().
  */
private int broke_before_previous;

/*************************************************************************/

/*
 * Constructors
 */

/**
  * This constructor defaults this instance to iterating over characters.
  */
public
DefaultBreakIterator()
{
  this.broke_before_next = -1;
  this.broke_before_previous = -1;
}

/*************************************************************************/

/**
  * This method initializes to use the specified break sequences and
  * break semantics.
  */
public
DefaultBreakIterator(String[] breaks, boolean before_and_after)
{
  this();

  this.breaks = breaks;
  this.before_and_after = before_and_after;
}

/*************************************************************************/

/*
 * Instance Methods
 */

public int
current()
{
  return(ci.getIndex());
}

public int
first()
{
  broke_before_next = -1;
  broke_before_previous = -1;

  if (ci.first() == CharacterIterator.DONE)
    return(DONE);
  return(ci.getIndex());
}

public int
last()
{
  broke_before_next = -1;
  broke_before_previous = -1;

  if (ci.last() == CharacterIterator.DONE)
    return(DONE);
  return(ci.getIndex());
}

public int
next()
{
  broke_before_previous = -1;

  // Handle character case
  if (breaks == null)
    {
      if (ci.next() == CharacterIterator.DONE)
        return(DONE);
      return(ci.getIndex());
    }

  // Handle case where we broke before a break last time and now we
  // want to break after it.
  if (broke_before_next != -1)
    {
      ci.setIndex(ci.getIndex() + breaks[broke_before_next].length());
      broke_before_next = -1;
      return(ci.getIndex());
    }

  // Handle all other cases
  StringBuffer sb = new StringBuffer("");
  int start_index = ci.getIndex();
  for (;;)
    {
      char c = ci.next();
      if (c == CharacterIterator.DONE)
        return(DONE);

      sb.append(c);

      int i;
      for (i = 0; i < breaks.length; i++)
        {
          if (breaks[i].equals(sb.toString()))
            {
              // Check to see if we already broke at beginning of break seq
              if (before_and_after)
                {
                  broke_before_next = i;
                  ci.setIndex(ci.getIndex() - breaks[i].length() + 1);
                  return(ci.getIndex());
                }
              else
                {
                  ci.next();
                  return(ci.getIndex());
                }
            }

          if (breaks[i].startsWith(sb.toString()))
            break;
        }

      if (i == breaks.length)
        sb = new StringBuffer("");
    }
}

public int
next(int index)
{
  for (int i = 0; i < index; i++)
    if (next() == DONE)
      return(DONE);

  return(current());
}

public int
previous()
{
  broke_before_next = -1;

  // Handle character case
  if (breaks == null)
    {
      if (ci.previous() == CharacterIterator.DONE)
        return(DONE);
      return(ci.getIndex());
    }

  if (broke_before_previous != -1)
    {
      ci.setIndex(ci.getIndex() - breaks[broke_before_previous].length());
      broke_before_previous = -1;
      return(ci.getIndex());
    }

  // Handle all other cases
  StringBuffer sb = new StringBuffer("");
  int start_index = ci.getIndex();
  for (;;)
    {
      char c = ci.previous();
      if (c == CharacterIterator.DONE)
        return(DONE);

      sb.insert(0, c);

      int i;
      for (i = 0; i < breaks.length; i++)
        {
          if (breaks[i].equals(sb.toString()))
            {
              // Check to see if we already broke at beginning of break seq
              if (before_and_after)
                {
                  broke_before_previous = i;
                  ci.setIndex(ci.getIndex() + breaks[i].length());
                  return(ci.getIndex());
                }
              else
                {
                  return(ci.getIndex()+breaks[i].length());
                }
            }

          if (breaks[i].startsWith(sb.toString()))
            break;
        }

      if (i == breaks.length)
        sb = new StringBuffer("");
    }
}

public int
previous(int index)
{
  for (int i = 0; i < index; i++)
    if (previous() == DONE)
      return(DONE);

  return(current());
}

public int
following(int index)
{
  broke_before_next = -1;
  broke_before_previous = -1;

  ci.setIndex(index);
  return(next());
}

public int
preceding(int index)
{
  broke_before_next = -1;
  broke_before_previous = -1;

  ci.setIndex(index);
  return(previous());
}

public CharacterIterator
getText()
{
  return(ci);
}

public void
setText(CharacterIterator ci)
{
  this.ci = ci;

  broke_before_next = -1;
  broke_before_previous = -1;
}

public boolean
isBoundary(int index)
{
  // Handle character case
  if (breaks == null)
    {
      return(true);
    }

  // Handle other cases
  int save_index = ci.getIndex();
  ci.setIndex(index);
  StringBuffer sb = new StringBuffer("");
  for (;;)
    {
      char c = ci.next();
      if (c == CharacterIterator.DONE)
        return(false);

      sb.append(c);

      int i;
      for (i = 0; i < breaks.length; i++)
        {
          if (breaks[i].equals(sb.toString()))
            {
              ci.setIndex(save_index);
              return(true);
            }

          if (breaks[i].startsWith(sb.toString()))
            break;
        }

      if (i == breaks.length)
        {
          ci.setIndex(save_index);
          return(false);
        }
    }
}

} // class DefaultBreakIterator

