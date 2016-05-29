package jx.net.protocol.tcp;

import java.lang.*;
import java.io.*;
import metaxa.os.*;
import jx.net.protocol.tcp.*;

/**
 * this class is the implementation of an InputStream - it has all the same methods and extends it with a method that is needed
 * by the protocol stack (for example TCP) to hand over its received data
 * the stream maintains a internal buffer used a ringbuffer 
 */ 
class MyInputStream extends InputStream {

    /** the size of internal buffer for incoming data - we set it to 16 KB */
    private final static int BUFFERSIZE = 16 * 1024;
    /** the tcplayer that uses that stream - currently we don´t need it for this stream, but further extensions may need it
     *  and because the output stream needs the reference it is also saved here 
     */
    private TCP tcplayer;
    /** the socket this stream belongs to */
    private Socket socket;
    /** the internal buffer for buffering data received from the stack and not yet read by the application using the stream */
    private byte[] buffer;
    /** pos is the position in the buffer where the next data element is read, fill is the position where to insert the next data
     * (always modulo BUFFERSIZE) 
     */
    private int pos, fill;
    /**
     * the free space in the internal buffer as number of bytes 
     */
    private int freespace;
    /** a lock for the reading methods - if there is no data to read from the buffer the methods wait at this lock
     * and get notified if new data has arrived 
     */
    private Object lock;
    /** if a end of stream symbol is received - currently not used */
    private boolean endOfStream;
    
    /**
     * the class constructor 
     * initializes the internal buffer and the associated variables as pos, fill or freespace
     *
     * @param tcp the TCPlayer used by the socket this stream belongs to - currently not needed
     * @param sock the socket this stream belongs to 
     */
    public MyInputStream(TCP tcp, Socket sock) {
	tcplayer = tcp;
	socket = sock;
	buffer = new byte[BUFFERSIZE];
	pos = fill = 0;
	freespace = BUFFERSIZE;
	lock = new Object();
	endOfStream = false;
    }
    
    /**
     * read just one byte and returns it as a integer
     *
     * @return the next element out of the internal buffer casted to an integer
     *
     * @exception IOException is thrown if there occurs an internal error (waiting at the lock gets interrupted)
     */
    public synchronized int read() throws IOException {
	int ret;
	int i = 0;
	if (endOfStream)
	    return -1;
	// if the connection was aborted, we deliver the data still buffered, but after that we throw a exception
	if (socket.connectionAborted() && (freespace == BUFFERSIZE)) {
	    System.out.println("Connection abort: " + socket.connectionAbortReason());
	    throw new IOException();
	}
	try {
	    if (freespace == BUFFERSIZE) {
		// just security check
		if (pos != fill)
		    System.out.println("MyInputStream.read(): no data, but pos != fill !!");
		lock.wait();
	    }
	}
	catch (InterruptedException e) {
	    System.out.println("InputStream: InterruptedException caught!!");
	    throw new IOException();
	}
	ret = (int)buffer[(pos++ % BUFFERSIZE)];
	freespace++;
	return ret;
    }
	
    /**
     * reads up to b.length bytes and returns it into the users buffer
     *
     * @param b destination byte array 
     *
     * @return the number of bytes returned, -1 if a failure occured
     *
     * @exception IOException if an internal error occured
     */
    public  synchronized int read(byte b[]) throws IOException {
	return read(b, 0, b.length); 
    }

    /**
     * reads up to len bytes and returns it into the users buffer - reads beginning at off and therefore discards the first off bytes
     * from the internal buffer
     *
     * @param b destination byte array
     * @param off offset where the read should start
     * @param len number of bytes that should be read
     *
     * @return number of bytes returned, -1 if a failure occured
     *
     * @exception IOException if an an internal error occured
     */
    public  synchronized int read(byte b[], int off, int len) throws IOException {
	int i = 0;
	int maxreadlength;
	if (endOfStream)
	    return -1;
	// if the connection was aborted, we deliver the data still buffered, but after that we throw a exception
	if (socket.connectionAborted() && (freespace == BUFFERSIZE)) {
	    System.out.println("Connection abort: " + socket.connectionAbortReason());
	    throw new IOException();
	}
	try {
	    if (freespace == BUFFERSIZE) {
		// just security check
		if (pos != fill)
		    System.out.println("MyInputStream.read(): no data, but pos != fill !!");
		lock.wait();
	    }
	}
	catch (InterruptedException e) {
	    System.out.println("InputStream: InterruptedException caught!!");
	}
	// determine the maximum amount of data to read
	maxreadlength = BUFFERSIZE - freespace;
	int readlength;
	if (len > maxreadlength)
	    readlength = maxreadlength;
	else
	    readlength = len;
	readlength -= off;
	if (readlength <= 0)
	    return -1;
	    
	pos += off % BUFFERSIZE;
	freespace += off;
	if (fill > pos) {
	    System.arraycopy(buffer, pos, b, 0, readlength);
	    pos += readlength;
	    freespace += readlength;
	    return readlength;
	}
	else if (pos > fill) {
	    if (readlength > (BUFFERSIZE - pos)) {
		System.arraycopy(buffer, pos, b, 0, BUFFERSIZE - pos);
		readlength -= (BUFFERSIZE - pos);
		freespace += (BUFFERSIZE - pos);
		System.arraycopy(buffer, 0, b, BUFFERSIZE - pos, readlength);
		freespace += readlength;
		int helper = (BUFFERSIZE - pos);
		pos = readlength;
		return (readlength + helper);
	    }
	    else if (readlength <= (BUFFERSIZE - pos)) {
		System.arraycopy(buffer, pos, b, 0, readlength);
		pos += readlength % BUFFERSIZE;
		freespace += readlength;
		return readlength;
	    }
	}
	else {
	    // pos would be equal fill and this should not happen here
	    System.out.println("MyInputStream.read(...): reached pos == fill, but this must not happen!");
	    return -1;
	}
	return -1;
    }
    
    /**
     * discards a number of bytes in the stream
     *
     * @param n the number of bytes that should be skipped and therefore are discarded
     *
     * @return the number of bytes skipped, -1 if the stream is no longer valid (end of stream reached)
     *
     * @exception IOException if a internal error occurs, currently not used
     */
    public synchronized long skip(long n) throws IOException {
	long ret;
	int i = 0;
	if (endOfStream)
	    return -1;
	// if the connection was aborted, we deliver the data still buffered, but after that we throw a exception
	if (socket.connectionAborted() && (freespace == BUFFERSIZE)) {
	    System.out.println("Connection abort: " + socket.connectionAbortReason());
	    throw new IOException();
	}
	if (freespace == BUFFERSIZE)
	    return 0;
	if (n > (BUFFERSIZE - freespace)) {
	    ret = (BUFFERSIZE - freespace);
	}
	else {
	    ret = n;
	}
	
	pos += ret % BUFFERSIZE;
	freespace += ret;
	return ret;
    }

    /**
     * check how much data can be read from the stream
     *
     * @return number of bytes currently available 
     *
     * @exception IOException if an internal error occured
     */
    public synchronized int available() throws IOException {
	// we inform the user but still return the number of data available and do not throw an exception
	if (socket.connectionAborted()) {
	    System.out.println("Connection abort: " + socket.connectionAbortReason());
	}
	return (BUFFERSIZE - freespace);
    }
    /**
     * just for the interface - does nothing in this implementation 
     */
    public void close() throws IOException {}
    /**
     * this method is - corresponding to Java API Spec 1.1 - not supported
     * so the same applies to this implementation 
     */
    public synchronized void mark(int readlimit) {}
    /**
     * this method is - corresponding to Java API Spec 1.1 - not supported
     * so the same applies to this implementation 
     */
    public synchronized void reset() throws IOException {}
    /**
     * this method is - corresponding to Java API Spec 1.1 - not supported
     * so the same applies to this implementation 
     */
    public boolean markSupported() {
	return false;
    }

    /**
     * this method is needed by the tcp-stack to insert new data that has been received by the NIC
     * 
     * @param tcp is a handle to a tcp segment that was received
     * @param offset is the offset where the real data start at
     *
     * @return if the buffer can´t handle that much data false is returned, true otherwise
     */
    public synchronized boolean insertReceived(TCPFormat tcp, int offset) {
	// if there is not enough space in the buffer, we completly reject the segment
	if ((tcp.buf().length - offset) > freespace)
	    return false;
	// this is the case where wrapping at the end of the buffer must be considered
	if (fill > pos) {
	    // if there is not enough space between the fill position till the end of the buffer we have to 
	    // copy the rest at the beginning of the buffer - there must be enough space there from the beginning of the buffer
	    // to the position of pos as otherwise the first check of this method (against freespace) would have rejected the segment
	    if ((BUFFERSIZE - fill) < (tcp.buf().length - offset)) {
		int firstcopy = (BUFFERSIZE - fill);
		System.arraycopy(tcp.buf(), offset, buffer, fill, firstcopy);
		fill = 0;
		freespace -= firstcopy;
		int secondcopy = tcp.buf().length - offset - firstcopy;
		System.arraycopy(tcp.buf(), offset + firstcopy, buffer, fill, secondcopy);
		fill += secondcopy;
		freespace -= secondcopy;
	    }
	    else {
		// there is enough space between fill position and end of buffer
		System.arraycopy(tcp.buf(), offset, buffer, fill, tcp.buf().length);
		fill += tcp.buf().length;
		freespace -= tcp.buf().length;
	    }
	}
	// there is enough space between fill position and reading position pos and no wrapping can occur
	else if (fill < pos) {
	    System.arraycopy(tcp.buf(), offset, buffer, fill, tcp.buf().length);
	    // modulo just for security - it can´t wrap here
	    fill += tcp.buf().length % BUFFERSIZE;
	    freespace -= tcp.buf().length;
	}
	else {
	    // impossible case checked for security
	    System.out.println("MyInputStream.insertReceived(): else reached - pos == fill - this must not happen!!");
	    return false;
	}
	// now notify threads waiting for new data to arrive
	lock.notify();
	return true;
    }
}
