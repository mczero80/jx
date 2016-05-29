/* URL.java -- Uniform Resource Locator Class
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


package java.net;

import java.io.InputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.StringTokenizer;
import java.util.Hashtable;

/**
  * This final class represents an Internet Uniform Resource Locator (URL).
  * For details on the syntax of URL's and what they can be used for,
  * refer to RFC 1738, available from <a 
  * href="http://ds.internic.net/rfcs/rfc1738.txt">http://ds.internic.net/rfcs/rfc1738.txt</a>
  * <p>
  * There are a great many protocols supported by URL's such as "http",
  * "ftp", and "file".  This object can handle any arbitrary URL for which
  * a URLStreamHandler object can be written.  Default protocol handlers
  * are provided for the "http" and "ftp" protocols.  Additional protocols
  * handler implementations may be provided in the future.  In any case,
  * an application or applet can install its own protocol handlers that
  * can be "chained" with other protocol hanlders in the system to extend
  * the base functionality provided with this class. (Note, however, that
  * unsigned applets cannot access properties by default or install their
  * own protocol handlers).
  * <p>
  * This chaining is done via the system property java.protocol.handler.pkgs
  * If this property is set, it is assumed to be a "|" separated list of
  * package names in which to attempt locating protocol handlers.  The
  * protocol handler is searched for by appending the string 
  * ".<protocol>.Handler" to each packed in the list until a hander is found.
  * If a protocol handler is not found in this list of packages, or if the
  * property does not exist, then the default protocol handler of
  * "gnu.java.net.<protocol>.Handler" is tried.  If this is
  * unsuccessful, a MalformedURLException is thrown.
  * <p>
  * All of the constructor methods of URL attempt to load a protocol
  * handler and so any needed protocol handlers must be installed when
  * the URL is constructed.
  * <p>
  * Here is an example of how URL searches for protocol handlers.  Assume
  * the value of java.protocol.handler.pkgs is "com.foo|com.bar" and the
  * URL is "news://comp.lang.java.programmer".  URL would looking the 
  * following places for protocol handlers:
  * <p><pre>
  * com.foo.news.Handler
  * com.bar.news.Handler
  * gnu.java.net.news.Handler
  * </pre><p>
  * If the protocol handler is not found in any of those locations, a
  * MalformedURLException would be thrown.
  * <p>
  * Please note that a protocol handler must be a subclass of
  * URLStreamHandler.
  * <p>
  * Normally, this class caches protocol handlers.  Once it finds a handler
  * for a particular protocol, it never tries to look up a new handler
  * again.  However, if the system property
  * gnu.java.net.nocache_protocol_handlers is set, then this
  * caching behavior is disabled.  This property is specific to this
  * implementation.  Sun's JDK may or may not do protocol caching, but it
  * almost certainly does not examine this property.
  * <p>
  * Please also note that an application can install its own factory for
  * loading protocol handlers (see setURLStreamHandlerFactory).  If this is
  * done, then the above information is superseded and the behavior of this
  * class in loading protocol handlers is dependent on that factory.
  *
  * @version 0.5
  *
  * @author Aaron M. Renn (arenn@urbanophile.com)
  *
  * @see URLStreamHandler
  */
public final class URL implements Serializable 
{

/*************************************************************************/

/*
 * Class Variables
 */

/**
  * If an application installs in own protocol handler factory, this is
  * where we keep track of it.
  */
private static URLStreamHandlerFactory factory;

/**
  * This a table where we cache protocol handlers to avoid the overhead
  * of looking them up each time.
  */
private static Hashtable ph_cache = new Hashtable();

/**
  * Whether or not to cache protocol handlers.
  */
private static boolean cache_handlers;

/**
  * The search path of packages to search for protocol handlers in
  */
private static String ph_search_path;

static
{
  String s = System.getProperty("gnu.java.net.nocache_protocol_handlers");
  if (s == null)
    cache_handlers = true;
  else
    cache_handlers = false;

  ph_search_path = System.getProperty("java.protocol.handler.pkgs");

  // Tack our default package on at the ends
  if (ph_search_path != null)
    ph_search_path = ph_search_path + "|" + "gnu.java.net.protocol";
  else
    ph_search_path = "gnu.java.net.protocol";
}

/*************************************************************************/

/*
 * Instance Variables
 */

/**
  * The name of the protocol for this URL
  */
private String protocol;

/**
  * The hostname or IP address of this protocol
  */
private String host;

/**
  * The port number of this protocol or -1 if the port number used is
  * the default for this protocol.
  */
private int port = -1;

/**
  * The "file" portion of the URL
  */
private String file;

/**
  * The anchor portion of the URL
  */
private String ref;

/**
  * The protocol handler in use for this URL
  */
private URLStreamHandler ph;

/**
  * This is the hashCode for this URL
  */
private int hashCode;

/*************************************************************************/

/*
 * Static Methods
 */

/**
  * Sets the URLStreamHandlerFactory for this class.  This factory is
  * responsible for returning the appropriate protocol handler for
  * a given URL.
  *
  * @param fac The URLStreamHandlerFactory class to use
  *
  * @exception Error If the factory is alread set.
  */
public static synchronized void
setURLStreamHandlerFactory(URLStreamHandlerFactory fac)
{
  if (factory == null)
    factory = fac;
  else 
    throw new Error("URLStreamHandlerFactory alread set");
}

/*************************************************************************/

/**
  * This internal method is used in two different constructors to load
  * a protocol handler for this URL.
  *
  * @param The protocol to load a handler for
  *
  * @return A URLStreamHandler for this protocol
  *
  * @exception MalformedURLException If the protocol can't be loaded.
  */
private static URLStreamHandler
getProtocolHandler(String protocol) throws MalformedURLException
{
  URLStreamHandler ph;

  // First, see if a protocol handler is in our cache
  if (cache_handlers)
    {
      Class cls = (Class)ph_cache.get(protocol);
      if (cls != null)
        {
          try
            {
              ph = (URLStreamHandler)cls.newInstance();
              return(ph);
            }
          catch (Exception e) { ; }
        }
    }

  // Next check the factory and use that if set
  if (factory != null)
    {
      ph = factory.createURLStreamHandler(protocol);
      if (ph == null)
        throw new MalformedURLException(protocol);

      if (cache_handlers)
        ph_cache.put(protocol, ph.getClass());

      return(ph);
    }

  // Finally loop through our search path looking for a match
  StringTokenizer st = new StringTokenizer(ph_search_path, "|");
  while (st.hasMoreTokens())
    {
      String clsname = st.nextToken() + "." + protocol + ".Handler";
         
      try
        {
          Class cls = Class.forName(clsname); 
          Object obj = cls.newInstance();
          if (!(obj instanceof URLStreamHandler))
            continue;
          else
            ph = (URLStreamHandler)obj;

          if (cache_handlers)
            ph_cache.put(protocol, cls);

          return(ph);
        }
      catch (Exception e) { ; }
    }

  // Still here, which is bad
  throw new MalformedURLException(protocol);
}

/*************************************************************************/

/*
 * Constructors
 */

/**
  * Constructs a URL and loads a protocol handler for the values passed as
  * arguments.
  * 
  * @param protocol The protocol for this URL ("http", "ftp", etc)
  * @param host The hostname or IP address to connect to
  * @param port The port number to use, or -1 to use the protocol's default port
  * @param file The "file" portion of the URL.
  *
  * @exception MalformedURLException If a protocol handler cannot be loaded
  */
public
URL(String protocol, String host, int port, String file) 
    throws MalformedURLException
{
  this(protocol.toLowerCase(), host, port, file, null);
}

/*************************************************************************/

/**
  * Constructs a URL and loads a protocol handler for the values passed in
  * as arugments.  Uses the default port for the protocol.
  *
  * @param protocol The protocol for this URL ("http", "ftp", etc)
  * @param host The hostname or IP address for this URL
  * @param file The "file" portion of this URL.
  *
  * @exception MalformedURLException If a protocol handler cannot be loaded
  */
public
URL(String protocol, String host, String file) throws MalformedURLException
{
  this(protocol.toLowerCase(), host, -1, file, null);
}

/*************************************************************************/

/**
  * This method initializes a new instance of <code>URL</code> with the
  * specified protocol, host, port, and file.  Additionally, this method
  * allows the caller to specify a protocol handler to use instead of 
  * the default.  If this handler is specified, the caller must have
  * the "specifyStreamHandler" permission (see <code>NetPermission</code>)
  * or a <code>SecurityException</code> will be thrown.
  *
  * @param protocol The protocol for this URL ("http", "ftp", etc)
  * @param host The hostname or IP address to connect to
  * @param port The port number to use, or -1 to use the protocol's default port
  * @param file The "file" portion of the URL.
  * @param ph The protocol handler to use with this URL.
  *
  * @exception MalformedURLException If no protocol handler can be loaded
  * for the specified protocol.
  * @exception SecurityException If the <code>SecurityManager</code> exists
  * and does not allow the caller to specify its own protocol handler.
  */
public
URL(String protocol, String host, int port, String file,
    URLStreamHandler ph) throws MalformedURLException, SecurityException
{
  this.protocol = protocol.toLowerCase();
  this.host = host;
  this.port = port;
  this.file = file;

  if (ph != null)
    {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null)
        sm.checkPermission(new NetPermission("specifyStreamHandler"));

      this.ph = ph;
    }
  else
    {
      this.ph = getProtocolHandler(protocol);
    }

  hashCode = toString().hashCode();
}

/*************************************************************************/

/**
  * This method parses a String representation of a URL within the
  * context of an existing URL.  Principally this means that any fields
  * not present the URL are inheritied from the context URL.  This allows
  * relative URL's to be easily constructed (***true?***).  If the
  * context argument is null, then a complete URL must be specified in the
  * URL string.  If the protocol parsed out of the URL is different 
  * from the context URL's protocol, then then URL String is also
  * expected to be a complete URL.
  * 
  * @param context The context URL
  * @param url A String representing this URL
  *
  * @exception MalformedURLException If a protocol handler cannot be found 
  * for the URL cannot be parsed
  */
public
URL(URL context, String url) throws MalformedURLException
{
  this(context, url, null);
}

/*************************************************************************/

/**
  * This method parses a String representation of a URL within the
  * context of an existing URL.  Principally this means that any fields
  * not present the URL are inheritied from the context URL.  This allows
  * relative URL's to be easily constructed (***true?***).  If the
  * context argument is null, then a complete URL must be specified in the
  * URL string.  If the protocol parsed out of the URL is different 
  * from the context URL's protocol, then then URL String is also
  * expected to be a complete URL.
  * 
  * Additionally, this method allows the caller to specify a protocol handler to 
  * use instead of  the default.  If this handler is specified, the caller must 
  * have the "specifyStreamHandler" permission (see <code>NetPermission</code>)
  * or a <code>SecurityException</code> will be thrown.
  *
  * @param context The context URL
  * @param url A String representing this URL
  * @param ph The protocol handler for this URL
  *
  * @exception MalformedURLException If a protocol handler cannot be found or the URL cannot be parsed
  * @exception SecurityException If the <code>SecurityManager</code> exists
  * and does not allow the caller to specify its own protocol handler.
  */
public
URL(URL context, String url, URLStreamHandler ph) throws MalformedURLException
{
  int end, start = -1;

  int colon_index = url.indexOf(":");
  int slash_index = url.indexOf("/");

  // Find a protocol name in the string if there is one
  if (colon_index != -1)
    if ((slash_index == -1) ||((slash_index != -1) && 
                               (colon_index < slash_index)))
      {
        protocol = url.substring(0, colon_index);
        start = colon_index + 1; // Used for parsing later
      }

  // Handle defaulting of protocol from context.  If no protocol and no
  // context, then no URL.
  if (protocol == null)
    if (context == null)
      throw new MalformedURLException(url);
    else 
      protocol = context.getProtocol();
 
  protocol = protocol.toLowerCase();

  // Default in items as necessary
  if (context != null)
    if (context.getProtocol().toLowerCase().equals(protocol))
      {
        host = context.getHost();
        port = context.getPort();
        file = context.getFile();
        if (file == null)
          file = "";
      }

  // Get the protocol handler and parse the rest of the URL string.
  if (ph != null)
    {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null)
        sm.checkPermission(new NetPermission("specifyStreamHandler"));

      this.ph = ph;
    }
  else
    {
      this.ph = getProtocolHandler(protocol);
    }

  if (start == -1)
    start = 0;

  // We are supposed to stop parsing at the "#" char and treat everything
  // after that as an anchor.
  end = url.indexOf("#");
  if (end == -1)
    end = url.length();
  if (end != (url.length()))
    ref = url.substring(end + 1);

  // Ok, parseURL will not work here right now.  Instead, we just 
  // do a hack by treating the spec URL as a file that should be
  // appended to the context.  Only if the context is null do we try
  // to parse.

  if (context == null)
    {
      this.ph.parseURL(this, url, start, end);
    }
  else
    {
      int idx = file.lastIndexOf("/"); 
      if (idx == -1)
        file = url;
      else if (idx == (file.length() - 1))
        file = file + url;
      else
        {
          file = file.substring(0, idx+1) + url;
        }
    }

  hashCode = toString().hashCode();
}

/*************************************************************************/

/**
  * Initializes a URL from a complete string specification such as
  * "http://www.urbanophile.com/arenn/".  First the protocol name is parsed
  * out of the string.  Then a handler is located for that protocol and
  * the parseURL() method of that protocol handler is used to parse the
  * remaining fields.
  *
  * @param url The complete String representation of a URL
  *
  * @exception MalformedURLException If a protocol handler cannot be found or the URL cannot be parsed
  */
public
URL(String url) throws MalformedURLException
{
  this(null, url);
}

/*************************************************************************/

/*
 * Instance Methods
 */

/**
  * This protected method is used by protocol handlers to set the values
  * of the fields in this URL.  This might be done in the parseURL method
  * of that class.
  *
  * @param protocol The protocol name for this URL
  * @param host The hostname or IP address for this URL
  * @param port The port number of this URL
  * @param file The "file" portion of this URL.
  * @param ref The anchor portion of this URL.
  */
protected synchronized void
set(String protocol, String host, int port, String file, String ref)
{
  //*** Should we ignore null'd fields?  Assume not for now.

  this.protocol = protocol;
  this.host = host;
  this.port = port;
  this.file = file;
  this.ref = ref;
}

/*************************************************************************/

/**
  * Returns the protocol name of this URL
  *
  * @return The protocol
  */
public String
getProtocol()
{
  return(protocol);
} 

/*************************************************************************/

/**
  * Returns the hostname or IP address for this protocol
  *
  * @return The hostname
  */
public String
getHost()
{
  return(host);
}

/*************************************************************************/

/**
  * Returns the port number of this URL or -1 if the default port number is
  * being used
  *
  * @return The port number
  */
public int
getPort()
{
  return(port);
}

/*************************************************************************/

/**
  * Returns the "file" portion of this URL
  *
  * @return The file portion
  */
public String
getFile()
{
  return(file);
}

/*************************************************************************/

/**
  * Returns the ref (sometimes called the "reference") portion of the
  * URL
  *
  * @return The ref
  */
public String
getRef()
{
  return(ref);
}

/*************************************************************************/

/**
  * Test another URL for equality with this one.  This will be true only if
  * the argument is non-null and all of the fields in the URL's match 
  * exactly (ie, protocol, host, port, file, and ref).  Overrides
  * Object.equals().
  *
  * @param url The URL to compare with
  *
  * @return true if the URL is equal, false otherwise
  */
public boolean
equals(Object url)
{
  // Is it null?
  if (url == null)
    return(false);

  // Is it a URL?
  if (!(url instanceof URL))
    return(false);

  URL u = (URL)url;

  // Check everything but the ref
  if (!sameFile(u))
    return(false);

  // Do the ref's match
  String s = u.getRef();
  if (s != null)
    if (!s.equals(getRef()))
      return(false);
  else if (getRef() != null)
    return(false);

  // Still here so everything must be ok
  return(true);
}

/*************************************************************************/

/**
  * Tests whether or not another URL refers to the same "file" as this one.
  * This will be true if and only if the passed object is not null, is a
  * URL, and matches all fields but the ref (ie, protocol, host, port,
  * and file);
  *
  * @param url The URL object to test with
  *
  * @return true if URL matches this URL's file, false otherwise
  */
public boolean
sameFile(URL url)
{
  if (url == null)
    return(false);

  // Do the protocol's match?
  String s = url.getProtocol();
  if (s != null)
    if (!s.equals(getProtocol()))
      return(false);
  else if (getProtocol() != null)
    return(false);

  // Do the hostname's match?
  s = url.getHost();
  if (s != null)
    if (!s.equals(getHost()))
      return(false);
  else if (getHost() != null)
    return(false);

  // Do the port's match?
  if (url.getPort() != getPort())
    return(false);

  // Do the file's match?
  s = url.getFile();
  if (s != null)
    if (!s.equals(getFile()))
      return(false);
  else if (getFile() != null)
    return(false);

  // We're still here, so everything must be ok!
  return(true);
}

/*************************************************************************/

/*
  * This is the implementation of the Comparable interface for URL's.  It
  * will return a negative int, 0, or a positive int depending on whether
  * a URL is less than, equal to, or greater than this URL respectively.
  * This is done by returning the compareTo result on this string
  * representations of these URL's.
  *
  * @param url The URL to compare against
  *
  * @return An int indicating whether a URL is less than, equal to, or greater than this URL
  *
public int
compareTo(Object url)
{
  return(toExternalForm().compareTo(((URL)url).toExternalForm()));
}
*/

/*************************************************************************/

/**
  * Returns a String representing this URL.  The String returned is
  * created by calling the protocol handler's toExternalForm() method.
  *
  * @return A string for this URL
  */
public String
toExternalForm()
{
  return(ph.toExternalForm(this));
}

/*************************************************************************/

/**
  * Returns a String representing this URL.  Identical to toExternalForm().
  * The value returned is created by the protocol handler's 
  * toExternalForm method.  Overrides Object.toString()
  *
  * @return A string for this URL
  */
public String
toString()
{
  return(toExternalForm());
}

/*************************************************************************/

/**
  * Returns a URLConnection for this object created by calling the
  * openConnection() method of the protocol handler
  *
  * @return A URLConnection for this URL
  *
  * @exception IOException If an error occurs
  */
public synchronized URLConnection
openConnection() throws IOException
{
  return(ph.openConnection(this));
}

/*************************************************************************/

/**
  * This method returns an InputStream for this URL by first opening the
  * connection, then calling the getInputStream() method against the
  * connection.
  *
  * @return An InputStream for this URL
  *
  * @exception IOException If an error occurs
  */
public final synchronized InputStream
openStream() throws IOException
{
  return(openConnection().getInputStream());
}

/*************************************************************************/

/**
  * Returns the contents of this URL as an object by first opening a
  * connection, then calling the getContent() method against the connection
  *
  * @return A content object for this URL
  *
  * @exception IOException If an error occurs
  */
public final synchronized Object
getContent() throws IOException
{
  return(openConnection().getContent());
}

/*************************************************************************/

/**
  * This method returns a hash value for this object.
  *
  * @return a hash value for this object.
  */
public int
hashCode()
{
  return(hashCode);
}

} // class URL

