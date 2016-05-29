package jx.devices.net.emulation;


import jx.devices.DeviceFinder;
import jx.devices.Device;
import jx.zero.*;

import java.util.*;
import jx.zero.*;
import jx.zero.debug.*;
import jx.devices.*;
import jx.devices.net.*;

import jx.buffer.separator.MemoryConsumer;
import jx.buffer.separator.*;

/**
 * Emulation NET Device finder
 * @author Michael Golm
 */
public class EmulNetFinder implements DeviceFinder {
    String name;
    String macaddr;
    public EmulNetFinder(String name, String macaddr) {
	this.name = name;
	this.macaddr = macaddr;
    }
    public Device[] find(String args[]) {
	NetEmulation net = (NetEmulation)InitialNaming.getInitialNaming().lookup("NetEmulation");
	if (net == null) return null;
	return new Device[] { new NetImpl(net, name, macaddr) };
    }
}

class NetImpl implements NetworkDevice {
    Naming naming;
    NetEmulation net;
    Memory buffer;
    MemoryManager memoryManager;
    NonBlockingMemoryConsumer consumer;

    NetImpl(NetEmulation net, String name, String macStr) {
	this.net = net;
	byte[] macaddr=new byte[6];
	char[] macarr = macStr.toCharArray();
	int j=0;
	for(int i=0; i<macarr.length; i++) {
	    int n=0;
	    while(i<macarr.length && macarr[i]!=':') {
		n <<= 4;
		char c = macarr[i];
		if (c >= 'A' &&  c <= 'F') n += c - 'A' + 10;
		else if (c >= 'a' &&  c <= 'f') n += c - 'a' + 10;
		else if (c >= '0' &&  c <= '9') n += c - '0';
		else throw new Error("Parse error. No etheraddr:"+macStr);
		i++;
	    }
	    macaddr[j++] = (byte)(n&0xff);
	} 
	net.open(name, macaddr);
	naming = InitialNaming.getInitialNaming();
	memoryManager = (MemoryManager)naming.lookup("MemoryManager");
	buffer = memoryManager.alloc(net.getMTU());
	new Thread("NetEmul-Eventloop") {
		public void run() {
		    eventloop();
		}
	    }.start();
    }
    private void eventloop() {
	int size;
	for(;;) {
	    if((size=(net.receive(buffer)))==0) Thread.yield();
	    else {
		if (consumer!=null) {
		    Memory newMem;
		    newMem = consumer.processMemory(buffer, 0, size); 
		    buffer = newMem.revoke();
		}
	    }
	}
    }


    public void setReceiveMode(int mode) {}

    public Memory transmit1(Memory buf, int offset, int size) {
	net.send(buf, offset, size);
	return buf;
    }

    public Memory transmit(Memory buf) {
	if (! buf.isValid()) {
	    throw new Error("NetworkEmulation: I got an invalid memory!");
	}
	net.send(buf, 0, buf.size());
	return buf;
    }
    public byte[] getMACAddress() { return net.getMACAddress();}
    public int getMTU() { return net.getMTU();}

    public boolean registerNonBlockingConsumer(NonBlockingMemoryConsumer consumer) {
	this.consumer = consumer;
	return true;
    }
    

    public void open(DeviceConfiguration conf){}
    public DeviceConfigurationTemplate[] getSupportedConfigurations (){
	return new DeviceConfigurationTemplate[] {new NetworkConfigurationTemplate() };
    }

    public void close(){}

}


