package jx.compiler.execenv;

/** 
    A simple input stream to read the bytecode. 
    It is similar to java.io.ByteArrayInputStream, 
    but it has some additional methods needed in this case. 
*/ 

public final class BytecodeInputStream {

  private byte[] code; 
  private int numBytes; 
  private int pos;       // stream pointer 

  /** 
      @param code bytecode array
      @param numBytes size of the bytecode array
  */ 
  public BytecodeInputStream(byte[] code, int numBytes) {
    this.numBytes = numBytes; 
    this.code = code; 
    this.pos = 0; 
  }

  /** 
      @return the current position of the stream 
  */ 
  public int getCurrentPosition() { return pos;}

  /** 
      not really necessary, only used for exp.FastTranslator, 
  */
  public void setCurrentPosition(int newPos) {pos = newPos;}

  /** 
      @return true, if end of stream is reached 
  */
  public boolean endOfStream() {return pos >= numBytes;}

  /**
     @return true
  */
  public boolean hasMore() {return pos<numBytes;}
    
  /** 
      get the current byte without modifying the stream state
  */ 
  public final byte getCurrentByte() {
    return code[pos];
  }

  /** 
      get an unsigned byte that is 'offset' bytes after 
      the current position (result >= 0, always)
  */
  private final int getByteUnsigned(int offset) {
    return ((int)code[pos+offset]) & 0xff; 
  }

  /** 
      get an byte that is 'offset' bytes after 
      the current position
  */ 
  private final int getByte(int offset) {
    return (int)code[pos+offset]; 
  }

  /** 
      get an unsigned byte at current position 
      (result >= 0, always) 
  */ 
  public final int getCurrentByteUnsigned() {
    return ((int)code[pos]) & 0xff; 
  }

  /** 
      read byte and advance stream pointer
  */ 
  public final byte readByte() {
    return code[pos++];
  }

  /** 
      read unsigned byte and advance stream pointer
  */ 
  public final int readUnsignedByte() {
    return ((int)code[pos++]) & 0xff; 
  }

  /** 
      read signed short and advance stream pointer
  */ 
  public final short readShort() {
    int val = getByteUnsigned(1) + 
      (getByteUnsigned(0) << 8); 
    pos+=2; 
    return (short) val;
  }
  
  /** 
      read unsigned short and advanve stream pointer
  */
  public final int readUnsignedShort() {
    return (int)readShort() & 0xffff; 
  }
  
  /** 
      read integer, advance stream pointer
  */ 
  public final int readInt() {
    int val = getByteUnsigned(3) + 
      (getByteUnsigned(2) << 8) + 
      (getByteUnsigned(1) << 16) +
      (getByteUnsigned(0) << 24);
    pos+=4; 
    return val;
  }
}
