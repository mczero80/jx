/* java.lang.reflect.InvocationTargetException
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


package java.lang.reflect;

import java.io.*;

/**
 * InvocationTargetException is sort of a way to "wrap" whatever exception 
 * comes up when a method or constructor is called via Reflection.
 *
 * @author John Keiser
 * @version 1.1.0, 31 May 1998
 * @see Method#invoke(Object,Object[])
 * @see Constructor#newInstance(Object[])
 */

public class InvocationTargetException extends Exception 
{
  static final long serialVersionUID = 4085088731926701167L;
  private Throwable targetException = null;
  
  protected InvocationTargetException() 
    {
      super();
    }
  
  /**
   * Create an <code>InvocationTargetException</code> using another 
   * exception.
   * @param targetException the exception to wrap
   */
  public InvocationTargetException(Throwable targetException) 
    {
      super();
      this.targetException = targetException;
    }
  
  /** 
   * Create an <code>InvocationTargetException</code> using another 
   * exception and an error message.
   *
   * @param targetException the exception to wrap
   * @param err an extra reason for the exception-throwing
   */
  public InvocationTargetException(Throwable targetException, String err) 
    {
      super(err);
      this.targetException = targetException;
    }
  
  /**
   * Get the wrapped (targeted) exception.
   * 
   * @return the targeted exception.
   */
  public Throwable getTargetException() 
    {
      return targetException;
    }

  public void printStackTrace()
    {
      if (targetException == null)
	super.printStackTrace();
      else
	targetException.printStackTrace();
    }

  public void printStackTrace(PrintStream ps)
    {
      if (targetException == null)
	super.printStackTrace(ps);
      else
	targetException.printStackTrace(ps);
    }

    /*
  public void printStackTrace(PrintWriter pw)
    {
	
      if (targetException == null)
	  super.printStackTrace(pw);
      else
	targetException.printStackTrace(pw);
	
    }
    */

  /**
   * Serialize the object in a manner binary compatible with the JDK 1.2
   */
    /*
  private void writeObject(java.io.ObjectOutputStream s) 
    throws IOException
    {
      ObjectOutputStream.PutField oFields;
      oFields = s.putFields();
      oFields.put("target", targetException);
      s.writeFields(); 
    }
    */

  /**
   * Deserialize the object in a manner binary compatible with the JDK 1.2
   */    
    /*
  private void readObject(java.io.ObjectInputStream s)
    throws IOException, ClassNotFoundException
    {
      ObjectInputStream.GetField oFields;
      oFields = s.readFields();
      targetException = (Throwable)oFields.get("target", (Throwable)null);
    }
    */
}
