package jx.net.protocol.ip;

import jx.zero.*;
import jx.zero.debug.*;

import jx.net.format.Format;
import jx.net.IPAddress;

/**
  * Write IP header data according to the IP format in the buffer.
  * RFC791, RFC1812
  *	char verhdrlen;
  *	char service;
  *	unsigned short len;
  *	unsigned short ident;
  *	unsigned short frags;
  *	char ttl;
  *	char protocol;
  *	unsigned short chksum;
  *	char src[4];
  *	char dest[4];
  *
  */
public class IPFormat extends Format {
      static final boolean dumpAll = false; // switch on to see all ip packets

    public static final int FLAGS_MORE_FRAGMENTS = 2;
    public static final int FLAGS_DONT_FRAGMENT = 4;

    public static final int CHECKSUM_OFFSET = 10;

    public IPFormat(Memory buf) { this(buf,0); }
  public IPFormat(Memory buf, int offset) { 
    super(buf, offset);
    if (dumpAll){
	Debug.out.println("IP-Packet:");
	Dump.xdump1(buf, offset, 48);
    }
  }
  
  public void insertHeaderLength() {  
    writeByte(0, (byte)0x45 /*0x40 = version ; 5= sizeof IP >> 2 or number of words */);
  }
  public void insertTypeOfService(int tos){
    writeByte(1, (byte)tos);  
  }
  public void insertTotalLength(int len) {
    writeUShort(2, (short)len);  
  }
  public void insertIdentification(int id) {
    writeUShort(4, (short)id);  
  }
  public void insertFragmentOffset(int off) {
    writeUShort(6, (short)off); /* and flags*/
  }
  public void insertTimeToLive(int ttl) {
    writeByte(8, (byte)ttl);
  }
  public void insertProtocol(int p) {
    writeByte(9, (byte)p);
  }
  public void insertChecksum() {
      writeUShort(CHECKSUM_OFFSET, (short) 0);
      short checksum = computeChecksum();
      writeUShort(CHECKSUM_OFFSET, (short) checksum);
      //writeUShort(10, (short)sum); 
      //MYwriteUShort(10, (short)checksum); 
  }
  
    private void MYwriteUShort(int o, short d) { 
	writeByte(o+1, (byte)((d>>8) & 0xff));
	writeByte(o, (byte)(d & 0xff));
    }
  public void insertSourceAddress(IPAddress ip_src) {
    writeAddress(12, ip_src); 
  }
  public void insertDestAddress(IPAddress ip_dst) {
    writeAddress(16, ip_dst);
  }
  /*void insertOptions(int opt)  {  writeInt(20, opt);  } Laenge auf 6 setzen!!!*/ 
  
  public  int getVersion() {
    return (readByte(0) & 0xf0);
  }
  public int getHeaderLength() {  
    byte val = (byte)readByte(0);
    // get the second 4 bits
    val &= 0xf;
    // the HLEN field tells us the number of dwords, so we have to multiply with 4
    return (int)(val * 4);
  }
  public int getServiceType() {
    return readUnsignedByte(1);
  }
  public int getTotalLength() {
    return readUnsignedShort(2);
  }
  public int getIdentification() {
    return readUnsignedShort(4);
  }
  public int getFlags() {
      // int flags = readByte(6);
    int flags = readUnsignedByte(6);
    // get the 3 flags bits
    flags = flags >> 4;
    return flags & 0x7;
  }
  public int getFragmentOffset() {
    int frag = readUnsignedShort(6);
    frag &= 0x1fff;
    return frag;
  }
  public int getTimeToLive() {
    return readUnsignedByte(8);
  }
  public int getProtocol() {  
    return readUnsignedByte(9); 
  }
  public int getHeaderChecksum() {
    return readUnsignedShort(10);
  }
  public byte[] getSourceIPAddress() {
    byte[] ret = new byte[4];
    ret[0] = readByte(12);
    ret[1] = readByte(13);
    ret[2] = readByte(14);
    ret[3] = readByte(15);
    return ret;
  }
  public byte[] getDestIPAddress() {
    byte[] ret = new byte[4];
    ret[0] = readByte(16);
    ret[1] = readByte(17);
    ret[2] = readByte(18);
    ret[3] = readByte(19);
    return ret;
  }
  
  /**
    * IP checksum algorithm.
    * see RFC1071
    * the checksum is the one´s complement of the sum of 16-bit values summed up by one´s complement arithmetic
    *
    * @param format a format object containing the buffer to checksum
    *
    * @return checksum value
    *
    */
    /*  
  public int checksum(Memory buf, int offset, int length) {
    int sum = 0;
    boolean flag = false;
    int i;
    
    // if the length is odd, we must read a byte at the end, not a short
    if ((length % 2) != 0) {
      flag = true;
      length--;
    }
    
    for(i=0; i<length; i+=2) {
      int s = Format.readShort(buf,offset+i);
      sum += s;
      if (sum > 0xffff)
	sum -= 0xffff;
    }
    // read the last byte if length is odd
    if (flag) {
      sum += Format.readByte(buf,offset + i);
    }
    Debug.out.println("IPFORMAT.checksum() ist FERTIG!!!");
    // return the one´s complement of the sum 
    return((~sum) & 0x0000ffff);
  }
    */
  /**
   * does the same checksum computation as the method checksum, but is just used for IP-header computation
   * therefore we don´t need to check wether the length is odd, as this method uses requiresSpace() and we know the IP header length
   * is fixed to 20 Bytes (we don´t use IP options)
   *
   * @return the checksum value (one´s complement of the sum of one´s complements)
   */
  
  public int computeChecksumOLD() {
    int sum = 0;
    
    for(int i=0; i<requiresSpace(); i+=2) {
	int s = readShort(i);
	sum += s;
	if (sum > 0xffff)
	    sum -= 0xffff;
    }
    return((~sum) & 0x0000ffff);
  }

    public final short computeChecksum() {
	int len = requiresSpace();
	len >>>= 1;
	short sum = 0; 
	while (len-- > 0) {
	    short value = (short) (readUnsignedShort(len * 2) & 0xffff);
	    sum = addUShort(sum, value);
	}
	int checksum = (~sum) & 0xffff;
	return (short)checksum;
    }

     private final short addUShort(short a, short b){
	int result = (((int) a)&0xffff) + (((int) b)&0xffff);
	if (result > (int)0x0000ffff) {
	    result -= 0x0000ffff;
	}
	return (short)(result);
   }

    
  public int length() { 
    return requiresSpace(); 
  }
    
    public void dump() {
	Debug.out.println("IP-Packet:");
	Debug.out.println("   Source: "+(new IPAddress(getSourceIPAddress())));
	Debug.out.println("   Destination: "+(new IPAddress(getDestIPAddress())));
	Debug.out.println("   Checksum:"+getHeaderChecksum());
	Debug.out.println("   ID:"+getIdentification());
	int proto = getProtocol();
	if (proto == IP.PROTO_UDP) 	Debug.out.println("   Protocol: UDP");
	else if (proto == IP.PROTO_TCP) 	Debug.out.println("   Protocol: TCP");
	else Debug.out.println("   Protocol: "+proto);
	Debug.out.println("   First 32 bytes of data:");
	Dump.xdump(buf, offset+requiresSpace(), 32);
    }

  public static int requiresSpace() { 
    return 20; // 24 with options
  }
}
