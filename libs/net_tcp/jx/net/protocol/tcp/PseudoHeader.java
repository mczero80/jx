package jx.net.protocol.tcp;
import metaxa.os.*;
import java.util.*;
import java.lang.*;
import java.net.*;

import jx.zero.*;
import jx.timer.*;


class PseudoHeader extends Format {
  PseudoHeader() { this(new byte[requiresSpace()], 0);
  Debug.out.println("PSEUDOHEADER: " + requiresSpace());}
  PseudoHeader(byte[] buf, int offset) { super(buf, offset);}
  
  void insertSourceAddress(byte[] ip_src){  Debug.out.println("PSEUDO: insertSourceAddress");writeBytes(0, ip_src);  }
  void insertDestAddress(byte[] ip_dst)  {  Debug.out.println("PSEUDO: insertDestAddress"); writeBytes(4, ip_dst);  }
  void insertMBZ(int mbz)  { Debug.out.println("PSEUDO: insertMZB");  writeByte(8, (byte)mbz);  }
  void insertProtocol(int protocol)  {  Debug.out.println("PSEUDO: insertProtocol"); writeByte(9, (byte)protocol);  }
  void insertLength(int length)  {  Debug.out.println("PSEUDO: insertLength"); writeShort(10, (short)length);  }
  
  /* not part of the pseudoheader but eases checksum computation */
  void insertTCPChecksum(int checksum)  {  Debug.out.println("PSEUDO: insertTCPCheck"); writeShort(12, (short)checksum);  }
  
  // ACHTUNG IN DER TCP-DOKU NOCH UEBERPRUEFEN WIE LANGE PSEUDOHEADER IST!!!!!!!
  
  public byte[] buf() {
    return super.buf;
  }
  public int length() { return 12; }
  static int requiresSpace() { return 12; }
}
