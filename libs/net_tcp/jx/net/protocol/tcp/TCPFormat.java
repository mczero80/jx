package jx.net.protocol.tcp;
import metaxa.os.*;
import java.util.*;
import java.lang.*;
import java.net.*;

import jx.zero.*;
import jx.timer.*;

/*
 * Format of the TCP header:
 *
 */

class TCPFormat extends Format {
  
  int n_options = 0;
  
  TCPFormat(byte[] buf, int offset) { super(buf, offset);}

  void insertSourcePort(int port) { writeShort(0, (short)port); }
  void insertDestPort(int port)     {  writeShort(2, (short)port);  }
  void insertSeqNumber(int seq)     {  writeInt(4, seq);  }
  void insertAckNumber(int ack)     {  writeInt(8, ack);  }
  // insert the flags and combined with the data offset - see the usage of this method
  void insertFlags(int flags)     {  writeShort(12, (short)flags);  }
  void insertWindow(int window)     {  writeShort(14, (short)window);  }
  void insertChecksum(int cs)     { Debug.out.println("VOR TCP.insertChecksum"); writeShort(16, (short)cs); Debug.out.println("NACH TCP.insertChecksum"); }
  void insertUrgentPointer(int up)     {  writeShort(18, (short)up);  }
    
  // optional options --- check length
  // the argument o gives the maximum segment size the TCP will be able to cope with - option header will be added by this method
  void insertMaxSegOpt(int o) {  
    n_options = 1;
    // Header and length of MaxSeg Option: Kind = 2, length = 4 Bytes
    int maxseg = 0x02040000;
    // check, wether the upper 16 bits of argument o are 0 so they won´t change the header we´ll add later (5 lines below)
    if ((o & 0xffff0000) != 0) {
      Debug.out.println("TCPFormat.insertMaxSegOpt() - Option value destroys maxseg option header! - option discarded");
      return;
    }
    maxseg = maxseg | o;
    writeInt(20, maxseg);  
  }
  void insertData(byte[] d) {
    writeBytes(20+n_options*4, d);
  }
  // get access to the underlying buffer
  byte[] buf() {
    return buf;
  }

  /* read */
  int getSourcePort() {
    return readShort(0);
  }
  int getDestPort() { 
    return readShort(2);
  }
  int getSequenceNumber() {
    return readInt(4);
  }
  int getAcknowledgeNumber() {
    return readInt(8);
  }
  int getDataOffset() {
    return ((readByte(12) & 0xf0) >> 4);
  }
  // just an alias for the method above
  int getHeaderLength() {
    return getDataOffset();
  }
  // just an alias for the method getControlBits()
  int getFlags() {
    return getControlBits();
  }
  int getControlBits() {
    return (readByte(13 & 0x3f));
  }
  int getWindow() {
    return readShort(14);
  }
  int getChecksum() {
    return readShort(16);
  }
  int getUrgent() {
    return readShort(18);
  }
  public int length() { 
    return requiresSpace() + n_options * 4 /* + options */;
  }
  static int requiresSpace() { 
    return 20; 
  }
  
  /**
    * compute the checksum as described in RFC 1071 - the one´s complement of the sum of one´s complements
    * this method uses IPFormat.checksum() 
    *
    * @param ph the pseudoheader with is prefixed to TCP segment
    * @param tcp the TCP segment containing TCP header and data
    *
    * @return checksum computed over pseudoheader and tcp segment 
    *
    */
    
  public int checksummer(PseudoHeader ph, TCPFormat tcp) {
   
    int sum = 0;   
    int sum1 = IPFormat.checksum(ph);
    int sum2 = IPFormat.checksum(tcp);
    
    sum = sum1 + sum2;
    
    while ((sum >>> 16) > 0)
      sum = (sum & 0xffff) + (sum >>> 16);
    
    return ((~sum) & 0x0000ffff);
  }

  /* old version of the checksummer method
  byte[] buf1 = ph.buf();
  byte[] buf2 = tcp.buf;
  int length = buf1.length + buf2.length;
  int s = 0;
  boolean pad = false;
    

    Debug.out.println("CHECKSUMMER: ph ist: " + buf1.length + " und tcp ist: " + buf2.length);
    if ((buf2.length % 2) != 0) {
      length++;
      pad = true;
    }
    
    for(int i=0; i<length; i+=2) {
      if (i == (length - 2)) {
	if (pad) {
	  byte help = (byte)Format.readByte(buf2, i - buf1.length);
	  s = help | 0x0;
	}
      }
      else if (i < buf1.length) 
	s = Format.readShort(buf1, i);
      else 
	s = Format.readShort(buf2, i - buf1.length); 
      
      sum += s;
      if (sum > 0xffff)
	sum -= 0xffff;
    }
    Debug.out.println("TCPChecksummer() ist FERTIG!!!");
    return((~sum) & 0x0000ffff);
  }
  */	
}
