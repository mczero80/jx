package jx.net.protocol.tcp;

import java.lang.*;
import java.io.*;
import jx.timer.*;
import jx.zero.*;


/**
 * this class is the implementation of an OutputStream 
 * the stream does no buffering with one exception 
 * as the stream is used for the tcplayer, the immediate sending of little data segment is prevented in order to avoid 
 * segments containing just some bytes
 * such write method calls get buffered and can be forced to be send by calling flush()
 * furthermore a timer is started that calls flush() every 10 seconds so no data will be longer buffered than 10 seconds
 */ 
class MyOutputStream extends OutputStream { 
    /** the tcplayer used by the socket this stream belongs to - needed for sending of the data */
    private TCP tcplayer;
    /** the socket we belong to - we need to check wether there is an established connection and this information is maintained
     * by the socket
     */
    private Socket socket;
    /** insert position in the buffer for the buffering of small data fragments */
    private int pos;
    /** the maximum datasize of a segment that the tcplayer can transmit */
    private int maxDataSize;
    /** the buffer for the buffering of small data fragments */
    private byte[] tempBuffer;
    /** the size of the buffer above */
    private int bufferSize;
    /** reference to the clock to store the timer object that calls flush() every 10 seconds */
    private Clock clock;
    /** in order to synchronize access to the buffer this object is used */
    private Object synchron;
    /** three variables that affect some behaviour of the sending of the data 
     * MINIMUMSENDSIZE is the minumum size of data that will be send immediately - otherwise the data will be buffered
     * SIZEOFFLUSH is the number of bytes the buffer must reach before the data is send - the data is also send if 10 seconds passed
     * whatever happens first
     * FLUSHTIME the interval time measured in ticks at which flush() is called
     */
    private final static int MINIMUMSENDSIZE = 40;
    private final static int SIZETOFLUSH = 250;
    private final static int FLUSHTIME = 1000; 
    
    /**
     * class constructor initializes the internal structures and the buffer
     *
     * @param tcp the tcplayer used by the socket this stream belongs to
     * @param sock the socket this stream belongs to
     */
    public MyOutputStream(TCP tcp, Socket sock, TimerManager timerManager) {
	tcplayer = tcp;
	socket = sock;
	pos = 0;
	maxDataSize = tcplayer.maxDataSize();
	bufferSize = maxDataSize;
	tempBuffer = new byte[bufferSize];
	synchron = new Object();
	// add a timer to call flush() every 10 seconds
	timerManager.addMillisTimer(FLUSHTIME, new Flusher(timerManager), new FlushArg(timerManager, this, FLUSHTIME));
    }
    
    /** just for the interface - in this implementation this method does nothing */
    public void close() throws IOException {return;} 
    
    /** this method sends all the data that is buffered in the internal buffer 
     * data is buffered if it is too small to be send immediately
     * @exeception IOException if an internal error occured - currently not used
     */
    public void flush() throws IOException {
	if (pos == 0)
	    return;
	synchronized (synchron) {
	    byte[] sendHelper = new byte[pos];
	    System.arraycopy(tempBuffer, 0, sendHelper, 0, pos);
	    pos = 0;
	    tcplayer.send(socket, sendHelper); 
	}
	return;
    }
    
    /**
     * write the data out of buf to the stream
     *
     * @param buf the byte array containing the data to be written
     *
     * @exeception IOException is thrown if no connection is established
     */
    public void write(byte[] buf) throws IOException {
	// if the connection is not already established, we cannot send
	if (socket.getState() != State.ESTAB) {
	    System.out.println("write ohne TCP-Verbindung!!");
	    throw new IOException();
	}
	if (socket.connectionAborted()) {
	    System.out.println("Connection abort: " + socket.connectionAbortReason());
	    // force sending of buffered data
	    flush();
	    throw new IOException();
	}
	// we try to avoid sending segments with too little data or we end up sending segments with one byte or something similar
	// if the users wants to have that little data to be send immediatly, he is advised to call flush afterwarrds
	if (buf.length < MINIMUMSENDSIZE) {
	    // if there is still space in our buffer we copy the data into the buffer
	    synchronized (synchron) {
		if ((pos + buf.length) < bufferSize) {
		    System.arraycopy(buf, 0, tempBuffer, pos, buf.length);
		    pos += buf.length;
		}
		else {
		    // if the buffer is to filled up we first send the data in the buffer - then we copy the new data into the buffer
		    flush();
		    System.arraycopy(buf, 0, tempBuffer, pos, buf.length);
		}
	    }
	}
	// the amount of data need not be buffered as it above our send limit 
	else {
	    // if the data fits into one segment we send it 
	    if (buf.length <= maxDataSize)
		tcplayer.send(socket, buf);
	    // otherwise the data needs to be send by more then one segment 
	    // we have to cut the data in approbiate pieces, copy them in helpbuffers and then send then
	    else {
		int dataLength = buf.length;
		int poss = 0;
		byte[] sendHelper = new byte[maxDataSize];
		byte[] sendLastHelper;
		for ( ;dataLength > 0; ) {
		    // send one maximum segment
		    if (dataLength >= maxDataSize) {
			System.arraycopy(buf, poss, sendHelper, 0, maxDataSize);
			tcplayer.send(socket, sendHelper);
			poss += maxDataSize;
			dataLength -= maxDataSize;
		    }
		    // the last piece is smaller then the maximum segment size
		    else {
			sendLastHelper = new byte[dataLength];
			System.arraycopy(buf, poss, sendLastHelper, 0, dataLength);
			tcplayer.send(socket, sendLastHelper);
			poss += dataLength;
			dataLength = 0;
		    }
		}
	    }
	}
	// if the buffer for small data segments has reached a reasonable size, we send the data
	    if (pos > SIZETOFLUSH)
		flush();
	return;
    }
    

    /**
     * write the data out of buf starting at a offset to the stream
     *  
     * @param buf the byte array containing the data to be written
     * @param off offset where the write should start at
     * @param len amount of data that should be written
     *
     * @exeception IOException is thrown if no connection is established
     */
    public void write(byte[] buf, int off, int len) throws IOException {
	if (socket.getState() != State.ESTAB) {
	    System.out.println("write ohne TCP-Verbindung!!");
	    throw new IOException();
	}
	if (socket.connectionAborted()) {
	    System.out.println("Connection abort: " + socket.connectionAbortReason());
	    // force sending of buffered data
	    flush();
	    throw new IOException();
	}
	if (len < MINIMUMSENDSIZE) {
	    // if there is still space in our buffer we copy the data into the buffer
	    synchronized (synchron) {
		if ((pos + len) < bufferSize) {
		    System.arraycopy(buf, off, tempBuffer, pos, len);
		    pos += len;
		}
		else {
		    // if the buffer is to filled up we first send the data in the buffer - then we copy the new data into the buffer
		    flush();
		    System.arraycopy(buf, off, tempBuffer, pos, len);
		}
	    }
	}	
	// the amount of data need not be buffered as it is above our send limit 
	else {
	    // if the data fits into one segment we send it 
	    if (len <= maxDataSize) {
		byte[] sendHelper = new byte[len];
		System.arraycopy(buf, off, sendHelper, 0, len);
		tcplayer.send(socket, sendHelper);
	    }
	    // otherwise the data needs to be send by more then one segment 
	    // we have to cut the data in approbiate pieces, copy them in helpbuffers and then send then
	    else {
		int dataLength = len;
		int poss = off;
		byte[] sendHelper = new byte[maxDataSize];
		byte[] sendLastHelper;
		for ( ;dataLength > 0; ) {
		    // send one maximum segment
		    if (dataLength >= maxDataSize) {
			System.arraycopy(buf, pos, sendHelper, 0, maxDataSize);
			tcplayer.send(socket, sendHelper);
			pos += maxDataSize;
			dataLength -= maxDataSize;
		    }
		    // the last piece is smaller then the maximum segment size
		    else {
			sendLastHelper = new byte[dataLength];
			System.arraycopy(buf, poss, sendLastHelper, 0, dataLength);
			tcplayer.send(socket, sendLastHelper);
			poss += dataLength;
			dataLength = 0;
		    }
		}
	    }
	}
	// if the buffer for small data segments has reached a reasonable size, we send the data
	if (pos > SIZETOFLUSH)
	    flush();
	return;
    }
    
    /**
     * write one value to the stream
     *  
     * @param value an integer value written to the stream as a byte value
     *
     * @exeception IOException is thrown if no connection is established
     */
    public void write(int value) throws IOException {
	if (socket.getState() != State.ESTAB) {
	    System.out.println("write ohne TCP-Verbindung!!");
	    throw new IOException();
	} 
	if (socket.connectionAborted()) {
	    System.out.println("Connection abort: " + socket.connectionAbortReason());
	    // force sending of buffered data
	    flush();
	    throw new IOException();
	}
	synchronized (synchron) {
	    if ((pos + 1) < bufferSize) {
		tempBuffer[pos++] = (byte)value;
	    }
	    else {
		// if the buffer is to filled up we first send the data in the buffer - then we copy the new data into the buffer
		flush();
		tempBuffer[pos++] = (byte)value;
	    }
	}
	// if the buffer for small data segments has reached a reasonable size, we send the data
	if (pos > SIZETOFLUSH)
	    flush();
	
	return;
    }
}
