/* AllPermission.java -- Permission to do anything
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


package java.security;

/**
  * This class is a permission that implies all other permissions.  Granting
  * this permission effectively grants all others.  Extreme caution should
  * be exercised in granting this permission.
  *
  * @version 0.0
  *
  * @author Aaron M. Renn (arenn@urbanophile.com)
  */
public class AllPermission extends Permission
{

/*************************************************************************/

/*
 * Constructors
 */

/**
  * This method initializes a new instance of <code>AllPermission</code>.  It
  * performs no actions.
  */
public
AllPermission()
{
  super("all");
}

/*************************************************************************/

/**
  * This method initializes a new instance of <code>AllPermission</code>.  The
  * arguments passed to this method are used to set internal field for the
  * permission name.  However, these are not used in 
  * determining the actual permissions granted.  This class always will
  * return <code>true</code> in its implies method.
  *
  * @param name The name of this permission.
  * @param actions The action list for this permission - ignored in this class.
  */
public 
AllPermission(String name, String actions)
{
  super(name);
}

/*************************************************************************/

/*
 * Instance Methods
 */ 

/**
  * This method always returns <code>true</code> to indicate that this
  * permission always implies that any other permission is also granted.
  *
  * @param perm The <code>Permission</code> to test against - ignored in this class.
  *
  * @return Always returns <code>true</code>
  */
public boolean
implies(Permission perm)
{
  return(true);
}

/*************************************************************************/

/**
  * This method tests this class for equality against another <code>Object</code>.
  * This will return <code>true</code> if and only if the specified 
  * <code>Object</code> is an instance of <code>AllPermission</code>.
  *
  * @param obj The <code>Object</code> to test for equality to this object
  */
public boolean
equals(Object obj)
{
  if (obj instanceof AllPermission)
    return(true);

  return(false);
}

/*************************************************************************/

/**
  * This method returns a hash code for this object.
  *
  * @return A hash value for this object.
  */
public int
hashCode()
{
  return(System.identityHashCode(this));
}

/*************************************************************************/

/**
  * This method returns the list of actions associated with this object.
  * This will always be the empty string ("") for this class.
  *
  * @return The action list.
  */
public String
getActions()
{
  return("");
}

/*************************************************************************/

/**
  * This method returns a new instance of <code>PermissionCollection</code>
  * suitable for holding instance of <code>AllPermission</code>.
  *
  * @return A new <code>PermissionCollection</code>.
  */
public PermissionCollection
newPermissionCollection()
{
  return(null);
}

} // class AllPermission

