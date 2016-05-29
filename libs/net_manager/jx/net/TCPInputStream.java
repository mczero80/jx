package jx.net;

import jx.zero.*;
import jx.zero.debug.*;
import java.io.InputStream;
import java.io.IOException;


/**
 * TCPInputStream is returned by getInputStream of class Socket
 * @author Stefan Winkler
 */
public class TCPInputStream extends InputStream {
    private TCPSocket tcpSocket;
    private byte[] buffer;
    private int buf_pos;
    
    public TCPInputStream(TCPSocket tcpSocket) {
	this.tcpSocket = tcpSocket;
	this.buf_pos = -1;
    }

    /**
     * abstract method of class InputStream
     * Reads the next byte of data from the input stream.
     * The value byte is returned as an int in the range 0 to 255.
     * If no byte is available because the end of the stream has been reached,
     * the value -1 is returned. This method blocks until input data is available,
     * the end of the stream is detected, or an exception is thrown.
     */ 

    public int read() throws IOException {
	if ((buf_pos < 0) || (buf_pos >= buffer.length)) {
	    buffer =  tcpSocket.readFromInputBuffer();
	    buf_pos = 0;
	}
	return buffer[buf_pos++]&0x000000ff;
    }


    /*
    public int available() {	
	if (buf_pos >= 0) return (buffer.length - buf_pos);
	else return tcpSocket.getInputBufferLength();
    }
    */
    
}
