package jx.zero.debug;

import jx.zero.ReadOnlyMemory;
import jx.zero.Debug;

import java.io.PrintStream;

public class Dump {

  /**
    * Hexdump in the style:
    *  7f  45  4c  46  01  01  01  00
    */
  public static void xdump(byte[] data, int len) {
    if (len > data.length) len = data.length;
    for(int i=0; i<len;i++) {
      Debug.out.print(" " + byteToHex(data[i]) + " ");
      if ((i+1)%8 == 0) Debug.out.println(); 
    }
    if (len < 32) Debug.out.println();
  }

  public static void xdump(ReadOnlyMemory data, int len) {
    if (len > data.size()) len = data.size();
    for(int i=0; i<len;i++) {
      Debug.out.print(" " + byteToHex(data.get8(i)) + " ");
      if ((i+1)%8 == 0) Debug.out.println(); 
    }
    if (len < 32) Debug.out.println();
  }

  public static void xdump(PrintStream out, byte[] data, int len) {
    if (len > data.length) len = data.length;
    for(int i=0; i<len;i++) {
      out.print(" " + byteToHex(data[i]) + " ");
      if ((i+1)%8 == 0) out.println(); 
    }
    if (len < 32) out.println();
  }

  /**
    * hexdump in the style:
    * 00000000    7f454c46 01010100 00000000 00000000    .ELF............
    */
  public static void xdump1(byte[] data, int len) {
    if (len > data.length) len = data.length;
    StringBuffer ascii = new StringBuffer();
    for(int i=0; i<len;i++) {
      Debug.out.print(byteToHex(data[i]));
      ascii.append(byteToAscii(data[i]));
      if ((i+1)%4 == 0) Debug.out.print(" "); 
      if ((i+1)%16 == 0) {
	Debug.out.println("   "+ascii.toString()); 
	ascii = new StringBuffer();
      }
    }
    if (len < 32) Debug.out.println();
  }

  public static void xdump1(ReadOnlyMemory data, int offset, int len) {
    if (offset+len > data.size()) len = data.size();
    StringBuffer ascii = new StringBuffer();
    for(int i=offset; i<len+offset;i++) {
      Debug.out.print(byteToHex(data.get8(i)));
      ascii.append(byteToAscii(data.get8(i)));
      if ((i+1)%4 == 0) Debug.out.print(" "); 
      if ((i+1)%16 == 0) {
	Debug.out.println("   "+ascii.toString()); 
	ascii = new StringBuffer();
      }
    }
    if (len < 32) Debug.out.println();
  }

  public static void xdump(PrintStream out, ReadOnlyMemory data, int offset, int len) {
    if (offset+len > data.size()) len = data.size();
    StringBuffer ascii = new StringBuffer();
    for(int i=offset; i<len+offset;i++) {
      out.print(byteToHex(data.get8(i)));
      ascii.append(byteToAscii(data.get8(i)));
      if ((i+1)%4 == 0) out.print(" "); 
      if ((i+1)%16 == 0) {
	out.println("   "+ascii.toString()); 
	ascii = new StringBuffer();
      }
    }
    if (len < 32) out.println();
  }

  public static void xdump(byte[] data, int offset, int len) {
    if (offset+len > data.length) len = data.length-offset;
    for(int i=0; i<len;i++) {
      Debug.out.print(" " + byteToHex(data[offset+i]) + " ");
      if ((i+1)%8 == 0) Debug.out.println(); 
    }
    if (len < 32) Debug.out.println();
  }
  public static void xdump(ReadOnlyMemory data) {
      xdump(data, 0, data.size());
  }

  public static void xdump(ReadOnlyMemory data, int offset, int len) {
    if (offset+len > data.size()) len = data.size()-offset;
    for(int i=0; i<len;i++) {
      Debug.out.print(" " + byteToHex(data.get8(offset+i)) + " ");
      if ((i+1)%8 == 0) Debug.out.println(); 
    }
    if (len < 32) Debug.out.println();
  }

  static String toHex(int i) {
    if (i <= 9) return "" + i;
    switch(i) {
    case 10: return "a"; 
    case 11: return "b";
    case 12: return "c";
    case 13: return "d";
    case 14: return "e";
    case 15: return "f";
    default: Debug.out.println("Format error: "+i);
    }
    return null;
  }
  static String byteToHex(byte n) {
    int i = n;
    if (i<0) i+= 256;
    int i1 = i & 0xf;
    int i2 = (i & 0xf0) >> 4;
    return toHex(i2) + toHex(i1);
  }

  static char byteToAscii(byte n) {
    int i = n;
    if (i<0) i+= 256;
    if ((i>= 'A' && i<='Z') 
	|| ( i>= 'a' && i<='z') 
	|| (i>= '0' && i<='9') )
	return (char) i;
    else
      return '.';
  }

}
