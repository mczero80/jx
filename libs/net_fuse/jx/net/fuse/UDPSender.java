package jx.net.fuse;

import jx.buffer.separator.MemoryConsumer;
import jx.zero.*;
import jx.zero.debug.*;

import jx.net.PacketsProducer;
import jx.net.PacketsConsumer;
import jx.net.AddressResolution;
import jx.net.IPAddress;
import jx.net.UnknownAddressException;

/**
 * UDP communication endpoint
 * @author Michael Golm
 */
class UDPSender implements jx.net.UDPSender, DEPImpl {
    PacketsConsumer ipLayer;
    PacketsConsumer udpSender;
    int localPort;
    IPAddress dst;
    int remotePort;

    UDPSender(NetInit net, int localPort, IPAddress dst, int remotePort) throws UnknownAddressException  {
	this.localPort = localPort;
	this.remotePort = remotePort;
	this.dst = dst;
	byte dest[] = net.arp.lookup(dst);
	PacketsConsumer etherLayer = net.ether.getTransmitter(dest, "IP");
	ipLayer = net.ip.getTransmitter(etherLayer, dst, "UDP");
	udpSender = net.udp.getTransmitter(ipLayer, localPort, remotePort);
    }      

    public Memory send(Memory m) {
	/*
	int offset = m.getOffset();
	int size = m.size();
	Memory m0 = m.extendAndRevoke();
	Memory m1 = m0.getSubRange(offset, size);
	return udpSender.processMemory(m1);
	*/
	return udpSender.processMemory(m.revoke());
    }

    public void close() {
    }

    public int getLocalPort() { return localPort; }
    public int getRemotePort() { return remotePort; }
    public IPAddress getDestination() { return dst; }

}
