/* ItemEvent.java -- Event for item state changes.
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

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version. */


package java.awt.event;

import java.awt.ItemSelectable;

/**
  * This event is generated when an item changes state.
  *
  * @author Aaron M. Renn (arenn@urbanophile.com)
  */
public class ItemEvent extends java.awt.AWTEvent implements java.io.Serializable
{

/*
 * Static Variables
 */

/**
  * This is the first id in the event id range used by this class.
  */
public static final int ITEM_FIRST = 701;

/**
  * This is the last id in the event id range used by this class.
  */
public static final int ITEM_LAST = 701;

/**
  * This event id indicates a state change occurred.
  */
public static final int ITEM_STATE_CHANGED = 701;

/**
  * This type indicates that the item was selected.
  */
public static final int SELECTED = 1;

/**
  * This type indicates that the item was deselected.
  */
public static final int DESELECTED = 2;

/*************************************************************************/

/*
 * Instance Variables
 */

/**
  * @serial The item affected by this event
  */
private Object item;

/**
  * @serial  The state change direction
  */
private int stateChange;

/*************************************************************************/

/*
 * Constructors
 */

/**
  * Initializes a new instance of <code>ItemEvent</code> with the specified
  * source, id, and state change constant.
  *
  * @param source The source of the event.
  * @param id The event id.
  * @param item The item affected by the state change.
  * @param stateChange The state change, either <code>SELECTED</code> or
  * <code>DESELECTED</code>.
  */
public 
ItemEvent(ItemSelectable source, int id, Object item, int stateChange)
{
  super(source, id);
  this.item = item;
  this.stateChange = stateChange;
}
  
/*************************************************************************/

/*
 * Instance Methods
 */

/**
  * This method returns the event source as an <code>ItemSelectable</code>.
  * 
  * @return The event source as an <code>ItemSelected</code>.
  */
public ItemSelectable
getItemSelectable()
{
  return((ItemSelectable)getSource());
}

/*************************************************************************/

/**
  * Returns the item affected by this state change.
  *
  * @return The item affected by this state change.
  */
public Object
getItem()
{
  return(item);
}

/*************************************************************************/

/**
  * Returns the type of state change, ether <code>SELECTED</code> or
  * <code>DESELECTED</code>.
  *
  * @return The type of state change.
  */
public int
getStateChange()
{
  return(stateChange);
}

/*************************************************************************/

/**
  * Returns a string identifying this event.
  *
  * @param A string identifying this event.
  */
public String
paramString()
{
  return(getClass().getName() + " source=" + getSource() + " id=" + getID() +
         " item=" + getItem() + " stateChange=" + getStateChange());
}

} // class ItemEvent

