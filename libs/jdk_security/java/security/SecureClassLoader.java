/* SecureClassLoader.java --- A Secure Class Loader
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


package java.security;

/**
   A Secure Class Loader for loading classes with additional 
   support for specifying code source and permissions when
   they are retrieved by the system policy handler.

   @since JDK 1.2
   
   @author Mark Benvenuto
*/
public class SecureClassLoader extends ClassLoader
{

  private ClassLoader parent;

  /**
     Creates a new SecureClassLoader specifying the parent to make
     calls back to.

     If there is a security manager it first calls,
     checkCreateClassLoader to ensure creation of the class loader 
     is allowed.

     @param ClassLoader parent class loader

     @throws SecurityException if security manager exists and denies
     access.
  */
  protected SecureClassLoader(ClassLoader parent)
  {
    SecurityManager sm = System.getSecurityManager();
    if(sm != null)
      sm.checkCreateClassLoader();
    this.parent = parent;
  }

  /**
     Creates a new SecureClassLoader using the default parent to make
     calls back to.

     If there is a security manager it first calls,
     checkCreateClassLoader to ensure creation of the class loader 
     is allowed.

     @throws SecurityException if security manager exists and denies
     access.
  */
  protected SecureClassLoader()
  {
    SecurityManager sm = System.getSecurityManager();
    if(sm != null)
      sm.checkCreateClassLoader();
    this.parent = getClass().getClassLoader();
    /* FIXME - Use this code when Classloader is JDK 1.2 compatible
       this.parent = ClassLoader.getSystemClassLoader();
    */
  }

  /** 
      Creates a class using an array of bytes and a 
      CodeSource.

      @param name the name to give the class.  null if unknown.
      @param b the data representing the classfile, in classfile format.
      @param off the offset into the data where the classfile starts.
      @param len the length of the classfile data in the array.
      @param cs the CodeSource for the class

      @return the class that was defined and optional CodeSource.

      @exception ClassFormatError if the byte array is not in proper classfile format.
  */
  protected final Class defineClass(String name, byte[] b, int off, int len, CodeSource cs)
  {
    ProtectionDomain protectionDomain = new ProtectionDomain( cs, getPermissions( cs ) );
    try {
      /*
	FIXME after 1.2 support is added to Classloader
	Class c = parent.defineClass(name, b, off, len, protectionDomain);
	return c;
      */
      System.err.println("SecureClassLoader is broken because it is waiting got ClassLoader to be JDK 1.2 compatible");
      return null;
    } catch( ClassFormatError cfe )	{
      return null;
    }
  }

  /**
     Returns a PermissionCollection for the specified CodeSource.
     The default implmentation invokes 
     java.security.Policy.getPermissions.

     This method is called by defineClass that takes a CodeSource
     arguement to build a proper ProtectionDomain for the class
     being defined.
	
  */
  protected PermissionCollection getPermissions(CodeSource cs)
  {
    Policy policy = Policy.getPolicy();
    return policy.getPermissions( cs );
  }

  /**
     FIXME 
     JDK 1.1 hack to make this work
     loadClass is not abstract in JDK 1.2
  */

  protected Class loadClass(String name,
			    boolean resolve)
    throws ClassNotFoundException
  {
    Class c = findLoadedClass(name);
    if( c != null )
      parent.loadClass(name);
    /*if( c!= null ) <-- JDK 1.2 only
      findClass( name );*/
    resolveClass( c );
    return c;
  }

}
