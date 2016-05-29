package jx.net.protocol.icmp;

import jx.zero.*;

import jx.net.AddressResolution;
import jx.net.UnknownAddressException;
import jx.buffer.separator.MemoryConsumer;
import jx.net.PacketsProducer;
import jx.net.PacketsConsumer;

public class ICMP implements MemoryConsumer {
  
    private PacketsConsumer lowerConsumer;
    private PacketsProducer lowerProducer;

    static final boolean dumpAll = true; // switch on to see all icmp frames

    private static final int TYPE_ECHO = 8;

    public ICMP(PacketsConsumer lowerConsumer, PacketsProducer lowerProducer) {
	this.lowerProducer = lowerProducer;
	this.lowerConsumer = lowerConsumer; 
	lowerProducer.registerConsumer(this, "ICMP");
    }

    /*
    public boolean register(Object ip) {
	if (ipLayer != null)
	    return false;
	ipLayer = (IP)ip;
	return true;
    }
    */

    public Memory processMemory(Memory buf) {
	Memory mem = buf;
	Debug.out.println("ICMP packet received: offset=");
	ICMPFormat icmp = new ICMPFormat(buf);

	int type = icmp.getType();
	int code = icmp.getCode();
	int checksum = icmp.getChecksum();
	Debug.out.println("ICMP type: "+type+", code="+code+", checksum="+checksum);
	if (type == TYPE_ECHO) {
	    Debug.out.println("  ICMP ECHO RECEIVED. (ping)");
	}
	return buf;
    }
}







