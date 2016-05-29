package jx.rexec;

import jx.zero.*;

public class Encoder {
    public static final int CMD_CREATEDOMAIN  = 1;
    public static final int CMD_DESTROYDOMAIN = 2;
    public static final int CMD_DUMPDOMAIN    = 3;
    public static final int CMD_DATA          = 4;
    public static final int CMD_INSTALLZIP    = 5;

    public static final int MAGIC    = 0xabc95464;

    public static final int REXEC_PORT = 6666;
    public static final int TRANSMISSION_OK = 0x1;


    static final boolean debugData = false;
    int pos;
    Memory m;
    int offset,len;
    byte []core;

	public Encoder(Memory m) { this.m = m; }
	public Encoder(Memory m, int offset, int len) {
	    this.m = m; 
	    this.offset = offset;
	    this.len = len;
	}
	public Encoder(byte [] b) { this.core = b; }

	public void writeInt(int d) { 
	    if (m!=null) m.setLittleEndian32(pos, d);
	    else setLittleEndian32(pos, d);
	    pos += 4;
	}
	public void writeString(String s) {
	    int len = s.length();
	    writeInt(len);
	    byte[] c = s.getBytes();
	    for(int i=0; i<len; i++) {
		if (m!=null) m.set8(pos+i, (byte)c[i]);
		else core[pos+i] = (byte)c[i];
	    }
	    pos += len;
	}
	public void writeData(Memory m) {
	    int size = m.size();
	    writeInt(size);
	    m.copyToMemory(this.m, 0, pos, size);
	    pos += size;
	}

	public void writeData(byte[] m) {
	    writeData(m, 0, m.length);
	}

	public void writeData(byte[] m, int offset, int len) {
	    int size = len;
	    writeInt(size);
	    if (this.m!=null) this.m.copyFromByteArray(m, offset, pos, size);
	    else {
		for(int i=0; i<size; i++) {
		    core[pos+i] = m[offset+i];
		}
	    }
	    pos += size;
	}

    public int getLength() {
	return pos;
    }

    private void setLittleEndian32(int where, int what)  {
	core[where   +  3 ] = (byte)((what >>> 24) & 0xff); 
	core[(where) + 2] = (byte)((what >>> 16) & 0xff); 
	core[(where) + 1] = (byte)((what >>> 8) & 0xff); 
	core[(where) + 0] = (byte)(what & 0xff); 
    }

}
