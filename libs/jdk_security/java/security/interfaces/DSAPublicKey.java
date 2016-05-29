/*************************************************************************
/* DSAPublicKey.java -- A Digital Signature Algorithm public key
/*
/* Copyright (c) 1998, 2000 Free Software Foundation, Inc.
/* Written by Aaron M. Renn (arenn@urbanophile.com)
/*
/* This library is free software; you can redistribute it and/or modify
/* it under the terms of the GNU Library General Public License as published 
/* by the Free Software Foundation, either version 2 of the License, or
/* (at your option) any later verion.
/*
/* This library is distributed in the hope that it will be useful, but
/* WITHOUT ANY WARRANTY; without even the implied warranty of
/* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
/* GNU Library General Public License for more details.
/*
/* You should have received a copy of the GNU Library General Public License
/* along with this library; if not, write to the Free Software Foundation
/* Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
/*************************************************************************/

package java.security.interfaces;

import java.security.PublicKey;
import java.math.BigInteger;

/**
  * This interface models a Digital Signature Algorithm (DSA) public key
  *
  * @version 0.0
  *
  * @author Aaron M. Renn (arenn@urbanophile.com)
  */
public interface DSAPublicKey extends DSAKey, PublicKey
{

/*************************************************************************/

/**
  * Static Variables
  */

/**
  * This is the serialization UID for this interface
  */
public static final long serialVersionUID = 1234526332779022332L;

/*************************************************************************/

/**
  * Instance Methods
  */

/**
  * This method returns the value of the DSA public key
  */
public abstract BigInteger
getY();

} // interface DSAPublicKey 

