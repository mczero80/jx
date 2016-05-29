/* ActionEvent.java -- An action has been triggered
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

/**
  * This event is generated when an action on a component (such as a
  * button press) occurs.
  *
  * @author Aaron M. Renn (arenn@urbanophile.com)
  */
public class ActionEvent extends java.awt.AWTEvent 
             implements java.io.Serializable
{

/*
 * Static Variables
 */

/**
  * The first id number in the range of action id's.
  */
public static final int ACTION_FIRST = 1001;

/**
  * The last id number in the range of action id's.
  */
public static final int ACTION_LAST = 1001;

/**
  * An event id indicating that an action has occurred.
  */
public static final int ACTION_PERFORMED = 1001;

/**
  * Bit mask indicating the shift key was pressed.
  */
public static final int SHIFT_MASK = 1;

/**
  * Bit mask indicating the control key was pressed.
  */
public static final int CTRL_MASK = 1;

/**
  * Bit mask indicating the that meta key was pressed.
  */
public static final int META_MASK = 1;

/**
  * Bit mask indicating that the alt key was pressed.
  */
public static final int ALT_MASK = 1;

/*************************************************************************/

/*
 * Instance Variables
 */

/**
  * @serial Modifiers for this event
  */
private int modifiers;

/**
  * @serial The command for this event
  */
private String actionCommand;

/*************************************************************************/

/*
 * Constructors
 */

/**
  * Initializes a new instance of <code>ActionEvent</code> with the
  * specified source, id, and command.
  *
  * @param source The event source.
  * @param id The event id.
  * @param command The command string for this action.
  */
public
ActionEvent(Object source, int id, String command)
{
  super(source, id);
  this.actionCommand = command;
}

/*************************************************************************/

/**
  * Initializes a new instance of <code>ActionEvent</code> with the
  * specified source, id, command, and modifiers.
  *
  * @param source The event source.
  * @param id The event id.
  * @param command The command string for this action.
  * @param modifiers The keys held down during the action, which is 
  * combination of the bit mask constants defined in this class.
  */
public
ActionEvent(Object source, int id, String command, int modifiers)
{
  this(source, id, command);
  this.modifiers = modifiers;
}

/*************************************************************************/

/*
 * Instance Methods
 */

/**
  * Returns the command string associated with this action.
  *
  * @return The command string associated with this action.
  */
public String
getActionCommand()
{
  return(actionCommand);
}

/*************************************************************************/

/**
  * Returns the keys held down during the action.  This value will be a
  * combination of the bit mask constants defined in this class.
  *
  * @return The modifier bits.
  */
public int
getModifiers()
{
  return(modifiers);
}

/*************************************************************************/

/**
  * Returns a string that identifies the action event.
  *
  * @return A string identifying the event.
  */
public String
paramString()
{
  return("ActionEvent: source=" + getSource().toString() + " id=" + getID() +
         " command=" + getActionCommand() + " modifiers=" + getModifiers());
}

} // class ActionEvent 

