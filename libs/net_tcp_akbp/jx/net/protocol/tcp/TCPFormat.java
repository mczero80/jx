package jx.net.protocol.tcp;

import jx.zero.*;
import jx.zero.debug.*;
import jx.net.*;
import jx.net.format.Format;
import jx.net.IPData;

/**
 * Write/read TCP header data according to the TCP format.
 * Validate TCP fragment.
 * @author Stefan Winkler
 */
public class TCPFormat extends Format {
    private static final byte HEADER_LENGTH_WITHOUT_OPTIONS = 5;
    private static final byte TCP_PROTOCOL = 6;
    private static final byte SOURCE_PORT_OFFSET = 0;
    private static final byte DESTINATION_PORT_OFFSET = 2;
    private static final byte SEQUENCE_NUMBER_OFFSET = 4;
    private static final byte ACKNOWLEDGEMENT_NUMBER_OFFSET = 8;
    private static final byte HEADER_LENGTH_OFFSET = 12;
    private static final byte FLAGS_OFFSET = 13;
    private static final byte WINDOW_SIZE_OFFSET = 14;
    private static final byte CHECKSUM_OFFSET = 16;
    private static final byte URGENT_POINTER_OFFSET = 18;
    private static final byte OPTION_OFFSET = 20;
    
    public static final byte URG = 0x00000020;
    public static final byte ACK = 0x00000010;
    public static final byte PSH = 0x00000008;
    public static final byte RST = 0x00000004;
    public static final byte SYN = 0x00000002;
    public static final byte FIN = 0x00000001;

    private boolean optionsSet = false;
    private IPAddress sourceAddress;
    private IPAddress destinationAddress;
    private int packetsize;


    public TCPFormat(Memory buf) {
	this(buf, null, null);

    }

    public TCPFormat(Memory buf, IPAddress sourceAddress, IPAddress destinationAddress) {
	super(buf);
	this.sourceAddress = sourceAddress;
	this.destinationAddress = destinationAddress;
	this.packetsize = buf.size();
	throw new Error("disabled, use no splitting methods");
   }


    public TCPFormat(IPData data) {
	this(data, null, null);

    }
    public TCPFormat(IPData data, IPAddress sourceAddress, IPAddress destinationAddress) {
	super(data.mem, data.offset);
	this.sourceAddress = sourceAddress;
	this.destinationAddress = destinationAddress;
	this.packetsize = data.size;
    }

    public void insertSourcePort(int sourcePort) {
	writeUShort(SOURCE_PORT_OFFSET, (short) sourcePort);
    }

    public void insertDestinationPort(int destinationPort) {
	writeUShort(DESTINATION_PORT_OFFSET, (short) destinationPort);
    }

    public void insertSequenceNumber(int sequenceNumber) {
	writeInt(SEQUENCE_NUMBER_OFFSET, sequenceNumber);
    }

    public void insertAcknowledgmentNumber(int acknowledgmentNumber) {
	writeInt(ACKNOWLEDGEMENT_NUMBER_OFFSET, acknowledgmentNumber);
    }

    public void computeHeaderLength() {
	if (optionsSet) {
	    //
	}
	else {
	    writeByte(HEADER_LENGTH_OFFSET, (byte) ((HEADER_LENGTH_WITHOUT_OPTIONS << 4) & 0xf0));
	}
    }
    
    // flags = {URG, ACK, PSH, RST, SYN, FIN}{ | {URG, ACK, PSH, RST, SYN, FIN}}*
    public void insertFlags(int flags) {
	writeByte(FLAGS_OFFSET, (byte) flags);
    }

    public void insertWindowSize(int windowSize) {
	writeUShort(WINDOW_SIZE_OFFSET, (short) windowSize);
    }

    public void insertChecksum() {
	// place a zero in the checksum field before computing the checksum
	writeUShort(CHECKSUM_OFFSET, (short) 0);
	short checksum = computeChecksum();
	writeUShort(CHECKSUM_OFFSET, (short) checksum);
    }

    private final short buildShort(byte a, byte b) {
	return (short) (((((int) a) & 0xff) << 8) | (((int) b) & 0xff));
    }
    
    // computes the tcp-checksum
    private final short computeChecksum() {
	CPUManager cpuManager = (CPUManager) InitialNaming.getInitialNaming().lookup("CPUManager");
	short sum = 0;
	int len = length();

	// checksum algorithm of tcp segment
	boolean flag = (len%2 == 1);
	
	if (flag) {
	    short value = (short) (readUnsignedByte(len - 1) << 8);
            sum = addUShort(sum, value);
	} 

	len >>>= 1;
	while (len-- > 0) {
	    short value = (short) (readUnsignedShort(len * 2) & 0xffff);
	    sum = addUShort(sum, value);
	}

	// checksum computation of the tcp/ip pseudo-header
	byte[] bytes = sourceAddress.getBytes();
	sum = addUShort(sum, buildShort(bytes[0], bytes[1]));
	sum = addUShort(sum, buildShort(bytes[2], bytes[3]));
	bytes = destinationAddress.getBytes();
	sum = addUShort(sum, buildShort(bytes[0], bytes[1]));
	sum = addUShort(sum, buildShort(bytes[2], bytes[3]));
	sum = addUShort(sum, (short) TCP_PROTOCOL);
	sum = addUShort(sum, buildShort( (byte) ((length() >> 8) & 0xff), (byte) (length() & 0xff)));

	int checksum = (~sum) & 0xffff;
	//Debug.out.println("computeChecksum: checksum = " + Integer.toHexString(checksum) );
	return (short) checksum;
    }

     private final short addUShort(short a, short b){
	int result = (((int) a)&0xffff) + (((int) b)&0xffff);
	if (result > (int)0x0000ffff) {
	    result -= 0x0000ffff;
	}
	return (short)(result);
   }

    // test checksum of incoming segments
    public boolean isChecksumValid() {
	return (computeChecksum() == 0);
    }
    
    public void insertUrgentPointer(int urgentPointer) {
	writeUShort(URGENT_POINTER_OFFSET, (short) urgentPointer);
    }

    public void insertOptions(byte[] options) {
	// optionsSet = true;
    }
   
    public void insertData(byte[] data) {
	writeBytes(getHeaderLength(), data);
    }
	
    // abstract method of class Format
    public int length() {
	return this.packetsize;
    }

    public int getSourcePort() {
	return (int) readUnsignedShort(SOURCE_PORT_OFFSET);
    }
    
    public int getDestinationPort() {
	return (int) readUnsignedShort(DESTINATION_PORT_OFFSET);
    }

    public int getSequenceNumber() {
	return readInt(SEQUENCE_NUMBER_OFFSET);
    }

    public int getAcknowledgmentNumber() {
	return readInt(ACKNOWLEDGEMENT_NUMBER_OFFSET);
    }

    public int getHeaderLength() {
	int headerLength = (int) readByte(HEADER_LENGTH_OFFSET) & 0x000000f0;
	return ((headerLength >> 4) & 0x0000000f) * 4;
    }

    public int getFlags() {
	return (int) readByte(FLAGS_OFFSET) & 0x0000003f;
    }

    public String Flags2String(){
	return (isURGFlagSet() ? "U" : "")+
	    (isACKFlagSet() ? "A" : "")+
	    (isPSHFlagSet() ? "P" : "")+
	    (isRSTFlagSet() ? "R" : "")+
	    (isSYNFlagSet() ? "S" : "")+
	    (isFINFlagSet() ? "F" : "");
	
    }

    public int getWindowSize() {
	return (int) readUnsignedShort(WINDOW_SIZE_OFFSET);
    }

    public int getChecksum() {
	return (int) readUnsignedShort(CHECKSUM_OFFSET);
    }

    public int getUrgentPointer() {
	return (int) readUnsignedShort(URGENT_POINTER_OFFSET);
    }

    public byte[] getOptions() {
	return null;
    }

    public byte[] getData() {
	byte[] data = new byte[length() - getHeaderLength()];
	readBytes(getHeaderLength(), data);
	return data;
    }

    public boolean isURGFlagSet() {
	int flags = getFlags();
	return ((flags & URG) != 0);
    }
       
    public boolean isACKFlagSet() {
	int flags = getFlags();
	return ((flags & ACK) != 0);
    }

    public boolean isPSHFlagSet() {
	int flags = getFlags();
	return ((flags & PSH) != 0);
    }

    public boolean isRSTFlagSet() {
	int flags = getFlags();
	return ((flags & RST) != 0);
    }

    public boolean isSYNFlagSet() {
	int flags = getFlags();
	return ((flags & SYN) != 0);
    }

    public boolean isFINFlagSet() {
	int flags = getFlags();
	return ((flags & FIN) != 0);
    }

    public boolean areFlagsSet(int flags) {
	return (((getFlags() & flags) == flags) && ((getFlags() &  flags) == getFlags()));
    }

    public void dump() {
	Debug.out.println("TCP-Packet:");
	Debug.out.println("\t src port      : " + getSourcePort());
	Debug.out.println("\t dst port      : " + getDestinationPort());
	Debug.out.println("\t seq no        : 0x" + Integer.toHexString(getSequenceNumber()));
	Debug.out.println("\t ack no        : 0x" + Integer.toHexString(getAcknowledgmentNumber()));
	Debug.out.println("\t header len    : " + getHeaderLength());
	Debug.out.println("\t URG           : " + isURGFlagSet());
	Debug.out.println("\t ACK           : " + isACKFlagSet());
	Debug.out.println("\t PSH           : " + isPSHFlagSet());
	Debug.out.println("\t RST           : " + isRSTFlagSet());
	Debug.out.println("\t SYN           : " + isSYNFlagSet());
	Debug.out.println("\t FIN           : " + isFINFlagSet());
	Debug.out.println("\t window size   : " + getWindowSize());
	Debug.out.println("\t checksum      : 0x" + Integer.toHexString(getChecksum()));
	Debug.out.println("\t urgent ptr    : " + getUrgentPointer());
	Debug.out.println("\t options       : " + ((optionsSet) ? "" : "no options"));
	Debug.out.println("\t segment len   : " + length());
    }
}
