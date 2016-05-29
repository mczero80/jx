/* MouseEvent.java -- A mouse event
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

import java.awt.Component;
import java.awt.Point;

/**
  * This event is generated for a mouse event.
  *
  * @author Aaron M. Renn (arenn@urbanophile.com)
  */
public class MouseEvent extends InputEvent implements java.io.Serializable
{

/*
 * Static Variables
 */

/**
  * This is the first id in the range of event ids used by this class.
  */
public static final int MOUSE_FIRST = 500;

/**
  * This is the last id in the range of event ids used by this class.
  */
public static final int MOUSE_LAST = 506;

/**
  * This event id indicates that the mouse was clicked.
  */
public static final int MOUSE_CLICKED = 500;

/**
  * This event id indicates that the mouse was pressed.
  */
public static final int MOUSE_PRESSED = 501;

/**
  * This event id indicates that the mouse was released.
  */
public static final int MOUSE_RELEASED = 502;

/**
  * This event id indicates that the mouse was moved.
  */
public static final int MOUSE_MOVED = 503;

/**
  * This event id indicates that the mouse entered a component.
  */
public static final int MOUSE_ENTERED = 504;

/**
  * This event id indicates that the mouse exited a component.
  */
public static final int MOUSE_EXITED = 505;

/**
  * This event id indicates that the mouse was dragged over a component.
  */
public static final int MOUSE_DRAGGED = 506;

/*************************************************************************/

/*
 * Instance Variables
 */

/**
  * @serial  The X coordinate of the mouse pos
  */
private int x;

/**
  * @serial The Y coordinate of the mouse pos
  */
private int y;

/**
  * @serial The number of clicks for this event
  */
private int clickCount;

/**
  * @serial Whether or not this event triggers a popup menu
  */
private boolean popupTrigger;

/*************************************************************************/

/*
 * Constructors
 */

/**
  * Initializes a new instance of <code>MouseEvent</code> with the
  * specified information.
  *
  * @param source The source of the event.
  * @param id The event id.
  * @param when The timestamp of when the event occurred.
  * @param modifiers Any modifier bits for this event.
  * @param x The X coordinate of the mouse point.
  * @param y The Y coordinate of the mouse point.
  * @param clickCount The number of mouse clicks for this event.
  * @param popupTrigger <code>true</code> if this event triggers a popup
  * menu, <code>false</code> otherwise.
  */
    public 
    MouseEvent(Component source, int id, /*long when,*/ int modifiers, int x, int y,
           int clickCount, boolean popupTrigger)
{
    super(source, id, /*when,*/ modifiers);
  this.x = x;
  this.y = y;
  this.clickCount = clickCount;
  this.popupTrigger = popupTrigger;
}

/*************************************************************************/

/*
 * Instance Methods
 */

/**
  * This method returns the number of mouse clicks associated with this
  * event.
  *
  * @return The number of mouse clicks for this event. 
  */
public int
getClickCount()
{
  return(clickCount);
}

/*************************************************************************/

/**
  * This method returns the X coordinate of the mouse position. This is
  * relative to the source component.
  */
public int
getX()
{
  return(x);
}

/*************************************************************************/

/**
  * This method returns the Y coordinate of the mouse position. This is
  * relative to the source component.
  */
public int
getY()
{
  return(y);
}

/*************************************************************************/

/**
  * This method returns a <code>Point</code> for the x,y position of
  * the mouse pointer.  This is relative to the source component.
  *
  * @return A <code>Point</code> for the event position.
  */
public Point
getPoint()
{
  return (new Point(x, y));
}

/*************************************************************************/

/**
  * This method tests whether or not the event is a popup menu trigger.
  *
  * @return <code>true</code> if the event is a trigger, <code>false</code>
  * otherwise.
  */
public boolean
isPopupTrigger()
{
  return(popupTrigger);
}

/*************************************************************************/

/**
  * Adds the specified x and y coordinate values to the existing
  * x and y coordinate value for this event.
  *
  * @param x The value to add to the X coordinate of this event.
  * @param y The value to add to the Y coordiante of this event.
  */
public void
translatePoint(int x, int y)
{
  this.x += x;
  this.y += y;
}

/*************************************************************************/

/**
  * Returns a string identifying this event.
  *
  * @return A string identifying this event.
  */
public String
paramString()
{
  return(getClass().getName() + " source=" + getSource() + " id=" + getID() +
         " x=" + getX() + " y=" + getY() + " clicks=" + getClickCount());
}

} // class MouseEvent

