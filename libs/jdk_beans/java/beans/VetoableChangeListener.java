/* java.beans.VetoableChangeListener
   Copyright (C) 1998, 2000 Free Software Foundation, Inc.

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


package java.beans;

import java.util.EventListener;

/**
 ** VetoableChangeListener allows a class to monitor
 ** proposed changes to properties of a Bean and, if
 ** desired, prevent them from occurring.<P>
 **
 ** A vetoableChange() event will be fired <EM>before</EM>
 ** the property has changed.  If any listener rejects the
 ** change by throwing the PropertyChangeException, a new
 ** vetoableChange() event will be fired to all listeners
 ** who received a vetoableChange() event in the first
 ** place informing them of a reversion to the old value.
 ** The value, of course, never actually changed.<P>
 **
 ** <STRONG>Note:</STRONG> This class may not be reliably
 ** used to determine whether a property has actually
 ** changed.  Use the PropertyChangeListener interface
 ** for that instead.
 **
 ** @author John Keiser
 ** @version 1.1.0, 29 Jul 1998
 ** @since JDK1.1
 ** @see java.beans.PropertyChangeListener
 ** @see java.beans.VetoableChangeSupport
 **/

public interface VetoableChangeListener extends EventListener
{
  /** Fired before a Bean's property changes.
   ** @param e the change (containing the old and new values)
   ** @exception PropertyChangeException if the listener
   **            does not desire the change to be made.
   **/
  public abstract void vetoableChange(PropertyChangeEvent e)
    throws PropertyVetoException;
}
