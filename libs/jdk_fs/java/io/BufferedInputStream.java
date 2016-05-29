/* BufferedInputStream.java -- An input stream that implements buffering
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

/**
  * This subclass of <code>FilterInputStream</code> buffers input from an 
  * underlying implementation to provide a possibly more efficient read
  * mechanism.  It maintains the buffer and buffer state in instance 
  * variables that are available to subclasses.  The default buffer size
  * of 512 bytes can be overridden by the creator of the stream.
  * <p>
  * This class also implements mark/reset functionality.  It is capable
  * of remembering any number of input bytes, to the limits of
  * system memory or the size of <code>Integer.MAX_VALUE</code>
  * <p>
  * Please note that this class does not properly handle character
  * encodings.  Consider using the <code>BufferedReader</code> class which
  * does.
  *
  * @version 0.0
  *
  * @author Aaron M. Renn (arenn@urbanophile.com)
  */
public class BufferedInputStream extends FilterInputStream
{

/*************************************************************************/

/*
 * Class Variables
 */

/**
  * This is the default buffer size
  */
private static final int DEFAULT_BUFFER_SIZE = 512;

/*************************************************************************/

/*
 * Instance Variables
 */

/**
  * The buffer used for storing data from the underlying stream.
  */
protected byte[] buf;

/**
  * The number of valid bytes currently in the buffer.  It is also the index
  * of the buffer position one byte past the end of the valid data.
  */
protected int count;

/**
  * The index of the next character that will by read from the buffer.
  * When <code>pos == count</code>, the buffer is empty.
  */
protected int pos;

/**
  * The value of <code>pos</code> when the <code>mark()</code> method was called.  
  * This is set to -1 if there is no mark set.
  */
protected int markpos = -1;

/**
  * This is the maximum number of bytes than can be read after a 
  * call to <code>mark()</code> before the mark can be discarded.  After this may
  * bytes are read, the <code>reset()</code> method may not be called successfully.
  */
protected int marklimit;

/**
  * This buffer is used to hold marked data if the underlying stream does
  * not support mark/reset and if we end up reading more data than we can
  * hold in the internal buffer prior to a reset class.
  */
private byte[] markbuf;

/**
  * This is the current position into the markbuf from which data will
  * be restored during a reset operation
  */
private int markbufpos;

/**
  * This is the current number of bytes in markbuf
  */
private int markbufcount;

/**
  * This boolean variable is used to let the <code>refillBuffer()</code> method 
  * know if it should read from markbuf or the underlying stream.  true means
  * read from markbuf.
  */
private boolean doing_reset = false;

/**
  * Determines whether or not the buffer has ever been read
  */
private boolean primed = false;

/*************************************************************************/

/*
 * Constructors
 */

/**
  * This method initializes a new <code>BufferedInputStream</code> that will
  * read from the specified subordinate stream with a default buffer size
  * of 512 bytes
  *
  * @param in The subordinate stream to read from
  */
public
BufferedInputStream(InputStream in)
{
  this(in, DEFAULT_BUFFER_SIZE);
}

/*************************************************************************/

/**
  * This method initializes a new <code>BufferedInputStream</code> that will
  * read from the specified subordinate stream with a buffer size that
  * is specified by the caller.
  *
  * @param in The subordinate stream to read from
  * @param bufsize The buffer size to use
  */
public
BufferedInputStream(InputStream in, int bufsize)
{
  super(in);

  buf = new byte[bufsize];
}

/*************************************************************************/

/*
 * Instance Methods
 */

/**
  * This method closes the underlying input stream and frees any
  * resources associated with it.
  *
  * @exception IOException If an error occurs.
  */
public void
close() throws IOException
{
  super.close();
}

/*************************************************************************/

/**
  * This method marks a position in the input to which the stream can be
  * "reset" by calling the <code>reset()</code> method.  The parameter
  * <code>readlimit</code> is the number of bytes that can be read from the 
  * stream after setting the mark before the mark becomes invalid.  For
  * example, if <code>mark()</code> is called with a read limit of 10, then when
  * 11 bytes of data are read from the stream before the <code>reset()</code>
  * method is called, then the mark is invalid and the stream object
  * instance is not required to remember the mark.
  * <p>
  * Note that the number of bytes that can be remembered by this method
  * can be greater than the size of the internal read buffer.  It is also
  * not dependent on the subordinate stream supporting mark/reset
  * functionality.
  *
  * @param readlimit The number of bytes that can be read before the mark becomes invalid
  */
public synchronized void
mark(int readlimit)
{
  // If we already have a special buffer that we are reading text from,
  // adjust it to handle the new mark length
  if (doing_reset && (markbuf != null))
    {
      byte[] tmpbuf = new byte[readlimit + buf.length];

      if (pos != count)
        System.arraycopy(buf, pos, tmpbuf, 0, count - pos);

      int copy_bytes = readlimit;
      if ((markbufcount - markbufpos) <= readlimit)
        copy_bytes = markbufcount - markbufpos;

      System.arraycopy(markbuf, markbufpos, tmpbuf, count - pos, copy_bytes);

      primed = false;
      markbuf = tmpbuf;
      markbufpos = 0;
      markbufcount = copy_bytes + (count - pos);
      pos = 0;
      count = 0;
    }

  // We can hold the whole marked region in our buffer, but we need
  // to shift the valid data still in the buffer to the beginning
  // in order to do so
  if ((readlimit <= buf.length) && (readlimit > (count - pos)) &&
     (pos != count) && !doing_reset)
    {
      byte[] tmpbuf = new byte[buf.length];

      System.arraycopy(buf, pos, tmpbuf, 0, count - pos);

      buf = tmpbuf;
      count = count - pos;
      pos = 0;
    }

  markpos = pos;
  marklimit = readlimit;

  if (in.markSupported())
    in.mark(readlimit);
}

/*************************************************************************/

/**
  * This method returns <code>true</code> to indicate that this class supports
  * mark/reset functionality.
  *
  * @return <code>true</code> to indicate that mark/reset functionality is supported
  *
  */
public boolean
markSupported()
{
  return(true);
}

/*************************************************************************/

/**
  * This method resets a stream to the point where the <code>mark()</code> method
  * was called.  Any bytes that were read after the mark point was set will
  * be re-read during subsequent reads.
  * <p>
  * This method will throw an IOException if the number of bytes read from
  * the stream since the call to <code>mark()</code> exceeds the mark limit
  * passed when establishing the mark.
  *
  * @exception IOException If an error occurs;
  */
public synchronized void
reset() throws IOException
{
  if (markpos == -1)
    throw new IOException("Stream not marked");

  doing_reset = false;

  if (markbuf == null)
    {
      pos = markpos;  
      markpos = -1;
    }
  else
    {
      markpos = -1;
      if (in.markSupported())
        {
          in.reset();

          if (markbuf != null)
            {
              System.arraycopy(markbuf, 0, buf, 0, markbuf.length);
              pos = 0;
              count = markbuf.length;
              markbuf = null;
            }
        }
      else
        {
          pos = 0;
          count = 0;
          markbufpos = 0;
          primed = false;
          doing_reset = true;
        }
    }
}

/*************************************************************************/

/**
  * This method returns the number of bytes that can be read from this
  * stream before a read can block.  A return of 0 indicates that blocking
  * might (or might not) occur on the very next read attempt.
  * <p>
  * The number of available bytes will be the number of read ahead bytes 
  * stored in the internal buffer plus the number of available bytes in
  * the underlying stream.
  *
  * @return The number of bytes that can be read before blocking could occur
  *
  * @exception IOException If an error occurs
  */
public int
available() throws IOException
{
  return((count - pos) + in.available());
}

/*************************************************************************/

/**
  * This method skips the specified number of bytes in the stream.  It
  * returns the actual number of bytes skipped, which may be less than the
  * requested amount.
  * <p>
  * This method first discards bytes in the buffer, then calls the
  * <code>skip</code> method on the underlying stream to skip the remaining bytes.
  *
  * @param num_bytes The requested number of bytes to skip
  *
  * @return The actual number of bytes skipped.
  *
  * @exception IOException If an error occurs
  */
public synchronized long
skip(long num_bytes) throws IOException
{
  if (num_bytes <= 0)
    return(0);

  if ((count - pos) >= num_bytes)
    {
      pos += num_bytes;
      return(num_bytes);
    } 

  int bytes_discarded = count - pos;
  pos = 0;
  count = 0;

  long bytes_skipped = in.skip(num_bytes - bytes_discarded); 

  return(bytes_discarded + bytes_skipped);
}

/*************************************************************************/

/**
  * This method reads an unsigned byte from the input stream and returns it
  * as an int in the range of 0-255.  This method also will return -1 if
  * the end of the stream has been reached.
  * <p>
  * This method will block until the byte can be read.
  *
  * @return The byte read or -1 if end of stream
  *
  * @exception IOException If an error occurs
  */
public synchronized int
read() throws IOException
{
  if ((pos == count) || !primed)
    {
      refillBuffer(1);
      
      if (pos == count)
        return(-1);
    }

  ++pos;

  return((buf[pos - 1] & 0xFF));
}

/*************************************************************************/

/**
  * This method read bytes from a stream and stores them into a caller
  * supplied buffer.  It starts storing the data at index <code>offset</code> into
  * the buffer and attempts to read <code>len</code> bytes.  This method can
  * return before reading the number of bytes requested.  The actual number
  * of bytes read is returned as an int.  A -1 is returned to indicate the
  * end of the stream.
  * <p>
  * This method will block until some data can be read.
  *
  * @param buf The array into which the bytes read should be stored
  * @param offset The offset into the array to start storing bytes
  * @param len The requested number of bytes to read
  *
  * @return The actual number of bytes read, or -1 if end of stream.
  *
  * @exception IOException If an error occurs.
  */
public synchronized int
read(byte[] buf, int offset, int len) throws IOException
{
  if (len == 0)
    return(0);

  // Read the first byte here in order to allow IOException's to 
  // propagate up
  int byte_read = read();
  if (byte_read == -1) 
    return(-1);
  buf[offset] = (byte)byte_read;

  int total_read = 1;
  if (len == total_read)
    return(total_read);

  // Read the rest of the bytes
  try
    {
      for(;total_read != len;)
        {
          if (pos == count)
            refillBuffer(len - total_read);

          if (pos == count)
            if (total_read == 0)
              return(-1);
            else
              return(total_read);

          if ((len - total_read) <= (count - pos))
            {
              System.arraycopy(this.buf, pos, buf, offset + total_read, 
                               len - total_read);

              pos += (len - total_read);
              total_read += (len - total_read);
            }
          else
            {
              System.arraycopy(this.buf, pos, buf, offset + total_read, 
                               count - pos);

              total_read += (count - pos);
              pos += (count - pos);
            }
        }
    }
  catch (IOException e)
    {
      return(total_read);
    }

  return(total_read);
}

/*************************************************************************/

/**
  * This private method is used to refill the buffer when it empty.  But
  * it also handles writing out bytes to the mark buffer and restoring
  * them from the mark buffer if necessary.  The paramter is the number
  * of additional bytes planned to be read, so that the mark can be
  * invalidated if necessary.
  * 
  * @param addl_bytes The number of additional bytes the caller plans to read
  */
private void
refillBuffer(int addl_bytes) throws IOException
{
  primed = true;

  // Handle case where we are re-reading stored bytes during a reset
  if (doing_reset)
    {
      if ((markbufcount - markbufpos) <= buf.length)
        {
          System.arraycopy(markbuf, markbufpos, buf, 0,
                           markbufcount - markbufpos);

          pos = 0;
          count = markbufcount - markbufpos;
          markbuf = null;
          doing_reset = false;

          return;
        }
      else
        {
          System.arraycopy(markbuf, markbufpos, buf, 0, buf.length);

          pos = 0;
          count = buf.length;
          markbufpos += buf.length;
          return;
        }
    }

  // Copy bytes for mark/reset into another buffer if we are out of space
  if ((markpos != -1) && (markbuf == null))
    {
      // If the underlying stream supports mark/reset, we only have to
      // store previously buffered bytes since the underlying stream will
      // remember the rest for us.
      if (in.markSupported())
        markbuf = new byte[count - markpos];
      else
        markbuf = new byte[marklimit + buf.length];

      System.arraycopy(buf, markpos, markbuf, 0, count - markpos);
      markbufpos = 0;
      markbufcount = count - markpos;
    }

  // Read some more bytes.  Note that if pos != count, it means we
  // copied some back out of the mark buffer but still want more
  int bytes_read;
  bytes_read = in.read(buf);
    
  if (bytes_read == -1)
    {
      pos = 0;
      count = 0;
      return;
    }
  
  pos = 0;
  count = bytes_read;

  // We can't remember any more bytes, so invalidate the mark;
  if ((markbuf != null) && (markbufcount >= (markbuf.length - buf.length)) &&
    !in.markSupported())
    {
      markbuf = null;
      markpos = -1;
    }

  // If we are saving marked bytes in a separate buffer, copy them in
  if ((markbuf != null) && !in.markSupported())
    {
      int len;
      if (bytes_read > (markbuf.length - markbufcount))
        len = markbuf.length - markbufcount;
      else
        len = bytes_read;

      System.arraycopy(buf, 0, markbuf, markbufcount, len);
      markbufcount += len;
    }
}

} // class BufferedInputStream

