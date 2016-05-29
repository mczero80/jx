package jx.wintv;

public class RCKey {
   String label;
   byte code;
   
   RCKey(String label, int code){
      this.label = label;
      this.code = (byte)code;
   }
   
   public boolean equals(RCKey other){
      if( label.equals(other.label) && code == other.code )
	return true;
      return false;
   }
}

