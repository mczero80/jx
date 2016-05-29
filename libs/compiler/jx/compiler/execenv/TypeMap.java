package jx.compiler.execenv; 

import jx.zero.Debug; 
import jx.compiler.execenv.ExtendedDataOutputStream;
import jx.compiler.execenv.ExtendedDataInputStream;
import java.io.IOException;
import jx.classfile.datatypes.BCBasicDatatype;

public class TypeMap {
  public static void writeMap(ExtendedDataOutputStream out, boolean[] map, boolean writeLen) throws IOException {
      int n_bytes = (map.length+7) >> 3;
      int j=0;
      out.writeInt(n_bytes);
      if (writeLen) out.writeInt(map.length);
      //Debug.out.println("--");
      for(int k=0; k<n_bytes; k++) {
	  byte b=0;
	  for(int i=0; i<8; i++) {
	      if (j==map.length) {
		  for(;i<8;i++) {
		      b>>>=1;
		      b &= 0x7f;
		  }
		  break;
	      }
	      b >>>= 1;
	      b &= 0x7f;

	      if (map[j++]) {
		  b |= 0x80;
	      }
	  }
	  //Debug.out.println("B: 0x"+Integer.toBinaryString(b));
	  out.writeByte(b);
      }
  }    
    public static void writeMap(ExtendedDataOutputStream out, int[] map) throws IOException {
	boolean []map1 = new boolean[map.length];
	for(int i=0; i<map1.length; i++) {
	    if(map[i] == BCBasicDatatype.REFERENCE) {
		map1[i] = true;
	    }
	}
	writeMap(out, map1, true);
	
    }
    
}
