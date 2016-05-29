package jx.net.protocol.ether;

import jx.devices.net.NetworkDevice;
import jx.zero.Debug;
import java.lang.*;
import jx.zero.ThreadEntry;
import jx.zero.*;
import jx.zero.debug.*;
import jx.devices.*;

import jx.net.format.Format;

/**
  * Write IP header data according to the IP format in the buffer.
  *
  *	char dest[6];
  *	char src[6];
  *	short type;
  *
  */

public class EtherFormat extends Format {

    EtherFormat(Memory buf) { 
	this(buf,0);
    }
    EtherFormat(Memory buf, int offset) { 
	super(buf, offset);
	//Debug.out.println("Ether-Packet:");
	//Dump.xdump1(buf, offset, 64);
    }
    /* write */
    void insertDestAddress(byte[] addr)   {  writeBytes(0, addr);}
    void insertSourceAddress(byte[] addr) {  writeBytes(6, addr);}
    void insertType(int t)     {  writeShort(12, (short)t);  }

    /* read */
    byte[] getDestAddress() { byte[] d = new byte[6]; readBytes(0,d); return d; }
    byte[] getSourceAddress() { byte[] d = new byte[6]; readBytes(6,d); return d; }
    int getType() { return readShort(12); }

    /* space consumption */
    public int length() { return requiresSpace(); }
    public static int requiresSpace() { return 14; }

    boolean destAddressEquals(byte[] addr) { 
	for(int i=0; i<6; i++) {
	    if(readByte(i) != addr[i]) {
		Debug.out.println("Ether.addrCompare: position "+i+"differs:"+readByte(i)+"!="+addr[i]);
		return false;
	    }
	}
	return true;
    }

    public void dump() {
	Debug.out.println("Ether-Packet:");
	Debug.out.print("  Source: ");
	Dump.xdump(getSourceAddress(), 6);
	Debug.out.print("  Dest: ");
	Dump.xdump(getDestAddress(), 6);
	Debug.out.println("  Type: "+getType());
	Debug.out.println("   First 32 bytes of data:");
	Dump.xdump(buf, offset+requiresSpace(), 32);
    }
}

