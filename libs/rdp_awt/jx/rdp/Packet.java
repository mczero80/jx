package jx.rdp;

import jx.zero.*;
import java.net.*;

public class Packet {

    private Memory memory=null;
    private int position=0;
    private int mcs = -1;
    private int secure = -1;
    private int rdp = -1;
    private int size=0;
    private int start = -1;
    private int end=-1;

    
    /* constants for Packet */
    public static final int MCS_HEADER = 1;
    public static final int SECURE_HEADER = 2;
    public static final int RDP_HEADER = 3;
    
    public Packet(Memory data) {
	this.memory=data;
	size=this.memory.size();
    }
       
    public void copy(int from, int to, int length) {
	if ((from+length > size) || (to+length > size)) { 
	    throw new ArrayIndexOutOfBoundsException("memory accessed out of Range!");
	}
	memory.copy(from, to, length);
    }

    public void set8(int where, int what) {
	if (where < 0 || where >= size) { 
	    throw new ArrayIndexOutOfBoundsException("memory accessed out of Range!"); 
	}

	memory.set8(where, (byte)what);
    }

    public void set8(int what) {
	if (position >= size) { 
	    throw new ArrayIndexOutOfBoundsException("memory accessed out of Range!"); 
	}
	memory.set8(position, (byte)what);
	position++;
    }
   
    // where is a 8-bit offset
    public int get8(int where) {
	if (where < 0 || where >= size) { 
	    throw new ArrayIndexOutOfBoundsException("memory accessed out of Range!"); 
	}
	return memory.get8(where)&0x000000ff;
    }

    // where is a 8-bit offset
    public int get8() {
	if (position >= size) { 
	    throw new ArrayIndexOutOfBoundsException("memory accessed out of Range!"); 
	}
	return memory.get8(position++)&0x000000ff;
    }

    public void copyFromByteArray(byte[] array, int array_offset, int mem_offset, int len){
	if ((array_offset >= array.length) || (array_offset + len > array.length) || (mem_offset >= this.size) || (mem_offset + len > this.size)) {
	    throw new ArrayIndexOutOfBoundsException("memory accessed out of Range!");
	}
	memory.copyFromByteArray(array, array_offset, mem_offset, len);
    }
    
    public void copyToByteArray(byte[] array, int array_offset, int mem_offset, int len) {
	if ((array_offset >= array.length) || (array_offset + len > array.length) || (mem_offset >= this.size) || (mem_offset + len > this.size)) {
	    throw new ArrayIndexOutOfBoundsException("memory accessed out of Range!");
	}
	memory.copyToByteArray(array, array_offset, mem_offset, len);
    }

    public void copyToPacket(Packet dst, int srcOffset, int dstOffset, int len) {
	Memory destination=dst.getMemoryObject();
	if (memory.copyToMemory(destination, srcOffset, dstOffset, len) !=0){
	    throw new ArrayIndexOutOfBoundsException();
	}
    }

    public void copyFromPacket(Packet src, int srcOffset, int dstOffset, int len) {
	Memory source =src.getMemoryObject();
	if ( memory.copyFromMemory(source, srcOffset, dstOffset, len) !=0){
	    throw new ArrayIndexOutOfBoundsException();
	}
    }

    // return size in bytes
    public int size() {
	return this.size;
    }
   
    public int getPosition() {
	return this.position;
    }

    public int getLittleEndian16(int where) { 
	return memory.getLittleEndian16(where)&0x0000ffff;
     }

    public int getLittleEndian16() { 
	int data = memory.getLittleEndian16(position)&0x0000ffff;
	position+=2;
	return data;
     }

    public int getBigEndian16(int where) { 
	return memory.getBigEndian16(where)&0x0000ffff;
    }
    
    public int getBigEndian16() { 
	int data = memory.getBigEndian16(position)&0x0000ffff;
	position+=2;
	return data;
    }
    
    public void setLittleEndian16(int where, int what)  {
	memory.setLittleEndian16(where, (short)what);
    }
    
    public void setLittleEndian16(int what)  {
	memory.setLittleEndian16(position, (short)what);
	position+=2;
    }
    
    public void setBigEndian16(int where, int what)  {
	memory.setBigEndian16(where, (short)what);
    }
    
    public void setBigEndian16(int what)  {
	memory.setBigEndian16(position, (short)what);
	position+=2;
    }

    public int getLittleEndian32(int where) { 
	return this.memory.getLittleEndian32(where);
    }

    public int getLittleEndian32() { 
        int data = this.memory.getLittleEndian32(position);
	position+=4;
	return data;
    }

     public int getBigEndian32(int where) { 
	 return memory.getBigEndian32(where);
    }

     public int getBigEndian32() { 
	 int data = memory.getBigEndian32(position);
	 position+=4;
	 return data;
     }
	
    public void setLittleEndian32(int where, int what)  {
	memory.setLittleEndian32(where, what);
    }

    public void setLittleEndian32(int what)  {
	memory.setLittleEndian32(position, what);
	position+=4;
    }
	
    public void setBigEndian32(int where, int what)  {
	memory.setBigEndian32(where, what);
    }

    public void setBigEndian32(int what)  {
	memory.setBigEndian32(position, what);
	position+=4;
    }
    
    public Memory getSubRange(int start, int size) {
	if (start+size > this.size) {
	    throw new ArrayIndexOutOfBoundsException("Memory accessed out of Range!");
	}
	return memory.getSubRange(start, size);
    }
    
    public Memory getMemoryObject() {
	return this.memory;
    }

    public void incrementPosition(int length) {

	if (length > this.size || length+this.position > this.size || length <0) {
	    throw new ArrayIndexOutOfBoundsException();
	}
	this.position+=length;
    }

    public void setPosition(int position) {
	if (position > this.size || position <0) {
	    throw new ArrayIndexOutOfBoundsException();
	    }
	this.position=position;
    }
    
    public void markEnd() {
	this.end=this.position;
    }
    
    public void markEnd(int position) {
	if (position > this.size) {
	    throw new ArrayIndexOutOfBoundsException("Mark > size!");
	}
	this.end=position;
    }

    public int getEnd() {
	return this.end;
    }
    
    public Packet getRange(int start, int size) {
	Packet data = new Packet(this.memory.getSubRange(start, size));
	return data;
    }

    public int getHeader(int header) throws RdesktopException {
	switch(header) {
	case Packet.MCS_HEADER:
	    return this.mcs;
	case Packet.SECURE_HEADER:
	    return this.secure;
	case Packet.RDP_HEADER:
	    return this.rdp;
	default:
	    throw new RdesktopException("Wrong Header!");
	}
    }

    public void setHeader(int header) throws RdesktopException{
	switch(header) {
	case Packet.MCS_HEADER:
	    this.mcs = this.getPosition();
	    break;
	case Packet.SECURE_HEADER:
	    this.secure = this.getPosition();
	    break;
	case Packet.RDP_HEADER:
	    this.rdp = this.getPosition();
	    break;
	default:
	    throw new RdesktopException("Wrong Header!");
	}
    }

    public int getStart() {
	return this.start;
    }

    public void setStart(int position) {
	this.start=position;
    }
}
