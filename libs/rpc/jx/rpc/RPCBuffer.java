package jx.rpc;

import jx.zero.*;
import jx.zero.debug.Dump;
import jx.xdr.*;

import jx.net.IPAddress;
import jx.net.UDPData;

public class RPCBuffer implements XDRBuffer {
    Memory buf;
    RPCContinuation cont;
    int offset,size;
    IPAddress sourceAddress;
    int sourcePort;

    public RPCBuffer(Memory buf) {
	init(buf);
    }

    public RPCBuffer(Memory buf, int offset) {
	init(buf, offset);
    }

  public RPCBuffer(UDPData buf) {
      init(buf);      
  }
  public RPCBuffer(UDPData buf, int offset, int size) {
      init(buf,offset,size);      
  }

  public RPCBuffer(Memory buf, RPCContinuation cont) {
    this.buf = buf;
    this.cont = cont;
  }

  public void copyInto(Memory m) {
      for(int i=0; i<offset; i++) {
	  m.set8(i, buf.get8(i));
      }
  }

  public void copyInto(Memory m, int memoffset) {
      for(int i=0; i<offset; i++) {
	  m.set8(i+memoffset, buf.get8(i));
      }
  }

  public void append(RPCBuffer b) {
    guaranteeSpace(b.offset);
    buf.copyFromMemory(b.buf, 0, offset, b.offset);    
    offset += b.offset;
  }


  public void append(byte[] b) {
    guaranteeSpace(b.length);
    buf.copyFromByteArray(b, 0, offset, b.length);    
    offset += b.length;
  }

  private void guaranteeSpace(int size) {
    if (offset+size > buf.size()) {
	throw new Error("RPC packet must be fragmented. This (or IP fragmentation) is not yet implemented.");
    }
  }

  public void xdump() {
      Debug.out.println("offset="+offset+", buf.size="+size);
      Dump.xdump(buf, offset, size);
  }
  public void dumpUnprocessed() {
    Dump.xdump(buf, offset, buf.size()-offset);
  }
  public void xdump(int len) {
    Dump.xdump(buf, offset, len);
  }

    final public void setByte(int index, byte value) {
	buf.set8(offset+index, value);
    }
    final public byte getByte(int index) {
	//Debug.out.println("getbyte at "+(offset+index)+ "offset="+offset);
	return buf.get8(offset+index);
    }
    final public void advance(int offset) {
	this.offset += offset;
    }

    final public int size() { return offset; }

    final public IPAddress getSourceAddress() {
	return sourceAddress;
    }

    final public int getSourcePort() {
	return sourcePort;
    }

    final Memory getMemory() { return buf;}

    void init(UDPData buf) {
	this.buf = buf.mem;
	//init();
	cont = null;
	offset=0;
	sourceAddress = buf.sourceAddress;
	sourcePort = buf.sourcePort;
    }
    void init(UDPData buf, int offset, int size) {
	this.buf = buf.mem;
	cont = null;
	this.offset=buf.offset;
	this.size=buf.size;
	sourceAddress = buf.sourceAddress;
	sourcePort = buf.sourcePort;
    }
    void init(Memory buf) {
	this.buf = buf;
	cont = null;
	offset=0;
	sourceAddress = null;
	sourcePort = 0;
    }
    void init(Memory buf, int offset) {
	this.buf = buf;
	cont = null;
	this.offset=offset;
	sourceAddress = null;
	sourcePort = 0;
    }
    /* uses join/split
    public void init() {
	//this.buf = this.buf.joinAll();
	cont = null;
	offset=0;
	sourceAddress = null;
	sourcePort = 0;
    }
    */
    public void init() {
	//this.buf = this.buf.joinAll();
	cont = null;
	offset=14+20+8;
	sourceAddress = null;
	sourcePort = 0;
    }

    final public void getBytes(byte[] bbuf, int index, int len) {
	for(int i=0; i<len; i++) {
	     bbuf[i] = buf.get8(offset+index+i);
	}
    }
}
