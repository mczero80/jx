package jx.net.protocol.arp;

import jx.zero.*;

import jx.net.format.Format;
import jx.net.IPAddress;

class ARPFormat extends Format {
    
    /**
     * @param offset start of arp protocol
     * @param buf lower level, arp , higher level protocol data
     */
    ARPFormat(Memory buf, int offset) {
	super(buf, offset); 
    }
    
    void insertHardwareAddressSpace(short space) {
	writeShort(0, space);
    }
    void insertProtocolAddressSpace(short space) {
	writeShort(2, space);
    }
    void insertHardwareAddressLen(byte len) {
	writeByte(4, len);
    }
    void insertProtocolAddressLen(byte len) {
	writeByte(5, len);
    }
    void insertCommand(short command) {
	writeShort(6, command);
    }
    void insertSenderHardwareAddress(byte[] address) {
	writeBytes(8, address);
    }
    void insertSenderProtocolAddress(IPAddress address) {
	writeAddress(14, address);
    }
    void insertTargetHardwareAddress(byte[] address) {
	writeBytes(18, address);
    }
    void insertTargetProtocolAddress(byte[] address) {
	writeBytes(24, address);
    }


    short getHardwareAddressSpace() {
	return (short)readShort(0);
    }
    short getProtocolAddressSpace() {
	return (short)readShort(2);
    }
    byte insertHardwareAddressLen() {
	return (byte)readByte(4);
    }
    byte getProtocolAddressLen() {
	return (byte)readByte(5);
    }
    short getCommand() {
	return (short)readShort(6);
    }
    byte[] getSenderHardwareAddress() {
	byte[] d = new byte[6]; 
	readBytes(8,d); 
	return d;
    }
    byte[] getSenderProtocolAddress() {
	byte[] d = new byte[4]; 
	readBytes(14,d); 
	return d;
    }
    byte[] getTargetHardwareAddress() {
	byte[] d = new byte[6]; 
	readBytes(18,d); 
	return d;
    }
    IPAddress getTargetProtocolAddress() {
	return readAddress(24); 
    }
    public int length() {
	return requiresSpace();
    }
    public static int requiresSpace() {
	return 28;
    }

    public void dump() {
	Debug.out.println("ARP-Packet:");
	Debug.out.println("   Command: "+getCommand());
	Debug.out.println("   HardwareAddressSpace: "+getHardwareAddressSpace());
	Debug.out.println("   ProtocolAddressSpace: "+getProtocolAddressSpace());
    }
}
