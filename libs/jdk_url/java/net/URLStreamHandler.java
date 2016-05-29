/* URLStreamHandler.java -- Abstract superclass for all protocol handlers
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


package java.net;

import java.io.IOException;

/**
  * This class is the superclass of all URL protocol handlers.  The URL
  * class loads the appropriate protocol handler to establish a connection
  * to a (possibly) remote service (eg, "http", "ftp") and to do protocol
  * specific parsing of URL's.  Refer to the URL class documentation for
  * details on how that class locates and loads protocol handlers.
  * <p>
  * A protocol handler implementation should override the openConnection()
  * method, and optionally override the parseURL() and toExternalForm()
  * methods if necessary. (The default implementations will parse/write all
  * URL's in the same form as http URL's).  A protocol  specific subclass 
  * of URLConnection will most likely need to be created as well.
  * <p>
  * Note that the instance methods in this class are called as if they
  * were static methods.  That is, a URL object to act on is passed with
  * every call rather than the caller assuming the URL is stored in an
  * instance variable of the "this" object.
  * <p>
  * The methods in this class are protected and accessible only to subclasses.
  * URLStreamConnection objects are intended for use by the URL class only,
  * not by other classes (unless those classes are implementing protocols).
  *
  * @version 0.5
  *
  * @author Aaron M. Renn (arenn@urbanophile.com)
  *
  * @see URL
  */
public abstract class URLStreamHandler
{

/*************************************************************************/

/*
 * Constructors
 */

/**
  * Do nothing constructor for subclass
  */
public
URLStreamHandler()
{
  ; 
}

/*************************************************************************/

/*
 * Instance Methods
 */ 

/**
  * Returns a URLConnection for the passed in URL.  Note that this should
  * not actually create the connection to the (possibly) remote host, but
  * rather simply return a URLConnection object.  The connect() method of
  * URL connection is used to establish the actual connection, possibly
  * after the caller sets up various connection options.
  *
  * @param url The URL to get a connection object for
  *
  * @return A URLConnection object for the given URL
  *
  * @exception IOException If an error occurs
  */
protected abstract URLConnection
openConnection(URL url) throws IOException;

/*************************************************************************/

/**
  * This method parses the string passed in as a URL and set's the
  * instance data fields in the URL object passed in to the various values
  * parsed out of the string.  The start parameter is the position to start
  * scanning the string.  This is usually the position after the ":" which
  * terminates the protocol name.  The end parameter is the position to
  * stop scanning.  This will be either the end of the String, or the
  * position of the "#" character, which separates the "file" portion of
  * the URL from the "anchor" portion.
  * <p>
  * This method assumes URL's are formatted like http protocol URL's, so 
  * subclasses that implement protocols with URL's the follow a different 
  * syntax should override this method.  The lone exception is that if
  * the protocol name set in the URL is "file", this method will accept
  * a an empty hostname (i.e., "file:///"), which is legal for that protocol
  *
  * @param url The URL object in which to store the results
  * @param url_string The String-ized URL to parse
  * @param start The position in the string to start scanning from
  * @param end The position in the string to stop scanning
  */
protected void
parseURL(URL url, String url_string, int start, int end)
{
  // This method does not throw an exception or return a value.  Thus our
  // strategy when we encounter an error in parsing is to return without
  // doing anything.

  // Bunches of things should be true.  Make sure.
  if (end < start)
    return;
  if ((end - start) < 2)
    return;
  if (start > url_string.length())
    return;
  if (end > url_string.length())
    end = url_string.length(); // This should be safe

  // Turn end into an offset from the end of the string instead of 
  // the beginning
  end = url_string.length() - end;

  // Skip remains of protocol
  url_string = url_string.substring(start);
  if (!url_string.startsWith("//"))
    return;
  url_string = url_string.substring(2);

  // Declare some variables
  String host = null;
  int port = -1;
  String file = null;
  String anchor = null;

  // Process host and port
  int slash_index = url_string.indexOf("/");
  int colon_index = url_string.indexOf(":");

  if (slash_index > (url_string.length() - end))
    return;
  else if (slash_index == -1)
    slash_index = url_string.length() - end;

  if ((colon_index == -1) || (colon_index > slash_index))
    {
      host = url_string.substring(0, slash_index);
    }
  else
    {
      host = url_string.substring(0, colon_index);
      
      String port_str = url_string.substring(colon_index + 1, slash_index);
      try
        {
          port = Integer.parseInt(port_str);
        }
      catch (NumberFormatException e)
        {
          return;
        }
    }
  if (slash_index < (url_string.length() - 1))
    url_string = url_string.substring(slash_index + 1);
  else
    url_string = "";

  // Process file and anchor 
  if (end == 0)
    {
      file = "/" + url_string;
      anchor = null;
    }
  else
    {
      file = "/" + url_string.substring(0, url_string.length() - end);

      // Only set anchor if end char is a '#'.  Otherwise assume we're
      // just supposed to stop scanning for some reason
      if (url_string.charAt(url_string.length() - end) == '#')
        anchor = url_string.substring((url_string.length() - end) + 1,
                                      url_string.length());
      else
        anchor = null;
    }
  if ((file == null) || (file == ""))
    file = "/";

  // Now set the values
  setURL(url, url.getProtocol(), host, port, file, anchor); 
}

/*************************************************************************/

/**
  * This method converts a URL object into a String.  This method creates
  * Strings in the mold of http URL's, so protocol handlers which use URL's
  * that have a different syntax should override this method
  *
  * @param url The URL object to convert
  */
protected String
toExternalForm(URL url)
{
  String protocol = url.getProtocol();
  String host = url.getHost();
  int port = url.getPort();
  String file = url.getFile();
  String anchor = url.getRef();

  return(((protocol != null) ? (protocol + "://") : "") + 
         ((host != null) ? host : "") + 
         ((port != -1) ? (":" + port) : "") + 
         ((file != null) ? file : "/") +
         ((anchor != null) ? ("#" + anchor) : ""));
}

/*************************************************************************/

/**
  * This methods sets the instance variables representing the various fields
  * of the URL to the values passed in.
  *
  * @param url The URL in which to set the values
  * @param protocol The protocol name
  * @param host The host name
  * @param port The port number
  * @param file The file portion
  * @param anchor The anchor portion
  */
protected void
setURL(URL url, String protocol, String host, int port, String file,
       String anchor)
{
  url.set(protocol, host, port, file, anchor);
}

} // class URLStreamHandler

