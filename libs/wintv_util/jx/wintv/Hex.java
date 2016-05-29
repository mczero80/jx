package jx.wintv;

import jx.zero.*;
import jx.zero.debug.*;

public class Hex {

   public static String toHexString(int val, int width){
      StringBuffer sb = new StringBuffer(Integer.toHexString(val));
      while( sb.length() < width )
	sb.insert(0, '0');
      // sb.append(", "+val);
      return sb.toString();
   }
   
   public static String toHexString(byte val){
      return toHexString(val & 0xff, 2);
   }

   public static String toHexString(short val){
      return toHexString(val & 0xffff, 4);
   }
   
   public static String toHexString(int val){
      return toHexString(val & 0xffffffff, 8);
   }
   
   public static String toHexDump(byte ar[]){
      String retval = "";
      for(int i=0; i<ar.length; i+=16){
	 for(int j=i; j<i+16 && j<ar.length; ++j){
	    retval += toHexString(ar[j]);
	    retval += ' ';
	 }
	 retval += '\n';
      }
      return retval;
   }
   
   public static void dumpHex(DebugPrintStream out, byte ar[], int startaddr){
      String str = "";
      for(int i=0; i<ar.length; i+=16, startaddr+=16, str=""){
	 for(int j=i; j<i+16 && j<ar.length; ++j){
	    str += toHexString(ar[j]);
	    str += ' ';
	 }
	 out.print(toHexString(startaddr));
	 out.print(": ");
	 out.println(str);
      }
   }
   
   public static void dumpHex(DebugPrintStream out, Memory mem, 
			      int offset, int endoffset, 
			      int lineaddr){
      dumpHex(out, mem, offset, 16, endoffset, lineaddr);
   }

   public static void dumpHex(DebugPrintStream out, Memory mem,
			      int offset, int incr, int endoffset,
			      int lineaddr){
      for(;offset<endoffset; offset+=incr, lineaddr+=incr){
	 out.print(toHexString(lineaddr));
	 out.print(": ");
	 for(int n=0; n<16 && offset+n < endoffset; ++n){
	    out.print(toHexString(mem.get8(offset+n)));
	    out.print(' ');
	 }
	 out.println();
      }
   }
   
   public static void dumpHex16(DebugPrintStream out, Memory mem,
				int offset, int incr, int endoffset,
				int lineaddr){
      for(;offset<endoffset; offset+=incr, lineaddr+=incr){
	 out.print(toHexString(lineaddr));
	 out.print(": ");
	 for(int n=0; n<16 && offset+n < endoffset; n+=2){
	    out.print(toHexString(mem.get16((offset+n)/2)));
	    out.print(' ');
	 }
	 out.println();
      }
   }
   
   public static void dumpHex32(DebugPrintStream out, Memory mem, int bytes){
      dumpHex32(out, mem, 0x0, 16, bytes, mem.getStartAddress());
   }
   
   public static void dumpHex32(DebugPrintStream out, Memory mem,
				int offset, int incr, int endoffset,
				int lineaddr){
      for(;offset<endoffset; offset+=incr, lineaddr+=incr){
	 out.print(toHexString(lineaddr));
	 out.print(": ");
	 for(int n=0; n<16 && offset+n < endoffset; n+=4){
	    out.print(toHexString(mem.get32((offset+n)>>2)));
	    out.print(' ');
	 }
	 out.println();
      }
   }
}
