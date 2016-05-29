/* DefaultPermissionCollection.java -- Default perm collection type
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


package gnu.java.security;

import java.security.Permission;
import java.security.PermissionCollection;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Enumeration;

/**
  * We use this permission collection type internally when the 
  * <code>newPermissionCollection</code> method on a permission object
  * returns <code>null</code>.  This collection stores permissions of the
  * same type in a <code>Hashtable</code>.
  *
  * @version 0.0
  *
  * @author Aaron M. Renn (arenn@urbanophile.com)
  */
public class DefaultPermissionCollection extends PermissionCollection
                                         implements Serializable
{

/*************************************************************************/

/*
 * Instance Variables
 */

/**
  * This is the type of Permission we can store
  */
private Class permcls;

/**
  * This is the Hashtable where we store permissions.
  */
private Hashtable perms = new Hashtable();

/*************************************************************************/

/*
 * Constructors
 */

/**
  * Construct a new collection that will hold the given type of permission.
  */
public
DefaultPermissionCollection(String permtype) throws IllegalArgumentException
{
  try
    {
      permcls = Class.forName(permtype);
    }
  catch(ClassNotFoundException e)
    {
      throw new IllegalArgumentException(e.getMessage());
    }
}

/*************************************************************************/

/*
 * Instance Methods
 */

/**
  * This method adds a new permission to the collection
  */
public void
add(Permission perm) throws SecurityException, IllegalArgumentException
{
  if (isReadOnly())
    throw new SecurityException("PermissionCollection is read only");

  if (!permcls.isInstance(perm))
    throw new IllegalArgumentException("Wrong permission type: " + 
                                       perm.getClass().getName());

  if (perms.get(perm.getName()) != null)
    throw new IllegalArgumentException("Duplicate permission: " +
                                       perm.getName());

  perms.put(perm.getName(), perm);
}

/*************************************************************************/

/**
  * This method checks to see if the specified permission is implied by 
  * this collection.
  */
public boolean
implies(Permission perm)
{
  Object obj = perms.get(perm.getName());
  if (obj == null)
    return(false);

  if (!(obj instanceof Permission))
    return(false);

  Permission p = (Permission)obj;

  return(p.implies(perm));
}

/*************************************************************************/

/**
  * Returns all the elements in this collection.
  */
public Enumeration
elements()
{
  return(perms.elements());
}

} // class DefaultPermissionCollection

