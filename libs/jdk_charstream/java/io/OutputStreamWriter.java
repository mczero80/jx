/* OutputStreamWriter.java -- Writer that converts chars to bytes
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


package java.io;

//import gnu.java.io.EncodingManager;
//import gnu.java.io.encode.Encoder;

/**
  * This class writes characters to an output stream that is byte oriented
  * It converts the chars that are written to bytes using an encoding layer,
  * which is specific to a particular encoding standard.  The desired
  * encoding can either be specified by name, or if no encoding is specified,
  * the system default encoding will be used.  The system default encoding
  * name is determined from the system property <code>file.encoding</code>.
  * The only encodings that are guaranteed to be available are "8859_1"
  * (the Latin-1 character set) and "UTF8".  Unfortunately, Java does not
  * provide a mechanism for listing the encodings that are supported in
  * a given implementation.
  * <p>
  * Here is a list of standard encoding names that may be available:
  * <p>
  * <ul>
  * <li>8859_1 (ISO-8859-1/Latin-1)
  * <li>8859_2 (ISO-8859-2/Latin-2)
  * <li>8859_3 (ISO-8859-3/Latin-3)
  * <li>8859_4 (ISO-8859-4/Latin-4)
  * <li>8859_5 (ISO-8859-5/Latin-5)
  * <li>8859_6 (ISO-8859-6/Latin-6)
  * <li>8859_7 (ISO-8859-7/Latin-7)
  * <li>8859_8 (ISO-8859-8/Latin-8)
  * <li>8859_9 (ISO-8859-9/Latin-9)
  * <li>ASCII (7-bit ASCII)
  * <li>UTF8 (UCS Transformation Format-8)
  * <li>More Later
  * </ul>
  *
  * @version 0.0
  *
  * @author Aaron M. Renn (arenn@urbanophile.com)
  */
public class OutputStreamWriter extends Writer
{

/*************************************************************************/

/**
  * This is the byte-character encoder class that does the writing and
  * translation of characters to bytes before writing to the underlying
  * class.
  */
//private Encoder out;

/*************************************************************************/

/*
 * Constructors
 */

/**
  * This method initializes a new instance of <code>OutputStreamWriter</code>
  * to write to the specified stream using the default encoding.
  *
  * @param out The <code>OutputStream</code> to write to
  */
public
OutputStreamWriter(OutputStream out)
{
    //  this.out = EncodingManager.getEncoder(out);
}

/*************************************************************************/

/**
  * This method initializes a new instance of <code>OutputStreamWriter</code>
  * to write to the specified stream using a caller supplied character
  * encoding scheme.  Note that due to a deficiency in the Java language
  * design, there is no way to determine which encodings are supported.
  *
  * @param out The <code>OutputStream</code> to write to
  * @param encoding_scheme The name of the encoding scheme to use for character to byte translation
  *
  * @exception UnsupportedEncodingException If the named encoding is not available.
  */
public
OutputStreamWriter(OutputStream out, String encoding_scheme) 
                              throws UnsupportedEncodingException
{
    //  this.out = EncodingManager.getEncoder(out, encoding_scheme);
}

/*************************************************************************/

/*
 * Instance Methods
 */

/**
  * This method closes this stream, and the underlying <code>OutputStream</code>
  *
  * @exception IOException If an error occurs
  */
public void
close() throws IOException
{
    // out.close();
}

/*************************************************************************/

/**
  * This method flushes any buffered bytes to the underlying output sink.
  *
  * @exception IOException If an error occurs
  */
public void
flush() throws IOException
{
    //  out.flush();
}

/*************************************************************************/

/**
  * This method returns the name of the character encoding scheme currently
  * in use by this stream.  If the stream has been closed, then this method
  * may return <code>null</code>.
  *
  * @return The encoding scheme name
  */
public String
getEncoding()
{
    return null;//return(out.getSchemeName());
}

/*************************************************************************/

/**
  * This method writes a single character to the output stream.
  *
  * @param c The char to write, passed as an int.
  *
  * @exception IOException If an error occurs
  */
public void
write(int c) throws IOException
{
    //  out.write(c);
}

/*************************************************************************/

/**
  * This method writes <code>len</code> characters from the specified
  * array to the output stream starting at position <code>offset</code>
  * into the array.
  *
  * @param buf The array of character to write from
  * @param offset The offset into the array to start writing chars from
  * @param len The number of chars to write.
  *
  * @exception IOException If an error occurs
  */
public void
write(char[] buf, int offset, int len) throws IOException
{
    //  out.write(buf, offset, len);
}

/*************************************************************************/

/**
  * This method writes <code>len</code> bytes from the specified 
  * <code>String</code> starting at position <code>offset</code> into the
  * <code>String</code>.
  *
  * @param str The <code>String</code> to write chars from
  * @param offset The position in the <code>String</code> to start writing chars from
  * @param len The number of chars to write
  *
  * @exception IOException If an error occurs
  */
public void
write(String str, int offset, int len) throws IOException
{
    //  out.write(str, offset, len);
}

} // class OutputStreamWriter

