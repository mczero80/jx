package jx.xdr;
import jx.zero.*;

//import metaxa.os.rpc.RPCBuffer;

public abstract class FormatMem {
  public static boolean debug = false;

  protected Memory buf;
  protected int offset;
  protected FormatMem(Memory buf, int offset) { init(buf, offset); }
  protected FormatMem() {}

  protected void init(Memory buf, int offset) { this.buf = buf; this.offset = offset; }

  public abstract int length();

  /* write */

  protected void writeInt(int o, int d) { 
    buf.set8(offset+o+0, (byte)( (d>>24) & 0xff));
    buf.set8(offset+o+1, (byte)((d>>16)  & 0xff));
    buf.set8(offset+o+2, (byte)((d>>8) & 0xff));
    buf.set8(offset+o+3, (byte)(d & 0xff));
  }


  protected void writeShort(int o, short d) { 
    buf.set8(offset+o+0, (byte)((d>>8) & 0xff));
    buf.set8(offset+o+1, (byte)(d & 0xff));
  }

  protected void writeUShort(int o, short d) { 
    writeShort(o, d); 
  }

  protected void writeByte(int o, byte d) { 
    buf.set8(offset+o, d);
  }

  protected void writeBytes(int o, byte[] d) { 
    for(int i=0; i<d.length; i++) {
      buf.set8(offset+o+i, d[i]);
    }
  }

  protected void writeString(int o, String s) { 
    byte [] b = s.getBytes();
    writeByteArray(o, b);
  }

  protected void writeIntArray(int o, int[] d) { 
    writeInt(o, d.length);
    for(int i=0; i<d.length; i++) {
      writeInt(o+4+i*4, d[i]);
    }
  }

  protected void writeByteArray(int o, byte[] d) { 
    writeInt(o, d.length);
    for(int i=0; i<d.length; i++) {
      writeByte(o+4+i, d[i]);
    }
  }
  

  /* read */

  protected byte readByte(int o) { 
      //    return buf.get8(offset+o) < 0 ? buf.get8(offset+o)+256 : buf.get8(offset+o);
      return buf.get8(offset+o);
  }

  protected void readBytes(int o, byte[] d) { 
    for(int i=0; i<d.length; i++) {
      d[i] = buf.get8(offset+o+i);
    }
  }

  protected short readShort(int o) { 
    short d;
    d = readByte(o+1);
    d |= readByte(o) << 8;
    return d;
  }

  protected static byte readByte(Memory buf, int o) { 
      //  return buf.get8(o) < 0 ? buf.get8(o)+256 : buf.get8(o);
      return buf.get8(o);
  }

  protected static int readShort(Memory buf, int o) { 
    int d;
    d = readByte(buf, o+1);
    d += readByte(buf, o) << 8;
    return d;
  }

   protected int readInt(int o) { 
    long d;
    d = readByte(o+3);
    d += readByte(o+2) << 8;
    d += readByte(o+1) << 16;
    d += readByte(o) << 24;
    return (int) d;
  }

   protected static int readInt(Memory buf, int o) { 
    long d;
    d = readByte(buf, o+3);
    d += readByte(buf, o+2) << 8;
    d += readByte(buf, o+1) << 16;
    d += readByte(buf, o) << 24;
    return (int) d;
  }

    /*
  public static void writeInt(RPCBuffer buf,int i) { 
    long d = i;
    buf.buf[buf.offset+0] = (byte)( (d>>24) & 0xff);
    buf.buf[buf.offset+1] = (byte)((d>>16)  & 0xff);
    buf.buf[buf.offset+2] = (byte)((d>>8) & 0xff);
    buf.buf[buf.offset+3] = (byte)(d & 0xff);
    buf.offset += 4;
  }

  public static void writeShort(RPCBuffer buf, int d) { 
    buf.buf[buf.offset+0] = (byte)((d>>8) & 0xff);
    buf.buf[buf.offset+1] = (byte)(d & 0xff);
    buf.offset += 2;
  }

  public static void writeString(RPCBuffer buf, String s) { 
    byte [] b = s.getBytes();
    writeByteArray(buf, b);
    int l = s.length();
    if (l%4 != 0) buf.offset += 4-(l%4); //padding
  }

   public static int readInt(RPCBuffer buf) { 
    long d;
    d = readByte(buf.buf, buf.offset+3);
    d += readByte(buf.buf, buf.offset+2) << 8;
    d += readByte(buf.buf, buf.offset+1) << 16;
    d += readByte(buf.buf, buf.offset) << 24;
    buf.offset += 4;
    if (debug) System.out.println("Int: "+d);
    return (int) d;
  }

   public static short readShort(RPCBuffer buf) { 
    int d;
    d = readByte(buf.buf, buf.offset+1);
    d += readByte(buf.buf, buf.offset) << 8;
    buf.offset += 2;
    if (debug) System.out.println("Short: "+d);
    return (short) d;
  }


   public static int readByte(RPCBuffer buf) { 
    int d = readByte(buf.buf, buf.offset);
    buf.offset++;
    if (debug) System.out.println("Byte: "+d);
    return  d;
  }

  public static void writeByteArray(RPCBuffer buf, byte[] d) { 
    writeInt(buf, d.length);
    for(int i=0; i<d.length; i++) {
      buf.buf[buf.offset+i] = d[i];
    }
    buf.offset += d.length;
  }

  public static void writeFixByteArray(RPCBuffer buf, byte[] d, int l) { 
    if (d.length != l) {
      System.out.println("error: fix byte len != array length");
      System.exit(1);
    }
    for(int i=0; i<l; i++) {
      buf.buf[buf.offset+i] = d[i];
    }
    buf.offset += l;
  }

  public static void writeIntArray(RPCBuffer buf, int[] d) { 
    writeInt(buf, d.length);
    for(int i=0; i<d.length; i++) {
      writeInt(buf, d[i]);
    }
  }
  public static void writeShortArray(RPCBuffer buf, short[] d) { 
    writeInt(buf, d.length);
    for(int i=0; i<d.length; i++) {
      writeShort(buf, d[i]);
    }
  }

   public static boolean readBoolean(RPCBuffer buf) { 
     int i= readInt(buf);
     return (i!=0);
  }

  public static int[] readIntArray(RPCBuffer buf) { 
    int l = readInt(buf);
     int[] b = new int[l];
     for(int i=0; i<l; i++) {
       b[i]=readInt(buf);
     }
     return b;
  }
  public static short[] readShortArray(RPCBuffer buf) { 
    int l = readInt(buf);
     short[] b = new short[l];
     for(int i=0; i<l; i++) {
       b[i]=readShort(buf);
     }
     return b;
  }

  public static byte[] readByteArray(RPCBuffer buf) { 
    int l = readInt(buf);
    byte[] b = new byte[l];
    for(int i=0; i<l; i++) {
      b[i]=(byte)readByte(buf);
    }
    return b;
  }

  public static byte[] readFixByteArray(RPCBuffer buf, int l) { 
    byte[] b = new byte[l];
    for(int i=0; i<l; i++) {
      b[i]=(byte)readByte(buf);
    }
    return b;
  }


   public static String readString(RPCBuffer buf)  { 
     if (buf.offset + 4 > buf.buf.length) {
       System.out.println("exit");
       System.exit(1);
     }
     int l = readInt(buf);
     for (;buf.offset + 4 + l > buf.buf.length;) {
       buf.cont.rpc.waitForNextReply(buf.cont);
       int bytesInOldBuf = buf.buf.length-buf.offset-4;
       buf.buf = new byte[bytesInOldBuf + buf.cont.buf.length];
       System.arraycopy(buf.buf, buf.offset, buf.buf, 0, bytesInOldBuf);
       System.arraycopy(buf.cont.buf, 0, buf.buf, bytesInOldBuf, buf.cont.buf.length);
     }
     byte[] b = new byte[l];
     for(int i=0; i<l; i++) {
       b[i]=(byte)readByte(buf.buf, buf.offset+i);
     }
     buf.offset += l;
     if (l%4 != 0) buf.offset += 4-(l%4); //padding
     String s = new String(b);
     if (debug) System.out.println("String: "+s);
     return s;
   }
    */

  /*
   * length methods 
   */
  public static int lengthByte(byte d)  { return 1; } 
  public static int lengthShort(short d)  { return 2; } 
  public static int lengthInt(int d)  { return 4; } 
  public static int lengthIntArray(int [] d)  { return d.length*4 + 4; } 
  public static int lengthShortArray(short [] d)  { return d.length*2 + 4; } 
  public static int lengthByteArray(byte [] d)  { return d.length + 4; } 
  public static int lengthFixByteArray(byte [] d, int l)  { return l; } 
  public static int lengthString(String s)  {
    int o = 4;
    int l = s.length(); 
    o+=l;
    if (l%4 != 0) o += 4-(l%4); //padding
    return o;
  } 
  
}
