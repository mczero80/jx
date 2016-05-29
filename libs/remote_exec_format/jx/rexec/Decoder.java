package jx.rexec;

import jx.zero.*;
import jx.zero.debug.Dump;

public class Decoder {
    static final boolean debugData = false;

	int pos;
	Memory m;
    byte []core;
    int len;
	static final boolean testChecksum = true;

	public Decoder(Memory m) { this.m = m; }
	public Decoder(Memory m, int offset, int len) {
	    this.m = m; 
	    this.pos = offset;
	    this.len = len;
	}
	public Decoder(byte[] m, int offset, int len) {
	    this.core = m; 
	    this.pos = offset;
	    this.len = len;
	}

	public int readInt() { 
	    int ret;
	    if (m!=null) ret = m.getLittleEndian32(pos);
	    else ret = getLittleEndian32(pos);
	    pos += 4;
	    return ret;
	}
	public String readString() {
	    len = readInt();
	    char[] c = new char[len]; 
	    for(int i=0; i<len; i++) {
		if (m!=null) c[i] = (char)m.get8(pos+i);
		else c[i] = (char)core[pos+i];
	    }
	    pos += len;
	    return new String(c);
	}
	public Memory readData() {
	    //Dump.xdump1(m, 0, 128);
	    //Debug.out.println("**POS:"+ pos);
	    int size = readInt();
	    int checksum = readInt();
	    Debug.out.println("CHECKSUM:"+ checksum);
	    
	    //Debug.out.println("**SIZE:"+ size);
	    Memory ret = m.getSubRange(pos, size);
	    if (debugData) Dump.xdump1(ret, 0, size);
	    pos += size;

	    if (testChecksum) {
		int c=0;
		for(int i=0; i<size; i++) {
		    if (debugData) if (i%8==0) Debug.out.println("");
		    byte b = ret.get8(i);
		    c = (c ^ b) & 0xff;
		    if (debugData) Debug.out.print(c+" ");
		}
		if (debugData) Debug.out.println("MY CHECKSUM:"+ c);
		if (checksum != c) throw new Error("wrong checksum");
	    }

	    return ret;
	}

    public int getLittleEndian32(int where) { 
	return ((core[where + 3] << 24) & 0xff000000) 
	    |  ((core[where + 2] << 16) & 0x00ff0000) 
	    |  ((core[where + 1] <<  8) & 0x0000ff00) 
	    |   (core[where + 0] & 0xff); 
    }

    }
