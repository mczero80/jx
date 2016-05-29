package jx.xdr;

import jx.zero.Debug;

public abstract class Format {
    final static boolean debug = false;

    protected byte[] buf;
    protected int offset;
    protected Format(byte[] buf, int offset) { init(buf, offset); }
    protected Format() {}

    protected void init(byte[] buf, int offset) { this.buf = buf; this.offset = offset; }

    public abstract int length();

    /*

      protected void writeInt(int o, int d) { 
      buf[offset+o+0] = (byte)( (d>>24) & 0xff);
      buf[offset+o+1] = (byte)((d>>16)  & 0xff);
      buf[offset+o+2] = (byte)((d>>8) & 0xff);
      buf[offset+o+3] = (byte)(d & 0xff);
      }


      protected void writeShort(int o, short d) { 
      buf[offset+o+0] = (byte)((d>>8) & 0xff);
      buf[offset+o+1] = (byte)(d & 0xff);
      }

      protected void writeUShort(int o, short d) { 
      writeShort(o, d); 
      }

      protected void writeByte(int o, byte d) { 
      buf[offset+o] = d;
      }

      protected void writeBytes(int o, byte[] d) { 
      for(int i=0; i<d.length; i++) {
      buf[offset+o+i] = d[i];
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
  


      protected int readByte(int o) { 
      return buf[offset+o] < 0 ? buf[offset+o]+256 : buf[offset+o];
      }

      protected void readBytes(int o, byte[] d) { 
      for(int i=0; i<d.length; i++) {
      d[i] = buf[offset+o+i];
      }
      }

      protected int readShort(int o) { 
      int d;
      d = readByte(o+1);
      d += readByte(o) << 8;
      return d;
      }

      protected static int readByte(byte[] buf, int o) { 
      return buf[o] < 0 ? buf[o]+256 : buf[o];
      }

      protected static int readShort(byte[] buf, int o) { 
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

      protected static int readInt(byte[] buf, int o) { 
      long d;
      d = readByte(buf, o+3);
      d += readByte(buf, o+2) << 8;
      d += readByte(buf, o+1) << 16;
      d += readByte(buf, o) << 24;
      return (int) d;
      }
    */
    
    public static void writeInt(XDRBuffer buf,int d) { 
	buf.setByte(0, (byte)((d>>24) & 0xff));
	buf.setByte(1, (byte)((d>>16) & 0xff));
	buf.setByte(2, (byte)((d>>8)  & 0xff));
	buf.setByte(3, (byte)( d      & 0xff));
	buf.advance(4);
    }

    public static void writeShort(XDRBuffer buf, int d) { 
	buf.setByte(0,  (byte)((d>>8) & 0xff));
	buf.setByte(1,  (byte)(d & 0xff));
	buf.advance(2);
    }

    public static void writeString(XDRBuffer buf, String s) { 
	byte [] b = s.getBytes();
	writeByteArray(buf, b);
	int l = s.length();
	if (l%4 != 0) buf.advance(4-(l%4)); //padding
    }

    public static int readInt(XDRBuffer buf) { 
	int d = buf.getByte(3) & 0xff;
	d |= (buf.getByte(2) & 0xff) << 8;
	d |= (buf.getByte(1) & 0xff) << 16;
	d |= (buf.getByte(0) & 0xff) << 24;
	if (debug) Debug.out.println("Int: "+d+" bytes: "+buf.getByte(0)+" "+buf.getByte(1)+" "+buf.getByte(2)+" "+buf.getByte(3)+" ");
	buf.advance(4);
	return d;
    }
    
    public static short readShort(XDRBuffer buf) { 
	int d;
	d = readByte(buf, 1);
	d += readByte(buf, 0) << 8;
	buf.advance(2);
	if (debug) Debug.out.println("Short: "+d);
	return (short) d;
    }


    public static int readByte(XDRBuffer buf) { 
	int d = readByte(buf, 0);
	buf.advance(1);
	if (debug) Debug.out.println("Byte: "+d);
	return  d;
    }

    public static void writeByteArray(XDRBuffer buf, byte[] d) { 
	writeInt(buf, d.length);
	for(int i=0; i<d.length; i++) {
	    buf.setByte(i, d[i]);
	}
	buf.advance(d.length);
    }

    public static void writeFixByteArray(XDRBuffer buf, byte[] d, int l) { 
	if (d.length != l) {
	    Debug.out.println("error: fix byte len != array length");
	    System.exit(1);
	}
	for(int i=0; i<l; i++) {
	    buf.setByte(i, d[i]);
	}
	buf.advance(l);
    }

    public static void writeIntArray(XDRBuffer buf, int[] d) { 
	writeInt(buf, d.length);
	for(int i=0; i<d.length; i++) {
	    writeInt(buf, d[i]);
	}
    }
    public static void writeShortArray(XDRBuffer buf, short[] d) { 
	writeInt(buf, d.length);
	for(int i=0; i<d.length; i++) {
	    writeShort(buf, d[i]);
	}
    }

    public static boolean readBoolean(XDRBuffer buf) { 
	int i= readInt(buf);
	return (i!=0);
    }

    public static int[] readIntArray(XDRBuffer buf) { 
	int l = readInt(buf);
	//Debug.out.println("IntArray: "+l);
	int[] b = new int[l];
	for(int i=0; i<l; i++) {
	    b[i]=readInt(buf);
	}
	return b;
    }
    public static short[] readShortArray(XDRBuffer buf) { 
	int l = readInt(buf);
	short[] b = new short[l];
	for(int i=0; i<l; i++) {
	    b[i]=readShort(buf);
	}
	return b;
    }

    public static byte[] readByteArray(XDRBuffer buf) { 
	int l = readInt(buf);
	if (debug) Debug.out.println("Bytearraylen: "+l);
	byte[] b = new byte[l];
	for(int i=0; i<l; i++) {
	    b[i] = buf.getByte(i);
	}
	buf.advance(l);
	return b;
    }

    public static byte[] readFixByteArray(XDRBuffer buf, int l) { 
	byte[] b = new byte[l];
	for(int i=0; i<l; i++) {
	    b[i]=(byte)readByte(buf);
	}
	return b;
    }


    public static String readString(XDRBuffer buf)  { 
	int l = readInt(buf);
	if (debug) Debug.out.println("Stringlen: "+l);
	if (l > 1024) throw new Error("String too large");
	//	for (;4 + l > buf.buf.length;) {
	    /*
	    buf.cont.rpc.waitForNextReply(buf.cont);
	    int bytesInOldBuf = buf.buf.length-buf.offset-4;
	    buf.buf = new byte[bytesInOldBuf + buf.cont.buf.length];
	    System.arraycopy(buf.buf, buf.offset, buf.buf, 0, bytesInOldBuf);
	    System.arraycopy(buf.cont.buf, 0, buf.buf, bytesInOldBuf, buf.cont.buf.length);
	    */
	//  throw new Error("not implemented");
	//}
	byte[] b = new byte[l];
	for(int i=0; i<l; i++) {
	    b[i]=(byte)readByte(buf, i);
	}
	// TODO: buf.getBytes(b, i, l);
	buf.advance(l);
	/*	if (l%4 != 0) //buf.offset += 4-(l%4); //padding
	    for(int j=0; j<4-(l%4);j++) readByte(buf);
	*/
	buf.advance((4-(l%4))%4);  // ??? nicht besser (l + 3) & ~3
	String s = new String(b);
	if (debug) Debug.out.println("String: "+s);
	return s;
    }
    

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

    protected static int readByte(XDRBuffer  buf, int o) { 
	int x = buf.getByte(o);
	return x < 0 ? x+256 : x;
    }
  
}
