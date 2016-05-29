package jx.netmanager;

import jx.buffer.separator.MemoryConsumer;
import jx.zero.*;
import jx.zero.debug.*;

import jx.net.PacketsProducer;
import jx.net.PacketsConsumer;
import jx.net.PacketsConsumer1;
import jx.net.AddressResolution;
import jx.net.IPAddress;
import jx.net.UnknownAddressException;

import jx.net.protocol.ip.IP;

/**
 * UDP communication endpoint
 * @author Michael Golm
 */
class UDPSender implements jx.net.UDPSender, Service {
    PacketsConsumer udpSender;
    PacketsConsumer1 udpSender1;
    int localPort;
    IPAddress dst;
    int remotePort;

    UDPSender(NetInit net, int localPort, IPAddress dst, int remotePort) throws UnknownAddressException  {
	this.localPort = localPort;
	this.remotePort = remotePort;
	this.dst = dst;
	PacketsConsumer ipLayer;
	byte dest[] = net.arp.lookup(dst);
	PacketsConsumer etherLayer = net.ether.getTransmitter(dest, "IP");
	ipLayer = net.ip.getTransmitter(etherLayer, dst, "UDP");
	udpSender = net.udp.getTransmitter(ipLayer, localPort, remotePort);

	PacketsConsumer1 etherLayer1 = net.ether.getTransmitter1(dest, "IP");
	PacketsConsumer1 ipLayer1 = net.ip.getTransmitter1(etherLayer1, dst, IP.PROTO_UDP);
	udpSender1 = net.udp.getTransmitter1(ipLayer1, localPort, remotePort);
    }      

    public Memory send1(Memory m, int offset, int size) {
	return udpSender1.processMemory(m, offset, size);
    }
    public Memory send(Memory m) {
	if (1==1) throw new Error("temporarily disabled. use send1");
	return udpSender.processMemory(m);
    }

    public void close() {
    }

    public int getLocalPort() { return localPort; }
    public int getRemotePort() { return remotePort; }
    public IPAddress getDestination() { return dst; }

}
