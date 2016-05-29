/* java.util.TooManyListenersException
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


package java.util;

/**
 * This exception is part of the java event model.  It is thrown if an
 * event listener is added via the addXyzEventListener method, but the
 * object doesn't support any more listeners, e.g. it only supports a
 * single event listener.
 *
 * @see EventListener
 * @see EventObject
 * @author Jochen Hoenicke 
 */

public class TooManyListenersException extends Exception
{
  private static final long serialVersionUID = 5074640544770687831L;

  /**
   * Constructs a TooManyListenersException with no detail message.
   */
  public TooManyListenersException()
  {
  }

  /**
   * Constructs a TooManyListenersException with a detail message.
   * @param detail the detail message.
   */
  public TooManyListenersException(String detail)
  {
    super(detail);
  }
}
