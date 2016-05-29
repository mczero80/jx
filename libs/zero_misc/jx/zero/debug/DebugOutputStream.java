package jx.zero.debug;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import jx.zero.debug.DebugChannel;


public class DebugOutputStream extends OutputStream {
  DebugChannel debugChannel;
  
  public DebugOutputStream(DebugChannel debugChannel) {
      this.debugChannel = debugChannel;
      /*debugChannel.write('X');*/
  }

  public void write(int b) throws IOException {
    debugChannel.write(b);
  }
    /**
     * atomically write this buffer
     */
  public void write(byte[] b, int off, int len) throws IOException {
    debugChannel.writeBuf(b,off,len);
  }
}
