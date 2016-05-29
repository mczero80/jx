package jx.net;

import jx.zero.*;
import jx.zero.debug.*;
import java.io.*;


/**
 * TCPOutputStream is returned by getOutputStream of class Socket
 * @author Stefan Winkler, Christian Fiermann
 */
public class TCPOutputStream extends OutputStream {
    private TCPSocket tcpSocket;

    private byte[] buffer;
    private int    buf_pos;

    public TCPOutputStream(TCPSocket tcpSocket) {
	this.buffer = new byte[1024];
	this.buf_pos = 0;
	this.tcpSocket = tcpSocket;
    }

    /**
     * abstract method of class OutputStream
     * Writes the specified byte to this output stream. 
     * The general contract for write is that one byte is written to the output stream. 
     * The byte to be written is the eight low-order bits of the argument b. 
     * The 24 high-order bits of b are ignored. 
     */
    public void write(int b) throws IOException {
	// byte[] data = new byte[1];
	// data[0]  = (byte) (b & 0xF);
	// tcpSocket.writeToOutputBuffer(data);

	buffer[buf_pos++] = (byte)b;
	if (buf_pos >= buffer.length) {
	    tcpSocket.send(buffer);
	    buf_pos=0;
	}
    }

    public void flush() throws IOException {
	int buf_size = buf_pos;
	byte[] sendBuf = new byte[buf_size];
	for (int i=0; i<buf_size; i++) {
	    sendBuf[i] = buffer[i];
	}
	tcpSocket.send(sendBuf);
	buf_pos=0;
    }

    public void close() {
	//	tcpSocket.close();
    }

}






