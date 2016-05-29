/* ContainerEvent.java -- Components added/removed from a container
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
import java.awt.Container;

/**
  * This event is generated when a component is added or removed from
  * a container.  Applications do not ordinarily need to received these
  * events since the AWT system handles them internally.
  *
  * @author Aaron M. Renn (arenn@urbanophile.com)
  */
public class ContainerEvent extends ComponentEvent
             implements java.io.Serializable
{

/*
 * Static Variables
 */

/**
  * This is the first id in the id range used by this class.
  */
public static final int CONTAINER_FIRST = 300;

/**
  * This is the last id in the id range used by this class.
  */
public static final int CONTAINER_LAST = 301;

/**
  * This id indicates a component was added to the container.
  */
public static final int COMPONENT_ADDED = 300;

/**
  * This id indicates a component was removed from the container.
  */
public static final int COMPONENT_REMOVED = 301;

/*************************************************************************/

/*
 * Instance Variables
 */

/**
  * @serial This is the child component that was added or removed
  */
private Component child;

/*************************************************************************/

/*
 * Constructors
 */

/**
  * Initializes a new instance of <code>ContainerEvent</code> with the
  * specified source and id.  Additionally, the affected child component
  * is also passed as a parameter.
  *
  * @param source The source container of the event.
  * @param id The event id.
  * @param child The child component affected by this event.
  */
public
ContainerEvent(Component source, int id, Component child)
{
  super(source, id);
  this.child = child;
}

/*************************************************************************/

/*
 * Instance Methods
 */

/**
  * This method returns the child object that was added or removed from
  * the container.
  *
  * @return The child object added or removed.
  */
public Component
getChild()
{
  return(child);
}

/*************************************************************************/

/**
  * Returns the source of this event as a <code>Container</code>.
  *
  * @return The source of the event.
  */
public Container
getContainer()
{
  return((Container)getSource());
}

/*************************************************************************/

/**
  * This method returns a string identifying this event.
  *
  * @return A string identifying this event.
  */
public String
paramString()
{
  return(getClass().getName() + " source=" + getSource() + " id=" + getID() +
         " child=" + getChild());
}

} // class ContainerEvent 

