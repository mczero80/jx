package jx.emulation;

import java.io.*;
import jx.zero.*;
import jx.zero.debug.*;

class DebugChannelImpl implements DebugChannel {
    public void write(int b) {
	System.out.write(b);
    }
    public int read() {
	try {
	    return System.in.read();
	}catch(IOException ex) {
	}
	return -1;
    }
    public  void writeBuf(byte[] b, int off, int len) {
	for(int i=off; i<off+len; i++) {
	    write(b[i]);
	}
    }
}
